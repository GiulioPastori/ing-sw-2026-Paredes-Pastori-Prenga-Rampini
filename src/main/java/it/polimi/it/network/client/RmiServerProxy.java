package it.polimi.it.network.client;

import it.polimi.it.network.message.Message;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.network.rmi.RmiClient;
import it.polimi.it.network.rmi.RmiPlayerSession;
import it.polimi.it.network.rmi.RmiServer;
import it.polimi.it.network.server.RmiServerImpl;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Client-side RMI Proxy.
 *
 * <p>Implements {@link ServerProxy} (transport-agnostic) and {@link RmiClient}
 * (server → client callback).
 *
 * <p>After registration the server returns a per-player
 * {@link RmiPlayerSession}; game actions are invoked as explicit remote methods
 * on that session instead of being tunneled through a generic JSON method.
 */

public class RmiServerProxy extends UnicastRemoteObject implements ServerProxy, RmiClient {

    private static final Logger LOG = Logger.getLogger(RmiServerProxy.class.getName());

    /** interval heartbeat client → server (ms). */
    private static final long HEARTBEAT_INTERVAL_MS = 5_000;

    private final String host;
    private final int    rmiPort;

    private RmiServer         serverStub;
    private RmiPlayerSession  playerSession;
    private Consumer<Message> onMessage;
    private Runnable          onDisconnect;
    private volatile boolean  connected = false;

    public RmiServerProxy(String host, int rmiPort) throws RemoteException {
        super();
        this.host    = host;
        this.rmiPort = rmiPort;
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
        try {
            Registry registry = LocateRegistry.getRegistry(host, rmiPort);
            serverStub = (RmiServer) registry.lookup(RmiServerImpl.REGISTRY_NAME);
            playerSession = serverStub.registerWithSession(nickname, this, numPlayers,
                    totemColour.name(), sessionId);
            if (playerSession == null) {
                connected = false;
                throw new IOException("Join RMI rifiutato dal server.");
            }
            connected = true;

            Thread t = new Thread(this::heartbeatLoop, "rmi-heartbeat");
            t.setDaemon(true);
            t.start();

            LOG.info("Connesso via RMI a " + host + ":" + rmiPort + " come '" + nickname + "'"
                    + (sessionId != null ? " sessione=" + sessionId : ""));
        } catch (Exception e) {
            throw new IOException("Connessione RMI fallita: " + e.getMessage(), e);
        }
    }

    @Override
    public Message fetchSessionList(int numPlayersFilter) throws IOException {
        try {
            Registry registry = LocateRegistry.getRegistry(host, rmiPort);
            RmiServer stub = (RmiServer) registry.lookup(RmiServerImpl.REGISTRY_NAME);
            return Message.sessionList(stub.listSessions(numPlayersFilter));
        } catch (Exception e) {
            throw new IOException("Impossibile recuperare sessioni RMI: " + e.getMessage(), e);
        }
    }

    @Override
    public void send(Message message) {
        if (!connected || playerSession == null) return;
        try {
            switch (message.getType()) {
                case Message.PLACE_TOTEM -> playerSession.placeTotem(message.ch("tileLetter"));
                case Message.TAKE_CARD ->
                        playerSession.takeCard(message.num("cardId"), message.bool("fromTopRow"));
                case Message.PASS -> playerSession.pass();
                case Message.POST_ROUND -> playerSession.postRound(message.num("cardId"));
                case Message.HEARTBEAT, Message.PING, Message.PONG -> playerSession.heartbeat();
                default -> LOG.warning("Messaggio non inviabile via RMI session API: "
                        + message.getType());
            }
        } catch (RemoteException e) {
            LOG.warning("Invio RMI fallito: " + e.getMessage());
            connected = false;
            if (onDisconnect != null) onDisconnect.run();
        }
    }

    @Override
    public void disconnect() {
        connected = false;
        try { if (playerSession != null) playerSession.disconnect(); } catch (Exception ignored) {}
        try { UnicastRemoteObject.unexportObject(this, true); } catch (Exception ignored) {}
    }

    // Callback RMI: server → client

    @Override
    public void receiveMessage(String json) throws RemoteException {
        if (onMessage != null) onMessage.accept(Message.decode(json));
    }

    //  Heartbeat periodico

    /**
     * Sends a HEARTBEAT every {@value #HEARTBEAT_INTERVAL_MS} ms via RMI.
     * The server-side player session updates its last-seen timestamp from this
     * invocation and can therefore detect silent RMI clients with a watchdog.
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
