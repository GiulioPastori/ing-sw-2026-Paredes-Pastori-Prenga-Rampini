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
     * Registra un client assegnandolo automaticamente a una sessione aperta
     * con {@code numPlayers} giocatori attesi (o ne crea una nuova).
     *
     * @return lo stub remoto della sessione giocatore, o {@code null} se il
     *         join è stato rifiutato; in quel caso il server invia anche un
     *         messaggio di errore al callback client
     */
    RmiPlayerSession register(String nickname, RmiClient client, int numPlayers, String totemColour)
            throws RemoteException;

    /**
     * Come {@link #register} ma consente di unirsi a una sessione specifica
     * identificata da {@code sessionId} (feature partite multiple).
     * Se {@code sessionId} è null o blank si comporta come {@link #register}.
     *
     * @return lo stub remoto della sessione giocatore, o {@code null} se il
     *         join è stato rifiutato
     */
    RmiPlayerSession registerWithSession(String nickname, RmiClient client,
                                         int numPlayers, String totemColour, String sessionId)
            throws RemoteException;

    /**
     * Restituisce la lista delle sessioni aperte.
     * Da chiamare prima di {@link #registerWithSession} per mostrare le partite disponibili.
     *
     * @param numPlayersFilter se &gt; 0 filtra per numero di giocatori; 0 = tutte
     */
    List<Message.SessionInfo> listSessions(int numPlayersFilter) throws RemoteException;
}
