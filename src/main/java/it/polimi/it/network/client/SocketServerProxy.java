package it.polimi.it.network.client;


import it.polimi.it.enums.TotemColour;
import it.polimi.it.network.message.Message;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * The TCP (socket) proxy on the client side.
 *
 * <p>It is what the rest of the client uses to talk to the server over a plain
 * socket. Messages travel as JSON text, one per line: a message is written
 * with {@code encode()} and read back with {@code decode()}.
 *
 * <p>When it connects, it starts two daemon threads:
 * <ul>
 *   <li><b>reader</b> — keeps reading lines from the server. Each line is
 *       turned into a {@link Message} and passed to the {@code onMessage}
 *       callback. If reading returns nothing or fails, this is how the proxy
 *       knows the connection is gone: it marks itself as not connected and runs
 *       the {@code onDisconnect} callback.</li>
 *   <li><b>heartbeat</b> — sends a {@code HEARTBEAT} every
 *       {@value #HEARTBEAT_INTERVAL_MS} ms. Any game message already works as a
 *       heartbeat, but this loop keeps sending one even while the user is slow
 *       to act, so the server does not think the client is gone.</li>
 * </ul>
 */
public class SocketServerProxy implements ServerProxy {

    private static final Logger LOG = Logger.getLogger(SocketServerProxy.class.getName());


    private static final long HEARTBEAT_INTERVAL_MS = 5_000;

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

    @Override public void setOnMessage(Consumer<Message> l) { this.onMessage   = l; }
    @Override public void setOnDisconnect(Runnable l)       { this.onDisconnect = l; }
    @Override public boolean isConnected()                  { return connected; }

    @Override
    public void connect(String nickname, int numPlayers, TotemColour totemColour) throws IOException {
        connectWithSession(nickname, numPlayers, totemColour, null);
    }

    @Override
    public void connectWithSession(String nickname, int numPlayers,
                                   TotemColour totemColour, String sessionId) throws IOException {
        socket    = new Socket(host, port);
        reader    = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer    = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        connected = true;

        // Use joinWithSession so a non-null sessionId is included in the payload.
        writer.println(Message.joinWithSession(
                nickname, numPlayers, totemColour.name(), sessionId).encode());

        Thread readerThread = new Thread(this::readLoop, "client-reader");
        readerThread.setDaemon(true);
        readerThread.start();

        Thread heartbeatThread = new Thread(this::heartbeatLoop, "client-heartbeat");
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();

        LOG.info("Connected to " + host + ":" + port + " as '" + nickname + "'"
                + (sessionId != null ? " session=" + sessionId : ""));
    }

    @Override
    public Message fetchSessionList(int numPlayersFilter) throws IOException {
        // Temporary connection: send LIST_SESSIONS, read SESSION_LIST, then close.
        try (java.net.Socket tmp = new java.net.Socket(host, port);
             java.io.BufferedReader r = new java.io.BufferedReader(
                     new java.io.InputStreamReader(tmp.getInputStream(), StandardCharsets.UTF_8));
             java.io.PrintWriter w = new java.io.PrintWriter(
                     new java.io.OutputStreamWriter(tmp.getOutputStream(), StandardCharsets.UTF_8), true)) {

            w.println(Message.listSessions(numPlayersFilter).encode());
            String line = r.readLine();
            if (line == null) throw new IOException("No response from the server.");
            Message resp = Message.decode(line);
            if (!Message.SESSION_LIST.equals(resp.getType()))
                throw new IOException("Unexpected response: " + resp.getType());
            return resp;
        }
    }

    @Override
    public void send(Message message) {
        if (!connected || writer == null) return;
        writer.println(message.encode());
    }

    @Override
    public void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    // Reads messages from the server.

    private void readLoop() {
        try {
            String line;
            while (connected && (line = reader.readLine()) != null) {
                Message msg = Message.decode(line);
                // Legacy PING support: answer with PONG for compatibility.
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

    // Periodic heartbeat sent to the server.

    /**
     * Keeps the connection alive while the user is not sending game actions.
     */
    private void heartbeatLoop() {
        while (connected) {
            try {
                Thread.sleep(HEARTBEAT_INTERVAL_MS);
                if (connected) send(Message.heartbeat());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
