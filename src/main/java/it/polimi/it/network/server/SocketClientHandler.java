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

/** TCP handler on the server side for a single connected client.
        *
        * <p>Three threads per client:
        * <ul>
* <li><b>reader</b> – reads incoming messages (main thread via {@link #run()}).</li>
        * <li><b>writer</b> – empties the outgoing {@link BlockingQueue}.</li>
        * <li><b>watchdog</b> – checks every {@value #WATCHDOG_INTERVAL_MS} ms if the client
* has sent at least one message in the last {@value #HEARTBEAT_TIMEOUT_MS} ms;
* if it expires, closes the connection.</li>
        * </ul>
        *
        * thanks to {@link AtomicBoolean} {@code disconnectReported}: both the readLoop
* and the watchdog can detect the disconnection, but only the first to do so
* notifies the session.*/

public class SocketClientHandler implements ClientConnection, Runnable {

    private static final Logger LOG = Logger.getLogger(SocketClientHandler.class.getName());

    /** Watchdog control interval (ms). */
    static final long WATCHDOG_INTERVAL_MS = 5_000;

    /** Maximum silence from the client before considering it disconnected (ms).
     * With heartbeat every 5s on the client side, 30s = 6 heartbeat tolerance —
     * enough for a slow but responsive user in case of a real crash. */
    static final long HEARTBEAT_TIMEOUT_MS = 30_000;

    private final Socket                 socket;
    private final String                 nickname;
    private final TotemColour            totemColour;
    private final GameSession            session;
    private final BlockingQueue<Message> outbound = new LinkedBlockingQueue<>();

    private BufferedReader reader;
    private PrintWriter    writer;

    /** true after close(): prevents multiple close operations. */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * true after the first disconnection report to the session.
     * Ensures that {@link GameSession#onPlayerDisconnected(String)} is
     * invoked only once even if both readLoop and watchdog detect it.
     */
    private final AtomicBoolean disconnectReported = new AtomicBoolean(false);

    /** Timestamp of the last message received from the client (any type). */
    private volatile long lastSeenTime = System.currentTimeMillis();

    public SocketClientHandler(Socket socket, String nickname, GameSession session, TotemColour totemColour) {
        this.socket   = socket;
        this.nickname     = nickname;
        this.totemColour  = totemColour;
        this.session  = session;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
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

    // ── Reading ───────────────────────────────────────────────────────────────

    private void readLoop() throws IOException {
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

    // ── writing ─────────────────────────────────────────────────────────────

    private void writeLoop() {
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

    // ── Disconnection (call maximum one time) ─────────────────────────

    private void reportDisconnect() {
        if (disconnectReported.compareAndSet(false, true)) {
            session.onPlayerDisconnected(nickname);
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
            try { if (reader != null) reader.close(); } catch (IOException ignored) {}
            try { if (writer != null) writer.close(); } catch (Exception  ignored) {}
            try { socket.close(); }                     catch (IOException ignored) {}
        }
    }
}