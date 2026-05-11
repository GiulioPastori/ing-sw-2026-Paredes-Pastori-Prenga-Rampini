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
 * Gestore TCP lato server per un singolo client connesso.
 *
 * <p>Tre thread per client:
 * <ul>
 *   <li><b>reader</b> – legge i messaggi in arrivo (thread principale via {@link #run()}).</li>
 *   <li><b>writer</b> – svuota la coda di uscita {@link BlockingQueue}.</li>
 *   <li><b>watchdog</b> – controlla ogni {@value #WATCHDOG_INTERVAL_MS} ms se il client
 *       ha inviato almeno un messaggio negli ultimi {@value #HEARTBEAT_TIMEOUT_MS} ms;
 *       se scade, chiude la connessione.</li>
 * </ul>
 *
 * grazie all'{@link AtomicBoolean} {@code disconnectReported}: sia il readLoop
 * che il watchdog possono rilevare la disconnessione, ma solo il primo a farlo
 * notifica la sessione.
 */
public class SocketClientHandler implements ClientConnection, Runnable {

    private static final Logger LOG = Logger.getLogger(SocketClientHandler.class.getName());

    /** Intervallo di controllo del watchdog (ms). */
    static final long WATCHDOG_INTERVAL_MS = 5_000;

    /** Silenzio massimo dal client prima di considerarlo disconnesso (ms).
     *  Con heartbeat ogni 5s lato client, 30s = 6 heartbeat di tolleranza —
     *  abbastanza per un utente lento ma reattivo in caso di crash reale. */
    static final long HEARTBEAT_TIMEOUT_MS = 30_000;

    private final Socket                 socket;
    private final String                 nickname;
    private final TotemColour            totemColour;
    private final GameSession            session;
    private final BlockingQueue<Message> outbound = new LinkedBlockingQueue<>();

    private BufferedReader reader;
    private PrintWriter    writer;

    /** true dopo close(): impedisce operazioni multiple di chiusura. */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * true dopo la prima segnalazione di disconnessione alla sessione.
     * Garantisce che {@link GameSession#onPlayerDisconnected(String)} venga
     * invocata una sola volta anche se readLoop e watchdog la rilevano entrambi.
     */
    private final AtomicBoolean disconnectReported = new AtomicBoolean(false);

    /** Timestamp dell'ultimo messaggio ricevuto dal client (qualsiasi tipo). */
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

    // ── Lettura ───────────────────────────────────────────────────────────────

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

    // ── Scrittura ─────────────────────────────────────────────────────────────

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

    // ── Disconnessione (chiamata al massimo una volta) ─────────────────────────

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