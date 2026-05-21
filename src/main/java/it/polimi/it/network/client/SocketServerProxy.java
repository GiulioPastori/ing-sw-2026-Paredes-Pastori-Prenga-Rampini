package it.polimi.it.network.client;


import it.polimi.it.enums.TotemColour;
import it.polimi.it.network.message.Message;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Proxy TCP lato client.
 *
 * <p>Gestisce tre thread:
 * <ul>
 *   <li><b>reader</b> — legge i messaggi in arrivo dal server.</li>
 *   <li><b>heartbeat</b> — invia un {@link Message#HEARTBEAT} ogni
 *       {@value #HEARTBEAT_INTERVAL_MS} ms per tenere viva la connessione
 *       e permettere al watchdog del server di rilevare disconnessioni reali.</li>
 * </ul>
 */
public class SocketServerProxy implements ServerProxy {

    private static final Logger LOG = Logger.getLogger(SocketServerProxy.class.getName());

    /** Intervallo di invio heartbeat dal client al server (ms). */
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

        // Usa joinWithSession: include sessionId nel payload se non-null
        writer.println(Message.joinWithSession(
                nickname, numPlayers, totemColour.name(), sessionId).encode());

        Thread readerThread = new Thread(this::readLoop, "client-reader");
        readerThread.setDaemon(true);
        readerThread.start();

        Thread heartbeatThread = new Thread(this::heartbeatLoop, "client-heartbeat");
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();

        LOG.info("Connesso a " + host + ":" + port + " come '" + nickname + "'"
                + (sessionId != null ? " sessione=" + sessionId : ""));
    }

    @Override
    public Message fetchSessionList(int numPlayersFilter) throws IOException {
        // Connessione temporanea: invia LIST_SESSIONS, legge SESSION_LIST, chiude subito.
        try (java.net.Socket tmp = new java.net.Socket(host, port);
             java.io.BufferedReader r = new java.io.BufferedReader(
                     new java.io.InputStreamReader(tmp.getInputStream(), StandardCharsets.UTF_8));
             java.io.PrintWriter w = new java.io.PrintWriter(
                     new java.io.OutputStreamWriter(tmp.getOutputStream(), StandardCharsets.UTF_8), true)) {

            w.println(Message.listSessions(numPlayersFilter).encode());
            String line = r.readLine();
            if (line == null) throw new IOException("Nessuna risposta dal server.");
            Message resp = Message.decode(line);
            if (!Message.SESSION_LIST.equals(resp.getType()))
                throw new IOException("Risposta inattesa: " + resp.getType());
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

    // ── Lettura messaggi dal server ───────────────────────────────────────────

    private void readLoop() {
        try {
            String line;
            while (connected && (line = reader.readLine()) != null) {
                Message msg = Message.decode(line);
                // PING legacy: risponde con PONG per compatibilità
                if (Message.PING.equals(msg.getType())) {
                    send(Message.pong());
                } else if (onMessage != null) {
                    onMessage.accept(msg);
                }
            }
        } catch (IOException e) {
            if (connected) LOG.warning("Connessione persa: " + e.getMessage());
        } finally {
            connected = false;
            if (onDisconnect != null) onDisconnect.run();
        }
    }

    // ── Heartbeat periodico verso il server ───────────────────────────────────

    /**
     * Invia un {@link Message#HEARTBEAT} ogni {@value #HEARTBEAT_INTERVAL_MS} ms.
     * Il server usa questi messaggi per rilevare client non più raggiungibili.
     * Qualsiasi messaggio di gioco fa già da heartbeat implicito, ma questo loop
     * garantisce segnali anche durante le lunghe attese di input dell'utente.
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