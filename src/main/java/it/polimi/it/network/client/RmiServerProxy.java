package it.polimi.it.network.client;

import it.polimi.it.network.message.Message;
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
 * Client-side RMI proxy.
 *
 * Implements ServerProxy (transport-agnostic) and RmiClient (callback).
 * Uses String JSON instead of serialised Message objects.
 */
public class RmiServerProxy extends UnicastRemoteObject implements ServerProxy, RmiClient {

    private static final Logger LOG = Logger.getLogger(RmiServerProxy.class.getName());

    private final String host;
    private final int    rmiPort;

    private RmiServer         serverStub;
    private Consumer<Message> onMessage;
    private Runnable          onDisconnect;
    private volatile boolean  connected = false;

    public RmiServerProxy(String host, int rmiPort) throws RemoteException {
        super();
        this.host    = host;
        this.rmiPort = rmiPort;
    }


    @Override public void setOnMessage(Consumer<Message> l)  { this.onMessage    = l; }
    @Override public void setOnDisconnect(Runnable l)        { this.onDisconnect  = l; }
    @Override public boolean isConnected()                   { return connected; }

    @Override
    public void connect(String nickname, int numPlayers) throws IOException {
        try {
            Registry registry = LocateRegistry.getRegistry(host, rmiPort);
            serverStub = (RmiServer) registry.lookup(RmiServerImpl.REGISTRY_NAME);
            serverStub.register(nickname, this, numPlayers);
            connected = true;
            LOG.info("Connected via RMI to " + host + ":" + rmiPort + " as '" + nickname + "'");
        } catch (Exception e) {
            throw new IOException("RMI connection failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void send(Message message) {
        if (!connected || serverStub == null) return;
        try {
            serverStub.sendMessage(message.encode());
        } catch (RemoteException e) {
            LOG.warning("RMI send failed: " + e.getMessage());
            connected = false;
            if (onDisconnect != null) onDisconnect.run();
        }
    }

    @Override
    public void disconnect() {
        connected = false;
        try { UnicastRemoteObject.unexportObject(this, true); } catch (Exception ignored) {}
    }

    // ── RmiClient callback ────────────────────────────────────────────────────

    @Override
    public void receiveMessage(String json) throws RemoteException {
        if (onMessage != null) onMessage.accept(Message.decode(json));
    }
}