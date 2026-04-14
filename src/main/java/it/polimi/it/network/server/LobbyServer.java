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
 * Reconnection logic:
 * When a player sends JOIN with a nickname that already belongs to a
 * started (possibly suspended) session, they are reconnected to that
 * session instead of being rejected or creating a new one.
 *
 * Multiple sessions:
 * The server supports multiple concurrent games. Each call from a new
 * player either joins a waiting session or creates a new one.
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

            // Normal join — check nickname uniqueness
            synchronized (this) {
                if (takenNicknames.contains(nickname)) {
                    writer.println(MessageCodec.encode(
                            new ErrorMessage("Nickname '" + nickname + "' is already taken.")));
                    socket.close();
                    return;
                }
                takenNicknames.add(nickname);
            }

            GameSession session = getOrCreateSession(join.numPlayers());

            SocketClientHandler handler = new SocketClientHandler(socket, nickname, session) {
                @Override public void close() {
                    super.close();
                    // Free nickname only when game is over (not on disconnect,
                    // so the player can reconnect with the same name)
                    if (session.isTerminated()) takenNicknames.remove(nickname);
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

    /**
     * Handles a reconnecting player: creates a new SocketClientHandler and
     * calls session.reconnectClient() instead of addClient().
     */
    private void handleReconnection(Socket socket, String nickname,
                                    GameSession session) {
        SocketClientHandler handler = new SocketClientHandler(socket, nickname, session) {
            @Override public void close() {
                super.close();
                if (session.isTerminated()) takenNicknames.remove(nickname);
            }
        };
        session.reconnectClient(handler);
        threadPool.submit(handler);
        LOG.info("Player '" + nickname + "' reconnected via Socket.");
    }

    // ── Session management ────────────────────────────────────────────────────

    /**
     * Looks for a started (possibly suspended) session that contains a player
     * with the given nickname who is currently marked as disconnected.
     *
     * @return the session, or null if this is a fresh join
     */
    private synchronized GameSession findSessionForReconnection(String nickname) {
        return activeSessions.stream()
                .filter(s -> s.isStarted() && !s.isTerminated() && s.hasPlayer(nickname))
                .findFirst()
                .orElse(null);
    }

    /** Returns an existing waiting session or creates a new one. Also used by RmiServerImpl. */
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
