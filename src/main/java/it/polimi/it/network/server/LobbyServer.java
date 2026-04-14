package it.polimi.it.network.server;

import it.polimi.it.network.message.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * TCP lobby server: accepts incoming connections and routes each player
 * to the appropriate GameSession.
 *
 * Changes vs original:
 * - {@link #reserveNickname(String)} and {@link #releaseNickname(String)} are
 *   now public so RmiServerImpl can share the same nickname namespace instead
 *   of maintaining its own separate set (which caused Socket and RMI players
 *   to be able to register with the same nickname in the same game).
 * - {@link #findSessionForReconnection(String)} is now public so RmiServerImpl
 *   can use the same reconnection logic as the Socket path.
 */
public class LobbyServer {

    private static final Logger LOG = Logger.getLogger(LobbyServer.class.getName());

    private final int             socketPort;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    private final List<GameSession> waitingSessions = new ArrayList<>();
    private final List<GameSession> activeSessions  = new ArrayList<>();
    private final Set<String>       takenNicknames  = ConcurrentHashMap.newKeySet();

    private volatile boolean running = false;

    public LobbyServer(int socketPort) {
        this.socketPort = socketPort;
    }

    // ── Server lifecycle ──────────────────────────────────────────────────────

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

    // ── Nickname management ───────────────────────────────────────────────────

    /**
     * Attempts to atomically reserve a nickname.
     * Public so RmiServerImpl can share the same namespace.
     *
     * @return true if reserved; false if already taken
     */
    public boolean reserveNickname(String nickname) {
        return takenNicknames.add(nickname);
    }

    /**
     * Releases a previously reserved nickname.
     * Call when a session terminates so the name can be reused.
     * Public so RmiServerImpl can call it on session end.
     */
    public void releaseNickname(String nickname) {
        takenNicknames.remove(nickname);
    }

    // ── New connection handshake ──────────────────────────────────────────────

    private void handleNewConnection(Socket socket) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            PrintWriter    writer = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream()), true);

            String firstLine = reader.readLine();
            if (firstLine == null) { socket.close(); return; }

            Message msg = MessageCodec.decode(firstLine);
            if (!(msg instanceof JoinMessage join)) {
                writer.println(MessageCodec.encode(
                        new ErrorMessage("First message must be JOIN.")));
                socket.close();
                return;
            }

            String nickname = join.nickname().trim();
            if (nickname.isBlank()) {
                writer.println(MessageCodec.encode(
                        new ErrorMessage("Nickname must not be blank.")));
                socket.close();
                return;
            }

            // Check if this is a RECONNECTION
            GameSession reconnectTarget = findSessionForReconnection(nickname);
            if (reconnectTarget != null) {
                handleReconnection(socket, nickname, reconnectTarget);
                return;
            }

            // Normal join — reserve nickname
            if (!reserveNickname(nickname)) {
                writer.println(MessageCodec.encode(
                        new ErrorMessage("Nickname '" + nickname + "' is already taken.")));
                socket.close();
                return;
            }

            GameSession session = getOrCreateSession(join.numPlayers());

            SocketClientHandler handler = new SocketClientHandler(socket, nickname, session) {
                @Override public void close() {
                    super.close();
                    if (session.isTerminated()) releaseNickname(nickname);
                }
            };

            session.addClient(handler);
            threadPool.submit(handler);
            LOG.info("Player '" + nickname + "' connected via Socket.");

        } catch (IOException e) {
            LOG.warning("Handshake error: " + e.getMessage());
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void handleReconnection(Socket socket, String nickname, GameSession session) {
        SocketClientHandler handler = new SocketClientHandler(socket, nickname, session) {
            @Override public void close() {
                super.close();
                if (session.isTerminated()) releaseNickname(nickname);
            }
        };
        session.reconnectClient(handler);
        threadPool.submit(handler);
        LOG.info("Player '" + nickname + "' reconnected via Socket.");
    }

    // ── Session management ────────────────────────────────────────────────────

    /**
     * Looks for a started (possibly suspended) session that contains the given
     * nickname as a player. Public so RmiServerImpl can use the same logic.
     *
     * @return the session, or null if this is a fresh join
     */
    public synchronized GameSession findSessionForReconnection(String nickname) {
        return activeSessions.stream()
                .filter(s -> s.isStarted() && !s.isTerminated() && s.hasPlayer(nickname))
                .findFirst()
                .orElse(null);
    }

    /** Returns an existing waiting session or creates a new one. */
    public synchronized GameSession getOrCreateSession(int requestedPlayers) {
        activeSessions.removeIf(GameSession::isTerminated);
        waitingSessions.removeIf(GameSession::isTerminated);

        for (GameSession s : waitingSessions) {
            if (s.hasRoom()) return s;
        }

        int numPlayers = (requestedPlayers >= 2 && requestedPlayers <= 5)
                ? requestedPlayers : 2;
        GameSession session = new GameSession(numPlayers);
        waitingSessions.add(session);
        activeSessions.add(session);
        LOG.info("Created new GameSession for " + numPlayers + " players.");
        return session;
    }
}