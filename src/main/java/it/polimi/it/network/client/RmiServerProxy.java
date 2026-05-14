package it.polimi.it.network.client;

import it.polimi.it.network.message.Message;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.network.rmi.RmiClient;
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
 * <p>Sends a heartbeat {@link Message#HEARTBEAT} every
 * {@value #HEARTBEAT_INTERVAL_MS} ms via {@link RmiServer#sendMessage(String)}
 * so that the server can detect RMI disconnections even in the absence of actions.
 */

public class RmiServerProxy extends UnicastRemoteObject implements ServerProxy, RmiClient {

    private static final Logger LOG = Logger.getLogger(RmiServerProxy.class.getName());

    /** interval heartbeat client → server (ms). */
    private static final long HEARTBEAT_INTERVAL_MS = 5_000;

    private final String host;
    private final int    rmiPort;

    private RmiServer         serverStub;
    private Consumer<Message> onMessage;
    private Runnable          onDisconnect;
    private volatile boolean  connected = false;
    /** Nickname used during reconnection — It is used to construct the heartbeat message. */
    private String myNickname;

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
        try {
            this.myNickname = nickname;
            Registry registry = LocateRegistry.getRegistry(host, rmiPort);
            serverStub = (RmiServer) registry.lookup(RmiServerImpl.REGISTRY_NAME);
            serverStub.register(nickname, this, numPlayers, totemColour.name());
            connected = true;

            Thread t = new Thread(this::heartbeatLoop, "rmi-heartbeat");
            t.setDaemon(true);
            t.start();

            LOG.info("Connesso via RMI a " + host + ":" + rmiPort + " come '" + nickname + "'");
        } catch (Exception e) {
            throw new IOException("Connessione RMI fallita: " + e.getMessage(), e);
        }
    }

    @Override
    public void send(Message message) {
        if (!connected || serverStub == null) return;
        try {
            serverStub.sendMessage(message.encode());
        } catch (RemoteException e) {
            LOG.warning("Invio RMI fallito: " + e.getMessage());
            connected = false;
            if (onDisconnect != null) onDisconnect.run();
        }
    }

    @Override
    public void disconnect() {
        connected = false;
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
     * The RMI server uses {@code RmiClientHandler.send()} to detect disconnections;
     * the failure of the call itself is enough to trigger
     * {@link it.polimi.it.network.server.GameSession#onPlayerDisconnected(String)}.
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