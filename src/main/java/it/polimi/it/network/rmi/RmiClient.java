package it.polimi.it.network.rmi;

import it.polimi.it.network.message.Message;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMI callback interface exposed by each client.
 * The server calls receiveMessage() to push event messages.
 */
public interface RmiClient extends Remote {

    /**
     * Delivers a game event message to this client.
     *
     * @param message the event message
     * @throws RemoteException on RMI communication failure
     */
    void receiveMessage(Message message) throws RemoteException;
}