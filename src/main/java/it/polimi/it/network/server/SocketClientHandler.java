package it.polimi.it.network.server;

import it.polimi.it.network.message.*;

        import java.io.*;
        import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/**
 * Handles a single TCP client connection on the server side.
 *
 * Runs three internal threads:
 * - Reader: reads JSON lines from the socket, decodes them and forwards
 *           them to GameSession
 * - Writer: drains an outbound queue and writes JSON lines to the socket
 * - Ping:   sends a PING every PING_INTERVAL_MS; if no PONG arrives within
 *           PONG_TIMEOUT_MS the client is considered dead
 *
 * Using a BlockingQueue for outbound messages means send() never blocks
 * the caller (typically the GameSession event broadcast).
 */
public class SocketClientHandler implements ClientConnection, Runnable {

    private static final Logger LOG              = Logger.getLogger(SocketClientHandler.class.getName());
    private static final long   PING_INTERVAL_MS = 5_000;
    private static final long   PONG_TIMEOUT_MS  = 10_000;

    private final Socket                 socket;
    private final String                 nickname;
    private final GameSession            session;
    private final BlockingQueue<Message> outboundQueue = new LinkedBlockingQueue<>();

    private BufferedReader   reader;
    private PrintWriter      writer;
    private volatile boolean closed        = false;
    private volatile long    lastPongTime  = System.currentTimeMillis();

    public SocketClientHandler(Socket socket, String nickname, GameSession session) {
        this.socket   = socket;
        this.nickname = nickname;
        this.session  = session;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            Thread writerThread = new Thread(this::writeLoop, "writer-" + nickname);
            writerThread.setDaemon(true);
            writerThread.start();

            Thread pingThread = new Thread(this::pingLoop, "ping-" + nickname);
            pingThread.setDaemon(true);
            pingThread.start();

            readLoop();

        } catch (IOException e) {
            if (!closed) LOG.warning("Connection error for '" + nickname + "': " + e.getMessage());
        } finally {
            close();
            session.onPlayerDisconnected(nickname);
        }
    }

    // ── Read loop ─────────────────────────────────────────────────────────────

    private void readLoop() throws IOException {
        String line;
        while (!closed && (line = reader.readLine()) != null) {
            try {
                Message msg = MessageCodec.decode(line);
                if (msg instanceof PongMessage) {
                    lastPongTime = System.currentTimeMillis();
                } else {
                    session.handleMessage(msg, this);
                }
            } catch (IOException e) {
                LOG.warning("Malformed message from '" + nickname + "': " + line);
                send(new ErrorMessage("Malformed message: " + e.getMessage()));
            }
        }
    }

    // ── Write loop ────────────────────────────────────────────────────────────

    private void writeLoop() {
        while (!closed) {
            try {
                Message msg  = outboundQueue.take();
                String  json = MessageCodec.encode(msg);
                writer.println(json);
                if (writer.checkError()) {
                    LOG.warning("Write error for '" + nickname + "'");
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                LOG.warning("Encode error: " + e.getMessage());
            }
        }
    }

    // ── Ping loop ─────────────────────────────────────────────────────────────

    private void pingLoop() {
        while (!closed) {
            try {
                Thread.sleep(PING_INTERVAL_MS);
                send(new PingMessage());
                Thread.sleep(PONG_TIMEOUT_MS);
                if (System.currentTimeMillis() - lastPongTime > PONG_TIMEOUT_MS) {
                    LOG.warning("Pong timeout for '" + nickname + "'");
                    close();
                    session.onPlayerDisconnected(nickname);
                    return;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    // ── ClientConnection ──────────────────────────────────────────────────────

    @Override public String getNickname() { return nickname; }

    @Override
    public void send(Message message) {
        if (!closed) outboundQueue.offer(message);
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        try { socket.close(); } catch (IOException ignored) {}
    }
}