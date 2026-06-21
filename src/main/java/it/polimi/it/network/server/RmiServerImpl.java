package it.polimi.it.network.server;

import it.polimi.it.enums.TotemColour;
import it.polimi.it.network.message.Message;
import it.polimi.it.network.rmi.RmiClient;
import it.polimi.it.network.rmi.RmiPlayerSession;
import it.polimi.it.network.rmi.RmiServer;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/**
 * Implementazione RMI lato server.
 *
 * <p>Esportata nel registry RMI sotto {@value #REGISTRY_NAME}.
 * Condivide {@link LobbyServer} con il server Socket per partite miste.
 *
 * <p>Ogni client RMI è avvolto in un {@link RmiClientHandler} che implementa
 * {@link ClientConnection}, rendendo il trasporto trasparente a {@link GameSession}.
 */
public class RmiServerImpl extends UnicastRemoteObject implements RmiServer {

    private static final Logger LOG           = Logger.getLogger(RmiServerImpl.class.getName());
    public  static final String REGISTRY_NAME = "MesosServer";

    private final LobbyServer                   lobby;
    private final Map<String, RmiClientHandler> handlers = new ConcurrentHashMap<>();

    public RmiServerImpl(LobbyServer lobby, int rmiPort) throws RemoteException {
        super();
        this.lobby = lobby;
        Registry registry = LocateRegistry.createRegistry(rmiPort);
        registry.rebind(REGISTRY_NAME, this);
        LOG.info("RMI registry avviato sulla porta " + rmiPort);
    }

    // ── RmiServer ─────────────────────────────────────────────────────────────

    @Override
    public RmiPlayerSession register(String nickname, RmiClient client, int numPlayers, String totemColour)
            throws RemoteException {
        return registerWithSession(nickname, client, numPlayers, totemColour, null);
    }

    @Override
    public RmiPlayerSession registerWithSession(String nickname, RmiClient client,
                                                int numPlayers, String totemColour, String sessionId)
            throws RemoteException {

        if (nickname == null || nickname.isBlank()) {
            client.receiveMessage(Message.error("Il nickname non può essere vuoto.").encode());
            return null;
        }

        GameSession reconnectTarget = lobby.findSessionForReconnection(nickname);
        boolean claimedNickname = false;

        if (reconnectTarget == null) {
            if (!lobby.claimNickname(nickname)) {
                client.receiveMessage(
                        Message.error("Nickname '" + nickname + "' già in uso.").encode());
                return null;
            }
            claimedNickname = true;
        }

        TotemColour tc;
        try { tc = TotemColour.valueOf(totemColour); }
        catch (Exception e) { tc = TotemColour.RED; }

        RmiClientHandler handler = new RmiClientHandler(nickname, client, tc) {
            @Override public void close() {
                super.close();
                handlers.remove(nickname, this);
                if (getSession() != null && getSession().isTerminated())
                    lobby.releaseNickname(nickname);
            }
        };
        handlers.put(nickname, handler);

        try {
            if (reconnectTarget != null) {
                handler.setSession(reconnectTarget);
                reconnectTarget.reconnectClient(handler);
            } else {
                GameSession session;
                if (LobbyServer.NEW_SESSION.equals(sessionId)) {
                    // Utente ha scelto "Crea nuova partita"
                    session = lobby.getOrCreateSession(numPlayers, true);
                } else if (sessionId != null && !sessionId.isBlank()) {
                    session = lobby.getSessionById(sessionId);
                    if (session == null) {
                        // Race condition: fallback a nuova sessione
                        session = lobby.getOrCreateSession(numPlayers, true);
                        client.receiveMessage(Message.error(
                                "La sessione scelta non è più disponibile. "
                                        + "Sei stato assegnato a una nuova sessione.").encode());
                    }
                } else {
                    session = lobby.getOrCreateSession(numPlayers, false);
                }
                handler.setSession(session);
                if (!session.addClient(handler)) {
                    client.receiveMessage(Message.error(
                            "La sessione non è più disponibile.").encode());
                    handler.close();
                    if (claimedNickname) lobby.releaseNickname(nickname);
                    return null;
                }
            }
        } catch (RuntimeException e) {
            handler.close();
            if (claimedNickname) lobby.releaseNickname(nickname);
            throw e;
        }

        LOG.info("Giocatore RMI '" + nickname + "' registrato (sessionId=" + sessionId + ").");
        return handler;
    }

    @Override
    public java.util.List<Message.SessionInfo> listSessions(int numPlayersFilter) throws RemoteException {
        return lobby.getOpenSessions(numPlayersFilter);
    }
}

// ─────────────────────────────────────────────────────────────────────────────

