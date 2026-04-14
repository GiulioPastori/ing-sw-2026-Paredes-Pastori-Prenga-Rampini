package it.polimi.it.network.rmi;

import it.polimi.it.network.message.Message;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMI interface exposed by the server.
 * Clients call sendMessage() to send actions.
 */
public interface RmiServer extends Remote {

    /**
     * Sends an action message from a client to the server.
     *
     * @param message the action message (PlaceTotem, TakeCard, Pass, PostRound)
     * @throws RemoteException on RMI communication failure
     */
    void sendMessage(Message message) throws RemoteException;

    /**
     * Registers a client callback so the server can push event messages.
     *
     * @param nickname    the player's chosen nickname
     * @param client      the client callback stub
     * @param numPlayers  desired number of players (used when creating a session)
     * @throws RemoteException on RMI communication failure
     */
    void register(String nickname, RmiClient client, int numPlayers) throws RemoteException;
}