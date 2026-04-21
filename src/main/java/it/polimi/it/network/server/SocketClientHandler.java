package it.polimi.it.network.server;

import it.polimi.it.network.message.Message;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/**
 * Server-side TCP client handler. Uses Message (org.json).
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
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            new Thread(this::writeLoop, "writer-" + nickname).start();
            new Thread(this::pingLoop,  "ping-"   + nickname).start();
            readLoop();
        } catch (IOException e) {
            if (!closed) LOG.warning("Error for '" + nickname + "': " + e.getMessage());
        } finally {
            close();
            session.onPlayerDisconnected(nickname);
        }
    }

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

    private void pingLoop() {
        while (!closed) {
            try {
                Thread.sleep(PING_INTERVAL_MS);
                send(Message.ping());
                Thread.sleep(PONG_TIMEOUT_MS);
                if (System.currentTimeMillis() - lastPongTime > PONG_TIMEOUT_MS) {
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

    @Override public String  getNickname() { return nickname; }
    @Override public void    send(Message m) { if (!closed) outbound.offer(m); }
    @Override public void    close() {
        if (closed) return;
        closed = true;
        try { socket.close(); } catch (IOException ignored) {}
    }
}