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
 * Server-side TCP handler for one connected client.
 *
 * <p>Each client uses three threads:
 * <ul>
 *   <li><b>reader</b>: reads inbound messages on the main handler thread via
 *       {@link #run()}.</li>
 *   <li><b>writer</b>: drains the outbound {@link BlockingQueue}.</li>
 *   <li><b>watchdog</b>: every {@value #WATCHDOG_INTERVAL_MS} ms, checks
 *       whether the client has sent any message during the last
 *       {@value #HEARTBEAT_TIMEOUT_MS} ms; on timeout it closes the
 *       connection.</li>
 * </ul>
 *
 * <p>{@link #disconnectReported} guarantees that the session is notified once:
 * both the read loop and watchdog can detect disconnection, but only the first
 * one reports it.
 */
public class SocketClientHandler implements ClientConnection, Runnable {

    private static final Logger LOG = Logger.getLogger(SocketClientHandler.class.getName());

    /** Watchdog polling interval, in milliseconds. */
    static final long WATCHDOG_INTERVAL_MS = 5_000;

    /**
     * Maximum client silence before considering it disconnected, in
     * milliseconds. With a client heartbeat every 5 seconds, 30 seconds allow
     * six missed heartbeats: enough for slow users while still reacting to real
     * crashes.
     */
    static final long HEARTBEAT_TIMEOUT_MS = 30_000;

    private final Socket                 socket;
    private final String                 nickname;
    private final TotemColour            totemColour;
    private final GameSession            session;
    private final BlockingQueue<Message> outbound = new LinkedBlockingQueue<>();

    private BufferedReader reader;
    private PrintWriter    writer;

    /** True after close(), preventing repeated close operations. */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * True after the first disconnect report to the session. Guarantees that
     * {@link GameSession#onPlayerDisconnected(String, ClientConnection)} is invoked only once,
     * even if both the read loop and watchdog detect the disconnection.
     */
    private final AtomicBoolean disconnectReported = new AtomicBoolean(false);

    /** Timestamp of the last message received from the client, of any type. */
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
            if (!closed.get()) LOG.warning("Read error for '" + nickname + "': " + e.getMessage());
        } finally {
            close();
            reportDisconnect();
        }
    }

    // Reading.

    private void readLoop() throws IOException {
        LOG.info("READ LOOP START " + nickname);
        String line;
        while (!closed.get() && (line = reader.readLine()) != null) {
            lastSeenTime = System.currentTimeMillis(); // any data means the client is alive
            try {
                Message msg = Message.decode(line);
                // HEARTBEAT and legacy PONG only refresh the timestamp.
                if (Message.HEARTBEAT.equals(msg.getType())
                        || Message.PONG.equals(msg.getType())) {
                    continue;
                }
                session.handleMessage(msg, this);
            } catch (Exception e) {
                send(Message.error("Malformed message: " + e.getMessage()));
            }
        }
    }

    // Writing.

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

    // Watchdog.

    private void watchdogLoop() {
        while (!closed.get()) {
            try {
                Thread.sleep(WATCHDOG_INTERVAL_MS);
                if (closed.get()) return;

                long elapsed = System.currentTimeMillis() - lastSeenTime;
                if (elapsed > HEARTBEAT_TIMEOUT_MS) {
                    LOG.warning("Heartbeat timeout for '" + nickname
                            + "' (" + elapsed + " ms without messages).");
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

    // Disconnect reporting, called at most once.

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
