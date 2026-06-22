package it.polimi.it.network.server;

import it.polimi.it.enums.TotemColour;
import it.polimi.it.network.message.Message;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * TCP server-side handler for a single connected client.
 *
 * <p>Three threads per client:
 * <ul>
 * <li><b>reader</b> – reads incoming messages (main thread via {@link #run()}).</li>
 * <li><b>writer</b> – empties the outgoing {@link BlockingQueue}.</li>
 * <li><b>watchdog</b> – checks every {@value #WATCHDOG_INTERVAL_MS} ms if the client
 * has sent at least one message in the last {@value #HEARTBEAT_TIMEOUT_MS} ms;
 * if it expires, it closes the connection.</li>
 * </ul>
 *
 * thanks to {@link AtomicBoolean} {@code disconnectReported}: both the readLoop
 * and the watchdog can detect the disconnection, but only the first one to do so
 * notifies the session.
 */
public class SocketClientHandler implements ClientConnection, Runnable {

    private static final Logger LOG = Logger.getLogger(SocketClientHandler.class.getName());

    /** Watchdog check interval (ms). */
    static final long WATCHDOG_INTERVAL_MS = 5_000;

    /** Maximum silence from the client before considering it disconnected (ms).
     * With a heartbeat every 5s on the client side, 30s = 6 tolerated heartbeats —
     * enough for a slow user but quick to react in case of a real crash. */
    static final long HEARTBEAT_TIMEOUT_MS = 30_000;

    private final Socket                 socket;
    private final String                 nickname;
    private final TotemColour            totemColour;
    private final GameSession            session;
    private final BlockingQueue<Message> outbound = new LinkedBlockingQueue<>();

    private BufferedReader reader;
    private PrintWriter    writer;

    /** true after close(): prevents multiple closing operations. */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * true after the first disconnection report to the session.
     * Ensures that {@link GameSession#onPlayerDisconnected} is
     * called only once even if both readLoop and watchdog detect it.
     */
    private final AtomicBoolean disconnectReported = new AtomicBoolean(false);

    /** Timestamp of the last message received from the client (any type). */
    private volatile long lastSeenTime = System.currentTimeMillis();

    public SocketClientHandler(Socket socket, String nickname, GameSession session, TotemColour totemColour) {
        this.socket      = socket;
        this.nickname    = nickname;
        this.totemColour = totemColour;
        this.session     = session;
    }

    /** Used by LobbyServer after the JOIN handshake — reuses already-opened streams. */
    public SocketClientHandler(Socket socket, String nickname, GameSession session,
                               TotemColour totemColour,
                               BufferedReader prebuiltReader, PrintWriter prebuiltWriter) {
        this.socket      = socket;
        this.nickname    = nickname;
        this.totemColour = totemColour;
        this.session     = session;
        this.reader      = prebuiltReader;
        this.writer      = prebuiltWriter;
    }

    @Override
    public void run() {
        LOG.info("CONNECT nick=" + nickname
                + " handler=" + System.identityHashCode(this));
        try {
            if (reader == null)
                reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            if (writer == null)
                writer = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            new Thread(this::writeLoop,    "writer-"   + nickname).start();
            new Thread(this::watchdogLoop, "watchdog-" + nickname).start();
            readLoop();
        } catch (IOException e) {
            if (!closed.get()) LOG.warning("Errore di lettura per '" + nickname + "': " + e.getMessage());
        } finally {
            close();
            reportDisconnect();
        }
    }

    // ── Lettura ───────────────────────────────────────────────────────────────

    private void readLoop() throws IOException {
        LOG.info("READ LOOP START " + nickname);
        String line;
        while (!closed.get() && (line = reader.readLine()) != null) {
            lastSeenTime = System.currentTimeMillis(); // qualsiasi dato = client vivo
            try {
                Message msg = Message.decode(line);
                // HEARTBEAT e PONG (legacy): aggiornano solo il timestamp
                if (Message.HEARTBEAT.equals(msg.getType())
                        || Message.PONG.equals(msg.getType())) {
                    continue;
                }
                session.handleMessage(msg, this);
            } catch (Exception e) {
                send(Message.error("Messaggio malformato: " + e.getMessage()));
            }
        }
    }

    // ── Scrittura ─────────────────────────────────────────────────────────────

    private void writeLoop() {
        LOG.info("WRITE LOOP START " + nickname);
        while (!closed.get()) {
            try {
                Message msg = outbound.take();
                writer.println(msg.encode());
                if (writer.checkError()) break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // ── Watchdog ──────────────────────────────────────────────────────────────

    private void watchdogLoop() {
        while (!closed.get()) {
            try {
                Thread.sleep(WATCHDOG_INTERVAL_MS);
                if (closed.get()) return;

                long elapsed = System.currentTimeMillis() - lastSeenTime;
                if (elapsed > HEARTBEAT_TIMEOUT_MS) {
                    LOG.warning("Timeout heartbeat per '" + nickname
                            + "' (" + elapsed + " ms senza messaggi).");
                    close();
                    reportDisconnect();
                    return;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    // ── Disconnessione (chiamata al massimo una volta) ─────────────────────────

    private void reportDisconnect() {
        if (disconnectReported.compareAndSet(false, true)) {
            LOG.info("DISCONNECT nick=" + nickname
                    + " handler=" + System.identityHashCode(this));
            session.onPlayerDisconnected(nickname, this);
        }
    }

    // ── ClientConnection ──────────────────────────────────────────────────────

    @Override public String getNickname()    { return nickname; }
    @Override public TotemColour getTotemColour() { return totemColour; }

    @Override
    public void send(Message m) {
        if (!closed.get()) outbound.offer(m);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            // shutdownInput/Output first: unblocks readLine() and flushes writer
            // without relying on close() order — avoids silent blocking.
            try { socket.shutdownInput();  } catch (IOException ignored) {}
            try { socket.shutdownOutput(); } catch (IOException ignored) {}
            try { if (reader != null) reader.close(); } catch (IOException ignored) {}
            try { if (writer != null) writer.close(); } catch (Exception  ignored) {}
            try { socket.close(); }                     catch (IOException ignored) {}
        }
    }
}