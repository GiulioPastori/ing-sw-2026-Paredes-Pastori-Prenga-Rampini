package it.polimi.it.network.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMI callback interface exposed by each client.
 *
 * Uses String (JSON) instead of serialised objects to avoid
 * requiring Message to implement Serializable.
 */
public interface RmiClient extends Remote {

    /**
     * Called by the server to deliver a game event message (as JSON string).
     *
     * @param json the encoded Message
     * @throws RemoteException on RMI communication failure
     */
    void receiveMessage(String json) throws RemoteException;
}