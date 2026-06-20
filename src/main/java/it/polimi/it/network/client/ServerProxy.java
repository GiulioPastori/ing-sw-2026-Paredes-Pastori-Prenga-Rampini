package it.polimi.it.network.client;

import it.polimi.it.enums.TotemColour;
import it.polimi.it.network.message.Message;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Client-side abstraction over the server connection.
 * Implemented by {@link SocketServerProxy} (TCP) and {@link RmiServerProxy} (RMI).
 */
public interface ServerProxy {

    void setOnMessage(Consumer<Message> listener);
    void setOnDisconnect(Runnable listener);

    /**
     * Gets the list of open sessions WITHOUT joining.
     * Safe to call before
     * @param numPlayersFilter
     * @return the decoded SESSION_LIST message
     * @throws IOException if the server cannot be reached
     */
    Message fetchSessionList(int numPlayersFilter) throws IOException;

    /** Connects and sends JOIN, getting auto-assigned to a session */
    void connect(String nickname, int numPlayers, TotemColour totemColour) throws IOException;

    /** Connects and sends JOIN to a specific session.
     * @param sessionId id of the chosen session; null/blank = auto-assign */
    void connectWithSession(String nickname, int numPlayers,
                            TotemColour totemColour, String sessionId) throws IOException;

    void send(Message message);
    void disconnect();
    boolean isConnected();
}
