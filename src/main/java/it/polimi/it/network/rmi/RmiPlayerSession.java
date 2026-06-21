package it.polimi.it.network.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Per-player remote session returned by the RMI lobby after registration.
 *
 * <p>The client invokes game operations directly on this stub. The server-side
 * implementation owns the nickname associated with the session, so action
 * identity is not taken from client-provided payloads.
 */
public interface RmiPlayerSession extends Remote {

    void placeTotem(char tileLetter) throws RemoteException;

    void takeCard(int cardId, boolean fromTopRow) throws RemoteException;

    void pass() throws RemoteException;

    void postRound(int cardId) throws RemoteException;

    void heartbeat() throws RemoteException;

    void disconnect() throws RemoteException;
}
