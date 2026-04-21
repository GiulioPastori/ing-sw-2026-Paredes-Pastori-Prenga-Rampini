package it.polimi.it.network.server;

import it.polimi.it.network.message.Message;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * TCP lobby server.
 *
 * Accepts incoming connections and routes each player to the appropriate
 * GameSession. Supports multiple concurrent sessions and reconnections.
 */
public class LobbyServer {

    private static final Logger LOG = Logger.getLogger(LobbyServer.class.getName());

    private final int             socketPort;
    private final ExecutorService threadPool     = Executors.newCachedThreadPool();
    private final List<GameSession> waitingSessions = new ArrayList<>();
    private final List<GameSession> activeSessions  = new ArrayList<>();
    private final Set<String>       takenNicknames  = ConcurrentHashMap.newKeySet();

    private volatile boolean running = false;

    public LobbyServer(int socketPort) {
        this.socketPort = socketPort;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

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

            Message msg = Message.decode(firstLine);
            if (!Message.JOIN.equals(msg.getType())) {
                writer.println(Message.error("First message must be JOIN.").encode());
                socket.close();
                return;
            }

            String nickname   = msg.str("nickname").trim();
            int    numPlayers = msg.num("numPlayers");

            if (nickname.isBlank()) {
                writer.println(Message.error("Nickname must not be blank.").encode());
                socket.close();
                return;
            }

            // Check reconnection first
            GameSession reconnectTarget = findSessionForReconnection(nickname);
            if (reconnectTarget != null) {
                handleReconnection(socket, nickname, reconnectTarget);
                return;
            }

            // Normal join — check uniqueness
            synchronized (this) {
                if (takenNicknames.contains(nickname)) {
                    writer.println(Message.error(
                            "Nickname '" + nickname + "' is already taken.").encode());
                    socket.close();
                    return;
                }
                takenNicknames.add(nickname);
            }

            GameSession session = getOrCreateSession(numPlayers);

            SocketClientHandler handler = new SocketClientHandler(socket, nickname, session) {
                @Override public void close() {
                    super.close();
                    if (session.isTerminated())
                        takenNicknames.remove(nickname);
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
                if (session.isTerminated())
                    takenNicknames.remove(nickname);
            }
        };
        session.reconnectClient(handler);
        threadPool.submit(handler);
        LOG.info("Player '" + nickname + "' reconnected via Socket.");
    }

    // ── Session management ────────────────────────────────────────────────────

    /**
     * Finds a started session that contains a disconnected player with the
     * given nickname. Used for reconnection logic.
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

        int n = (requestedPlayers >= 2 && requestedPlayers <= 5) ? requestedPlayers : 2;
        GameSession session = new GameSession(n);
        waitingSessions.add(session);
        activeSessions.add(session);
        LOG.info("Created new GameSession for " + n + " players.");
        return session;
    }
}