/**
 * Avvolge un callback RMI come {@link ClientConnection}.
 *
 * <p><b>Send asincrono</b>: i messaggi in uscita vengono accodati in una
 * {@link BlockingQueue} e spediti da un thread daemon separato. In questo modo
 * {@link GameSession} non viene mai bloccato da timeout RMI o
 * eccezioni di rete, e le eccezioni durante l'invio non interrompono il flusso
 * di gioco del server.
 */
class RmiClientHandler extends UnicastRemoteObject
        implements ClientConnection, RmiPlayerSession {

    private static final Logger LOG = Logger.getLogger(RmiClientHandler.class.getName());

    private final String                 nickname;
    private final RmiClient              stub;
    private final BlockingQueue<Message> outbound = new LinkedBlockingQueue<>();

    private volatile GameSession session;
    private final java.util.concurrent.atomic.AtomicBoolean closed
            = new java.util.concurrent.atomic.AtomicBoolean(false);
    private final java.util.concurrent.atomic.AtomicBoolean disconnectReported
            = new java.util.concurrent.atomic.AtomicBoolean(false);

    private volatile long lastSeenTime = System.currentTimeMillis();
    private final Thread senderThread;
    private final Thread watchdogThread;

    private final TotemColour totemColour;
    RmiClientHandler(String nickname, RmiClient stub, TotemColour totemColour)
            throws RemoteException {
        super();
        this.nickname = nickname;
        this.totemColour = totemColour;
        this.stub     = stub;

        // Thread daemon che invia i messaggi in coda via RMI
        senderThread = new Thread(this::sendLoop, "rmi-sender-" + nickname);
        senderThread.setDaemon(true);
        senderThread.start();

        watchdogThread = new Thread(this::watchdogLoop, "rmi-watchdog-" + nickname);
        watchdogThread.setDaemon(true);
        watchdogThread.start();
    }

    void        setSession(GameSession s) { this.session = s; }
    GameSession getSession()              { return session; }

    @Override public TotemColour getTotemColour() { return totemColour; }
    @Override public String getNickname() { return nickname; }

    /**
     * Aggiunge il messaggio alla coda di uscita. Non bloccante.
     * Il thread sender si occupa dell'invio effettivo via RMI.
     */
    @Override
    public void send(Message message) {
        if (!closed.get()) outbound.offer(message);
    }

    @Override
    public void placeTotem(char tileLetter) throws RemoteException {
        handleRemoteAction(Message.placeTotem(nickname, tileLetter));
    }

    @Override
    public void takeCard(int cardId, boolean fromTopRow) throws RemoteException {
        handleRemoteAction(Message.takeCard(nickname, cardId, fromTopRow));
    }

    @Override
    public void pass() throws RemoteException {
        handleRemoteAction(Message.pass(nickname));
    }

    @Override
    public void postRound(int cardId) throws RemoteException {
        handleRemoteAction(Message.postRound(nickname, cardId));
    }

    @Override
    public void heartbeat() throws RemoteException {
        touch();
    }

    @Override
    public void disconnect() throws RemoteException {
        close();
        reportDisconnect();
    }

    private void handleRemoteAction(Message message) throws RemoteException {
        touch();
        if (closed.get()) throw new RemoteException("Sessione RMI chiusa.");
        GameSession currentSession = session;
        if (currentSession == null) throw new RemoteException("Sessione RMI non pronta.");
        currentSession.handleMessage(message, this);
    }

    private void touch() {
        lastSeenTime = System.currentTimeMillis();
    }

    private void sendLoop() {
        while (!closed.get()) {
            try {
                Message msg = outbound.take();
                if (closed.get()) return;
                stub.receiveMessage(msg.encode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                LOG.warning("Invio RMI fallito per '" + nickname + "': " + e.getMessage());
                close();
                reportDisconnect();
                return;
            }
        }
    }

    private void watchdogLoop() {
        while (!closed.get()) {
            try {
                Thread.sleep(SocketClientHandler.WATCHDOG_INTERVAL_MS);
                if (closed.get()) return;

                long elapsed = System.currentTimeMillis() - lastSeenTime;
                if (elapsed > SocketClientHandler.HEARTBEAT_TIMEOUT_MS) {
                    LOG.warning("Timeout heartbeat RMI per '" + nickname
                            + "' (" + elapsed + " ms senza invocazioni).");
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

    private void reportDisconnect() {
        if (disconnectReported.compareAndSet(false, true) && session != null) {
            session.onPlayerDisconnected(nickname, this);
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            outbound.clear();
            senderThread.interrupt();
            watchdogThread.interrupt();
            try { UnicastRemoteObject.unexportObject(this, true); } catch (Exception ignored) {}
        }
    }
}
