package it.polimi.it.network.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/** RMI interface exposed by the server.
 * Uses String (JSON) to avoid requiring Message to implement Serializable.
 */
public interface RmiServer extends Remote {

    /** Registers a client and auto-assigns it to an open session
     * with {@code numPlayers} expected players (or creates a new one).
     */
    void register(String nickname, RmiClient client, int numPlayers, String totemColour)
            throws RemoteException;

    /** Like {@link #register} but lets the client join a specific session
     * identified by {@code sessionId} (multiple-sessions feature).
     * If {@code sessionId} is null or blank it behaves like {@link #register}.
     */
    void registerWithSession(String nickname, RmiClient client,
                             int numPlayers, String totemColour, String sessionId)
            throws RemoteException;

    /** Returns the list of open sessions as JSON (SESSION_LIST).
     * Call it before {@link #registerWithSession} to show the available games.
     * @param numPlayersFilter if &gt; 0, filter by number of players; 0 = all
     */
    String listSessions(int numPlayersFilter) throws RemoteException;

    /** Sends an action message from the client to the server (as a JSON string).
     */
    void sendMessage(String json) throws RemoteException;
}
