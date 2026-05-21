package it.polimi.it.network.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMI interface exposed by the server.
 *
 * Uses String (JSON) to avoid requiring Message to implement Serializable.
 */
public interface RmiServer extends Remote {

    /**
     * Registra un client assegnandolo automaticamente a una sessione aperta
     * con {@code numPlayers} giocatori attesi (o ne crea una nuova).
     */
    void register(String nickname, RmiClient client, int numPlayers, String totemColour)
            throws RemoteException;

    /**
     * Come {@link #register} ma consente di unirsi a una sessione specifica
     * identificata da {@code sessionId} (feature partite multiple).
     * Se {@code sessionId} è null o blank si comporta come {@link #register}.
     */
    void registerWithSession(String nickname, RmiClient client,
                             int numPlayers, String totemColour, String sessionId)
            throws RemoteException;

    /**
     * Restituisce la lista delle sessioni aperte come JSON (SESSION_LIST).
     * Da chiamare prima di {@link #registerWithSession} per mostrare le partite disponibili.
     *
     * @param numPlayersFilter se &gt; 0 filtra per numero di giocatori; 0 = tutte
     */
    String listSessions(int numPlayersFilter) throws RemoteException;

    /**
     * Invia un messaggio di azione dal client al server (come stringa JSON).
     */
    void sendMessage(String json) throws RemoteException;
}
