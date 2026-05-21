package it.polimi.it.network.client;

import it.polimi.it.enums.TotemColour;
import it.polimi.it.network.message.Message;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Client-side abstraction over the server connection.
 *
 * Implemented by {@link SocketServerProxy} (TCP) and {@link RmiServerProxy} (RMI).
 *
 * <h2>Flusso partite multiple</h2>
 * <ol>
 *   <li>Chiama {@link #fetchSessionList} per sapere le sessioni aperte.</li>
 *   <li>Mostra la lista all'utente e fai scegliere.</li>
 *   <li>Chiama {@link #connectWithSession} passando il sessionId scelto
 *       (oppure {@link #connect} per l'auto-assign).</li>
 * </ol>
 */
public interface ServerProxy {

    void setOnMessage(Consumer<Message> listener);
    void setOnDisconnect(Runnable listener);

    /**
     * Recupera la lista delle sessioni aperte SENZA unirsi.
     * Sicuro da chiamare prima di {@link #connect}.
     *
     * @param numPlayersFilter se &gt; 0 filtra per giocatori attesi; 0 = tutte
     * @return messaggio SESSION_LIST decodificato
     * @throws IOException se il server non è raggiungibile
     */
    Message fetchSessionList(int numPlayersFilter) throws IOException;

    /**
     * Si connette e invia JOIN assegnandosi automaticamente a una sessione.
     */
    void connect(String nickname, int numPlayers, TotemColour totemColour) throws IOException;

    /**
     * Si connette e invia JOIN verso una sessione specifica.
     *
     * @param sessionId id della sessione scelta; null/blank = auto-assign (come {@link #connect})
     */
    void connectWithSession(String nickname, int numPlayers,
                            TotemColour totemColour, String sessionId) throws IOException;

    void send(Message message);
    void disconnect();
    boolean isConnected();
}
