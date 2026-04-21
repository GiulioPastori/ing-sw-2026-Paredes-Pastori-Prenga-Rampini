package it.polimi.it.network.server;

import it.polimi.it.network.message.Message;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/**
 * Server-side TCP client handler.
 *
 * <p>Each connected client runs three threads:
 * <ul>
 *   <li><b>reader</b> – the thread calling {@link #run()}, reads incoming lines.</li>
 *   <li><b>writer</b> – drains the outbound {@link BlockingQueue}.</li>
 *   <li><b>ping</b>   – sends periodic PINGs and closes the connection on timeout.</li>
 * </ul>
 */
public class SocketClientHandler implements ClientConnection, Runnable {

    private static final Logger LOG              = Logger.getLogger(SocketClientHandler.class.getName());
    private static final long   PING_INTERVAL_MS = 1000_000;
    private static final long   PONG_TIMEOUT_MS  = 1800_000;

    private final Socket                 socket;
    private final String                 nickname;
    private final GameSession            session;
    private final BlockingQueue<Message> outbound = new LinkedBlockingQueue<>();

    private BufferedReader   reader;
    private PrintWriter      writer;
    private volatile boolean closed       = false;
    private volatile long    lastPongTime = System.currentTimeMillis();

    public SocketClientHandler(Socket socket, String nickname, GameSession session) {
        this.socket   = socket;
        this.nickname = nickname;
        this.session  = session;
    }

    @Override
    public void run() {
        try {
            // FIX #2: explicit UTF-8 charset on both streams
            reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            new Thread(this::writeLoop, "writer-" + nickname).start();
            new Thread(this::pingLoop,  "ping-"   + nickname).start();
            readLoop();
        } catch (IOException e) {
            if (!closed) LOG.warning("Read error for '" + nickname + "': " + e.getMessage());
        } finally {
            close();
            session.onPlayerDisconnected(nickname);
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    private void readLoop() throws IOException {
        String line;
        while (!closed && (line = reader.readLine()) != null) {
            try {
                Message msg = Message.decode(line);
                if (Message.PONG.equals(msg.getType())) {
                    lastPongTime = System.currentTimeMillis();
                } else {
                    session.handleMessage(msg, this);
                }
            } catch (Exception e) {
                send(Message.error("Malformed message: " + e.getMessage()));
            }
        }
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    private void writeLoop() {
        while (!closed) {
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

    // ── Ping ──────────────────────────────────────────────────────────────────

    private void pingLoop() {
        while (!closed) {
            try {
                Thread.sleep(PING_INTERVAL_MS);
                if (closed) return;
                send(Message.ping());
                Thread.sleep(PONG_TIMEOUT_MS);
                if (!closed && System.currentTimeMillis() - lastPongTime > PONG_TIMEOUT_MS) {
                    LOG.warning("Ping timeout for '" + nickname + "' — closing connection.");
                    close(); // readLoop will notice and notify session via run()'s finally
                    return;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    // ── ClientConnection ──────────────────────────────────────────────────────

    @Override
    public String getNickname() { return nickname; }

    @Override
    public void send(Message m) {
        if (!closed) outbound.offer(m);
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        try { if (reader != null) reader.close(); } catch (IOException ignored) {}
        try { if (writer != null) writer.close(); } catch (Exception ignored) {}
        try { socket.close(); }                     catch (IOException ignored) {}
    }
}