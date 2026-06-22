package it.polimi.it.network.server;

import it.polimi.it.enums.TotemColour;
import it.polimi.it.network.message.Message;
import it.polimi.it.network.server.persistence.LeaderboardRepository;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * TCP lobby server.
 *
 * <p>Accepts incoming Socket connections and routes each player to the
 * appropriate {@link GameSession}.  The {@link RmiServerImpl} delegates
 * nickname management here so that Socket and RMI players share a single
 * authority on unique nicknames — eliminating the collision bug where an RMI
 * player could join with the same name as an already-connected Socket player.
 *
 * <p>The lobby holds a (possibly {@code null}) {@link LeaderboardRepository}
 * provided by {@link GameServer} and forwards it to every new
 * {@link GameSession}; sessions use it to persist game results and serve
 * end-of-game rankings (DB leaderboard feature).
 */
public class LobbyServer {

    private static final Logger LOG = Logger.getLogger(LobbyServer.class.getName());

    private final int                   socketPort;
    private final ExecutorService       threadPool      = Executors.newCachedThreadPool();
    private final List<GameSession>     waitingSessions = new ArrayList<>();
    private final List<GameSession>     activeSessions  = new ArrayList<>();

    /** Single authoritative set — shared with RmiServerImpl via claimNickname/releaseNickname. */
    private final Set<String> takenNicknames = ConcurrentHashMap.newKeySet();

    /** Repository injected by GameServer; may be {@code null} when the DB is unreachable. */
    private final LeaderboardRepository leaderboardRepository;

    private volatile boolean running = false;

    /** Backward-compatible constructor (no leaderboard). */
    public LobbyServer(int socketPort) {
        this(socketPort, null);
    }

    /**
     * Full constructor.
     *
     * @param socketPort            TCP port the lobby listens on
     * @param leaderboardRepository repository used by GameSession to persist
     *                              results; pass {@code null} to disable
     */
    public LobbyServer(int socketPort, LeaderboardRepository leaderboardRepository) {
        this.socketPort            = socketPort;
        this.leaderboardRepository = leaderboardRepository;
    }

    // ── Lifecycle

    public void start() throws IOException {
        running = true;
        LOG.info("LobbyServer listening on port " + socketPort);
        try (ServerSocket serverSocket = new ServerSocket(socketPort)) {
            serverSocket.setSoTimeout(1000);
            while (running) {
                try {
                    Socket client = serverSocket.accept();
                    threadPool.submit(() -> handleNewConnection(client));
                } catch (SocketTimeoutException ignored) {}
            }
        }
        threadPool.shutdownNow();
    }

    public void stop() { running = false; }

    //  Nickname authority (used by RmiServerImpl)

    /**
     * Atomically claims the nickname.
     *
     * @return {@code true} if the nickname was free and is now reserved;
     *         {@code false} if it was already taken.
     */
    public synchronized boolean claimNickname(String nick) {
        return takenNicknames.add(nick);
    }

    /** Releases a nickname so it can be reused after the player fully disconnects. */
    public synchronized void releaseNickname(String nick) {
        takenNicknames.remove(nick);
    }

    //  New connection handshake

