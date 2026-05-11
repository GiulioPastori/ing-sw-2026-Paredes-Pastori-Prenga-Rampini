package it.polimi.it.network.rmi;

import it.polimi.it.enums.TotemColour;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMI interface exposed by the server.
 *
 * Uses String (JSON) instead of serialised objects to avoid
 * requiring Message to implement Serializable.
 */
public interface RmiServer extends Remote {

    /**
     * Registers a client and its callback stub.
     *
     * @param nickname    the player's chosen nickname
     * @param client      the client callback stub
     * @param numPlayers  desired number of players
     * @throws RemoteException on RMI communication failure
     */
    void register(String nickname, RmiClient client, int numPlayers, String totemColour) throws RemoteException;

    /**
     * Sends an action message from a client to the server (as JSON string).
     *
     * @param json the encoded Message
     * @throws RemoteException on RMI communication failure
     */
    void sendMessage(String json) throws RemoteException;
}