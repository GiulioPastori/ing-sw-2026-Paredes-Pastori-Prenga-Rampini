package it.polimi.it.network.server;

import it.polimi.it.network.message.*;
        import it.polimi.it.network.rmi.RmiClient;
import it.polimi.it.network.rmi.RmiServer;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Server-side RMI implementation.
 *
 * Exported to the RMI registry under "MesosServer".
 * Shares the LobbyServer session management with the Socket server,
 * allowing mixed Socket + RMI games (different players may use different
 * transports in the same game).
 *
 * Each RMI client is wrapped in an RmiClientHandler that implements
 * ClientConnection, making it transparent to GameSession.
 */
public class RmiServerImpl extends UnicastRemoteObject implements RmiServer {

    private static final Logger LOG           = Logger.getLogger(RmiServerImpl.class.getName());
    public static final         String REGISTRY_NAME = "MesosServer";

    private final LobbyServer                   lobby;
    private final Map<String, RmiClientHandler> handlers       = new ConcurrentHashMap<>();
    private final java.util.Set<String>         takenNicknames = ConcurrentHashMap.newKeySet();

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
            client.receiveMessage(new ErrorMessage("Nickname must not be blank."));
            return;
        }
        if (takenNicknames.contains(nickname)) {
            client.receiveMessage(new ErrorMessage(
                    "Nickname '" + nickname + "' is already taken."));
            return;
        }
        takenNicknames.add(nickname);

        GameSession session = lobby.getOrCreateSession(numPlayers);

        RmiClientHandler handler = new RmiClientHandler(nickname, client) {
            @Override public void close() {
                super.close();
                handlers.remove(nickname);
                takenNicknames.remove(nickname);
            }
        };
        handlers.put(nickname, handler);
        handler.setSession(session);
        session.addClient(handler);

        LOG.info("RMI player '" + nickname + "' registered.");
    }

    @Override
    public void sendMessage(Message message) throws RemoteException {
        String name = extractPlayerName(message);
        if (name == null) return;
        RmiClientHandler handler = handlers.get(name);
        if (handler != null) handler.getSession().handleMessage(message, handler);
    }

    private String extractPlayerName(Message message) {
        return switch (message) {
            case PlaceTotemMessage m -> m.playerName();
            case TakeCardMessage   m -> m.playerName();
            case PassMessage       m -> m.playerName();
            case PostRoundMessage  m -> m.playerName();
            default -> null;
        };
    }
}

// ─────────────────────────────────────────────────────────────────────────────

/**
 * Wraps an RMI client callback as a ClientConnection so that GameSession
 * can treat Socket and RMI clients uniformly.
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
            stub.receiveMessage(message);
        } catch (RemoteException e) {
            LOG.warning("RMI send failed for '" + nickname + "': " + e.getMessage());
            close();
            if (session != null) session.onPlayerDisconnected(nickname);
        }
    }

    @Override public void close() { closed = true; }
}