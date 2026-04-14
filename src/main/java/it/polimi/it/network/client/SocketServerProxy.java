package it.polimi.it.network.client;

import it.polimi.it.network.message.*;

        import java.io.*;
        import java.net.Socket;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Client-side proxy for the server connection over TCP Socket.
 *
 * Maintains a persistent TCP connection to the server.
 * A background reader thread continuously reads JSON lines and delivers
 * them to the registered onMessage listener.
 * Responds automatically to PING messages with PONG (heartbeat).
 */
public class SocketServerProxy implements ServerProxy {

    private static final Logger LOG = Logger.getLogger(SocketServerProxy.class.getName());

    private final String host;
    private final int    port;

    private Consumer<Message> onMessage;
    private Runnable          onDisconnect;

    private Socket         socket;
    private BufferedReader reader;
    private PrintWriter    writer;
    private volatile boolean connected = false;

    public SocketServerProxy(String host, int port) {
        this.host = host;
        this.port = port;
    }

    // ── ServerProxy ───────────────────────────────────────────────────────────

    @Override
    public void setOnMessage(Consumer<Message> listener)   { this.onMessage    = listener; }

    @Override
    public void setOnDisconnect(Runnable listener)         { this.onDisconnect  = listener; }

    @Override
    public void connect(String nickname, int numPlayers) throws IOException {
        socket    = new Socket(host, port);
        reader    = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer    = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        connected = true;

        // Send JOIN as the very first message
        sendRaw(new JoinMessage(nickname, numPlayers));

        // Start background reader thread
        Thread t = new Thread(this::readLoop, "client-reader");
        t.setDaemon(true);
        t.start();

        LOG.info("Connected to " + host + ":" + port + " as '" + nickname + "'");
    }

    @Override
    public void send(Message message) {
        if (!connected) return;
        sendRaw(message);
    }

    @Override
    public void disconnect() {
        if (!connected) return;
        connected = false;
        try { socket.close(); } catch (IOException ignored) {}
    }

    @Override
    public boolean isConnected() { return connected; }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void sendRaw(Message message) {
        try {
            writer.println(MessageCodec.encode(message));
            if (writer.checkError()) LOG.warning("Write error — connection may be lost.");
        } catch (IOException e) {
            LOG.warning("Failed to send message: " + e.getMessage());
        }
    }

    private void readLoop() {
        try {
            String line;
            while (connected && (line = reader.readLine()) != null) {
                Message msg = MessageCodec.decode(line);
                if (msg instanceof PingMessage) {
                    sendRaw(new PongMessage()); // auto-reply to heartbeat
                } else if (onMessage != null) {
                    onMessage.accept(msg);
                }
            }
        } catch (IOException e) {
            if (connected) LOG.warning("Connection lost: " + e.getMessage());
        } finally {
            connected = false;
            if (onDisconnect != null) onDisconnect.run();
        }
    }
}