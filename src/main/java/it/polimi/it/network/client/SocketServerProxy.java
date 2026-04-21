package it.polimi.it.network.client;

import it.polimi.it.network.message.Message;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Client-side TCP proxy.
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

    @Override public void setOnMessage(Consumer<Message> l)  { this.onMessage    = l; }
    @Override public void setOnDisconnect(Runnable l)        { this.onDisconnect  = l; }
    @Override public boolean isConnected()                   { return connected; }

    @Override
    public void connect(String nickname, int numPlayers) throws IOException {
        socket = new Socket(host, port);
        // FIX: explicit UTF-8 charset on both streams
        reader    = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer    = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        connected = true;
        writer.println(Message.join(nickname, numPlayers).encode());
        Thread t = new Thread(this::readLoop, "client-reader");
        t.setDaemon(true);
        t.start();
        LOG.info("Connected to " + host + ":" + port + " as '" + nickname + "'");
    }

    @Override
    public void send(Message message) {
        if (!connected) return;
        writer.println(message.encode());
    }

    @Override
    public void disconnect() {
        connected = false;
        try { socket.close(); } catch (IOException ignored) {}
    }

    private void readLoop() {
        try {
            String line;
            while (connected && (line = reader.readLine()) != null) {
                Message msg = Message.decode(line);
                if (Message.PING.equals(msg.getType())) {
                    send(Message.pong());
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