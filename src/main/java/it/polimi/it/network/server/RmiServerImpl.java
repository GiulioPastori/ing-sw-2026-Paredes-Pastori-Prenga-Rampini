package it.polimi.it.network.server;

import it.polimi.it.enums.TotemColour;
import it.polimi.it.network.message.Message;
import it.polimi.it.network.rmi.RmiClient;
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
    public void register(String nickname, RmiClient client, int numPlayers, String totemColour)
            throws RemoteException {

        if (nickname == null || nickname.isBlank()) {
            client.receiveMessage(Message.error("Il nickname non può essere vuoto.").encode());
            return;
        }

        GameSession reconnectTarget = lobby.findSessionForReconnection(nickname);

        if (reconnectTarget == null) {
            if (!lobby.claimNickname(nickname)) {
                client.receiveMessage(
                        Message.error("Nickname '" + nickname + "' già in uso.").encode());
                return;
            }
        }

        TotemColour tc;
        try { tc = TotemColour.valueOf(totemColour); }
        catch (Exception e) { tc = TotemColour.RED; }
        RmiClientHandler handler = new RmiClientHandler(nickname, client, tc) {
            @Override public void close() {
                super.close();
                handlers.remove(nickname);
                if (getSession() != null && getSession().isTerminated())
                    lobby.releaseNickname(nickname);
            }
        };
        handlers.put(nickname, handler);

        if (reconnectTarget != null) {
            handler.setSession(reconnectTarget);
            reconnectTarget.reconnectClient(handler);
        } else {
            GameSession session = lobby.getOrCreateSession(numPlayers);
            handler.setSession(session);
            session.addClient(handler);
        }

        LOG.info("Giocatore RMI '" + nickname + "' registrato.");
    }

    @Override
    public void sendMessage(String json) throws RemoteException {
        Message msg = Message.decode(json);

        // HEARTBEAT: la chiamata RMI stessa dimostra che il client è vivo.
        if (Message.HEARTBEAT.equals(msg.getType())) return;

        String name = extractPlayerName(msg);
        if (name == null) return;
        RmiClientHandler handler = handlers.get(name);
        if (handler != null && handler.getSession() != null) {
            handler.getSession().handleMessage(msg, handler);
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String extractPlayerName(Message msg) {
        return switch (msg.getType()) {
            case Message.PLACE_TOTEM,
                 Message.TAKE_CARD,
                 Message.PASS,
                 Message.POST_ROUND -> msg.str("playerName");
            default -> null;
        };
    }
}

// ─────────────────────────────────────────────────────────────────────────────

/**
 * Avvolge un callback RMI come {@link ClientConnection}.
 *
 * <p><b>Send asincrono</b>: i messaggi in uscita vengono accodati in una
 * {@link BlockingQueue} e spediti da un thread daemon separato. In questo modo
 * {@link GameSession#} non viene mai bloccato da timeout RMI o
 * eccezioni di rete, e le eccezioni durante l'invio non interrompono il flusso
 * di gioco del server.
 */
class RmiClientHandler implements ClientConnection {

    private static final Logger LOG = Logger.getLogger(RmiClientHandler.class.getName());

    private final String                 nickname;
    private final RmiClient              stub;
    private final BlockingQueue<Message> outbound = new LinkedBlockingQueue<>();

    private GameSession session;
    private final java.util.concurrent.atomic.AtomicBoolean closed
            = new java.util.concurrent.atomic.AtomicBoolean(false);
    private final java.util.concurrent.atomic.AtomicBoolean disconnectReported
            = new java.util.concurrent.atomic.AtomicBoolean(false);

    private final TotemColour totemColour;
    RmiClientHandler(String nickname, RmiClient stub, TotemColour totemColour) {
        this.nickname = nickname;
        this.totemColour = totemColour;
        this.stub     = stub;

        // Thread daemon che invia i messaggi in coda via RMI
        Thread sender = new Thread(this::sendLoop, "rmi-sender-" + nickname);
        sender.setDaemon(true);
        sender.start();
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

    private void reportDisconnect() {
        if (disconnectReported.compareAndSet(false, true) && session != null) {
            session.onPlayerDisconnected(nickname);
        }
    }

    @Override
    public void close() {
        closed.set(true);
        outbound.clear();
    }
}