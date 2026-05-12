package it.polimi.it.network.server;

import it.polimi.it.enums.TotemColour;
import it.polimi.it.network.message.Message;

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
 *
 */
public class LobbyServer {

    private static final Logger LOG = Logger.getLogger(LobbyServer.class.getName());

    private final int             socketPort;
    private final ExecutorService threadPool      = Executors.newCachedThreadPool();
    private final List<GameSession> waitingSessions = new ArrayList<>();
    private final List<GameSession> activeSessions  = new ArrayList<>();

    /** Single authoritative set — shared with RmiServerImpl via claimNickname/releaseNickname. */
    private final Set<String> takenNicknames = ConcurrentHashMap.newKeySet();

    private volatile boolean running = false;

    public LobbyServer(int socketPort) {
        this.socketPort = socketPort;
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
            if (!Message.JOIN.equals(msg.getType())) {
                writer.println(Message.error("First message must be JOIN.").encode());
                socket.close();
                return;
            }

            String nickname     = msg.str("nickname").trim();
            int    numPlayers   = msg.num("numPlayers");
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
                handleReconnection(socket, nickname, reconnectTarget);
                return;
            }

            // FIX #1: unified claim via the single authority
            if (!claimNickname(nickname)) {
                writer.println(Message.error(
                        "Nickname '" + nickname + "' is already taken.").encode());
                socket.close();
                return;
            }

            GameSession session = getOrCreateSession(numPlayers);

            SocketClientHandler handler = new SocketClientHandler(socket, nickname, session, totemColour) {
                @Override public void close() {
                    super.close();
                    if (session.isTerminated())
                        releaseNickname(nickname);
                }
            };

            session.addClient(handler);
            // FIX #3: move out of waiting list once the session is full
            if (!session.hasRoom()) {
                synchronized (LobbyServer.this) {
                    waitingSessions.remove(session);
                }
            }
            threadPool.submit(handler);
            LOG.info("Player '" + nickname + "' connected via Socket.");

        } catch (IOException e) {
            LOG.warning("Handshake error: " + e.getMessage());
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void handleReconnection(Socket socket, String nickname, GameSession session) {
        SocketClientHandler handler = new SocketClientHandler(socket, nickname, session, null) {
            @Override public void close() {
                super.close();
                if (session.isTerminated())
                    releaseNickname(nickname);
            }
        };
        session.reconnectClient(handler);
        threadPool.submit(handler);
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
     * Returns an existing waiting session or creates a new one.
     *
     * <p>Terminated and started sessions are pruned from {@code waitingSessions}
     * to keep the list bounded.
     */
    public synchronized GameSession getOrCreateSession(int requestedPlayers) {
        // FIX #3: also remove started sessions from the waiting list
        activeSessions.removeIf(GameSession::isTerminated);
        waitingSessions.removeIf(s -> s.isTerminated() || s.isStarted());

        for (GameSession s : waitingSessions) {
            if (s.hasRoom()) return s;
        }

        int n = (requestedPlayers >= 2 && requestedPlayers <= 5) ? requestedPlayers : 2;
        GameSession session = new GameSession(n);
        waitingSessions.add(session);
        activeSessions.add(session);
        LOG.info("Created new GameSession for " + n + " players.");
        return session;
    }
}