    private void handleNewConnection(Socket socket) {
        try {
            // FIX #2: explicit UTF-8
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter    writer = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            String firstLine = reader.readLine();
            if (firstLine == null) { socket.close(); return; }

            Message msg = Message.decode(firstLine);

            // Multiple games: the client may request the session list before JOIN.
            if (Message.LIST_SESSIONS.equals(msg.getType())) {
                int filter = msg.num("numPlayersFilter");
                writer.println(Message.sessionList(getOpenSessions(filter)).encode());
                // Read the actual JOIN that follows immediately.
                String joinLine = reader.readLine();
                if (joinLine == null) { socket.close(); return; }
                msg = Message.decode(joinLine);
            }

            if (!Message.JOIN.equals(msg.getType())) {
                writer.println(Message.error("First message must be JOIN.").encode());
                socket.close();
                return;
            }

            String nickname     = msg.str("nickname").trim();
            int    numPlayers   = msg.num("numPlayers");
            // Optional sessionId: when present, the client wants that session.
            String sessionId    = msg.optStr("sessionId");
            String colourStr    = msg.str("totemColour");
            TotemColour totemColour;
            try { totemColour = TotemColour.valueOf(colourStr); }
            catch (Exception e) { totemColour = TotemColour.RED; }

            if (nickname.isBlank()) {
                writer.println(Message.error("Nickname must not be blank.").encode());
                socket.close();
                return;
            }

            // Reconnection check first (nickname already taken but in an active session)
            GameSession reconnectTarget = findSessionForReconnection(nickname);
            if (reconnectTarget != null) {
                handleReconnection(socket, nickname, reconnectTarget, reader, writer);
                return;
            }

            // FIX #1: unified claim via the single authority
            if (!claimNickname(nickname)) {
                writer.println(Message.error(
                        "Nickname '" + nickname + "' is already taken.").encode());
                socket.close();
                return;
            }

            // Resolve the target session: direct join, forced new game, or matchmaking.
            final GameSession session;
            if (NEW_SESSION.equals(sessionId)) {
                // The user explicitly chose to create a new game.
                session = getOrCreateSession(numPlayers, true);
                LOG.info("Player '" + nickname + "' chose to create a new session.");
            } else if (sessionId != null && !sessionId.isBlank()) {
                GameSession found = getSessionById(sessionId);
                if (found == null) {
                    // Race condition: the session became full or started while
                    // the client was choosing, so fall back to a new session.
                    LOG.warning("Session '" + sessionId
                            + "' unavailable; falling back to a new session for "
                            + numPlayers + " players.");
                    writer.println(Message.error(
                            "La sessione scelta non è più disponibile. "
                                    + "Sei stato assegnato a una nuova sessione.").encode());
                    session = getOrCreateSession(numPlayers, true);
                } else {
                    session = found;
                }
            } else {
                session = getOrCreateSession(numPlayers, false);
            }

            SocketClientHandler handler = new SocketClientHandler(
                    socket, nickname, session, totemColour, reader, writer) {
                @Override public void close() {
                    super.close();
                    if (session.isTerminated())
                        releaseNickname(nickname);
                }
            };

            if (!session.addClient(handler)) {
                writer.println(Message.error("La sessione non è più disponibile.").encode());
                handler.close();
                releaseNickname(nickname);
                return;
            }

            threadPool.submit(handler);

            // FIX #3: move out of waiting list once the session is full
            if (!session.hasRoom()) {
                synchronized (LobbyServer.this) {
                    waitingSessions.remove(session);
                }
            }

            LOG.info("Player '" + nickname + "' connected via Socket.");

        } catch (IOException e) {
            LOG.warning("Handshake error: " + e.getMessage());
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void handleReconnection(Socket socket, String nickname, GameSession session,
                                    BufferedReader reader, PrintWriter writer) {
        SocketClientHandler handler = new SocketClientHandler(
                socket, nickname, session, null, reader, writer) {
            @Override public void close() {
                super.close();
                if (session.isTerminated())
                    releaseNickname(nickname);
            }
        };
        // Submit thread FIRST so writeLoop is running before reconnectClient()
        // calls c.send() — otherwise messages are lost.
        threadPool.submit(handler);
        session.reconnectClient(handler);
        LOG.info("Player '" + nickname + "' reconnected via Socket.");
    }

    //  Session management

    /**
     * Finds a started, non-terminated session that contains a player with the
     * given nickname.  Used for the reconnection flow.
     */
    public synchronized GameSession findSessionForReconnection(String nickname) {
        return activeSessions.stream()
                .filter(s -> s.isStarted() && !s.isTerminated() && s.hasPlayer(nickname))
                .findFirst()
                .orElse(null);
    }

    /**
     * Sentinel session id sent by the client when the user explicitly chooses
     * to create a new game in the session list. It differs from {@code null},
     * which means automatic matchmaking, and from any real session id.
     */
    public static final String NEW_SESSION = "NEW";

    /**
     * Returns an existing waiting session or creates a new one.
     *
     * @param requestedPlayers requested player count
     * @param forceNew         if true, always creates a new session and ignores
     *                         already open sessions
     */
    public synchronized GameSession getOrCreateSession(int requestedPlayers, boolean forceNew) {
        activeSessions.removeIf(GameSession::isTerminated);
        waitingSessions.removeIf(s -> s.isTerminated() || s.isStarted());

        if (!forceNew) {
            for (GameSession s : waitingSessions) {
                if (s.hasRoom() && s.getExpectedPlayers() == requestedPlayers) {
                    return s;
                }
            }
        }

        int n = (requestedPlayers >= 2 && requestedPlayers <= 5) ? requestedPlayers : 2;
        GameSession session = new GameSession(n, leaderboardRepository);
        waitingSessions.add(session);
        activeSessions.add(session);
        LOG.info("Created new GameSession for " + n + " players (forceNew=" + forceNew + ").");
        return session;
    }

    /** Backward-compatible overload: automatic matchmaking with forceNew=false. */
    public synchronized GameSession getOrCreateSession(int requestedPlayers) {
        return getOrCreateSession(requestedPlayers, false);
    }

    // Multiple games: discovery and direct join.

    /**
     * Snapshot of all open sessions, optionally filtered by player count.
     *
     * @param numPlayersFilter if greater than 0, filters by expected player
     *                         count; 0 returns all sessions
     */
    public synchronized java.util.List<Message.SessionInfo> getOpenSessions(int numPlayersFilter) {
        waitingSessions.removeIf(s -> s.isTerminated() || s.isStarted());
        java.util.List<Message.SessionInfo> result = new java.util.ArrayList<>();
        for (GameSession s : waitingSessions) {
            if (numPlayersFilter > 0 && s.getExpectedPlayers() != numPlayersFilter) continue;
            result.add(new Message.SessionInfo(
                    s.getSessionId(),
                    s.getExpectedPlayers(),
                    s.getCurrentPlayers(),
                    s.getConnectedNicknames()));
        }
        return result;
    }

    /**
     * Returns the waiting session with the given id, or {@code null} if it is
     * not found, already started, terminated, or unknown.
     */
    public synchronized GameSession getSessionById(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return null;
        return waitingSessions.stream()
                .filter(s -> !s.isTerminated() && !s.isStarted()
                        && s.getSessionId().equals(sessionId))
                .findFirst()
                .orElse(null);
    }

}
