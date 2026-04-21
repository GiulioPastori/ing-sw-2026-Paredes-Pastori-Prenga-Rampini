package it.polimi.it.network.server;

import it.polimi.it.network.message.Message;
import it.polimi.it.network.rmi.RmiClient;
import it.polimi.it.network.rmi.RmiServer;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Server-side RMI implementation.
 *
 * Exported to the RMI registry under "MesosServer".
 * Shares LobbyServer session management with the Socket server,
 * allowing mixed Socket + RMI games.
 *
 * Each RMI client is wrapped in an RmiClientHandler that implements
 * ClientConnection, making it transparent to GameSession.
 */
public class RmiServerImpl extends UnicastRemoteObject implements RmiServer {

    private static final Logger LOG           = Logger.getLogger(RmiServerImpl.class.getName());
    public  static final String REGISTRY_NAME = "MesosServer";

    private final LobbyServer                   lobby;
    private final Map<String, RmiClientHandler> handlers       = new ConcurrentHashMap<>();
    private final Set<String>                   takenNicknames = ConcurrentHashMap.newKeySet();

    public RmiServerImpl(LobbyServer lobby, int rmiPort) throws RemoteException {
        super();
        this.lobby = lobby;
        Registry registry = LocateRegistry.createRegistry(rmiPort);
        registry.rebind(REGISTRY_NAME, this);
        LOG.info("RMI registry started on port " + rmiPort);
    }

    // ── RmiServer ─────────────────────────────────────────────────────────────

    @Override
    public void register(String nickname, RmiClient client, int numPlayers)
            throws RemoteException {

        if (nickname == null || nickname.isBlank()) {
            client.receiveMessage(Message.error("Nickname must not be blank.").encode());
            return;
        }
        if (takenNicknames.contains(nickname)) {
            client.receiveMessage(
                    Message.error("Nickname '" + nickname + "' is already taken.").encode());
            return;
        }
        takenNicknames.add(nickname);

        // Check reconnection
        GameSession reconnectTarget = lobby.findSessionForReconnection(nickname);

        RmiClientHandler handler = new RmiClientHandler(nickname, client) {
            @Override public void close() {
                super.close();
                handlers.remove(nickname);
                if (getSession() != null && getSession().isTerminated())
                    takenNicknames.remove(nickname);
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

        LOG.info("RMI player '" + nickname + "' registered.");
    }

    @Override
    public void sendMessage(String json) throws RemoteException {
        Message msg  = Message.decode(json);
        String  name = extractPlayerName(msg);
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
 * Wraps an RMI client callback as a ClientConnection.
 * Allows GameSession to treat Socket and RMI clients uniformly.
 */
class RmiClientHandler implements ClientConnection {

    private static final Logger LOG = Logger.getLogger(RmiClientHandler.class.getName());

    private final String    nickname;
    private final RmiClient stub;
    private GameSession     session;
    private volatile boolean closed = false;

    RmiClientHandler(String nickname, RmiClient stub) {
        this.nickname = nickname;
        this.stub     = stub;
    }

    void        setSession(GameSession s) { this.session = s; }
    GameSession getSession()              { return session; }

    @Override public String getNickname() { return nickname; }

    @Override
    public void send(Message message) {
        if (closed) return;
        try {
            stub.receiveMessage(message.encode());
        } catch (Exception e) {
            LOG.warning("RMI send failed for '" + nickname + "': " + e.getMessage());
            close();
            if (session != null) session.onPlayerDisconnected(nickname);
        }
    }

    @Override public void close() { closed = true; }
}