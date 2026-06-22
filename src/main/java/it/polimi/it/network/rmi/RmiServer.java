package it.polimi.it.network.rmi;

import it.polimi.it.network.message.Message;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * RMI interface exposed by the server.
 */
public interface RmiServer extends Remote {

    /**
     * Registers a client by assigning it automatically to an open session with
     * the requested player count, or by creating a new session.
     *
     * @return the remote player-session stub, or {@code null} if the join was
     *         rejected; in that case the server also sends an error message to
     *         the client callback
     */
    RmiPlayerSession register(String nickname, RmiClient client, int numPlayers, String totemColour)
            throws RemoteException;

    /**
     * Like {@link #register}, but joins the specific session identified by
     * {@code sessionId}. If {@code sessionId} is null or blank, this behaves
     * like {@link #register}.
     *
     * @return the remote player-session stub, or {@code null} if the join was
     *         rejected
     */
    RmiPlayerSession registerWithSession(String nickname, RmiClient client,
                                         int numPlayers, String totemColour, String sessionId)
            throws RemoteException;

    /**
     * Returns the list of open sessions.
     * Call this before {@link #registerWithSession} to show available games.
     *
     * @param numPlayersFilter if greater than 0, filters by player count; 0
     *                         returns all sessions
     */
    List<Message.SessionInfo> listSessions(int numPlayersFilter) throws RemoteException;
}
