package it.polimi.it.network.client;

import it.polimi.it.enums.TotemColour;
import it.polimi.it.network.message.Message;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Client-side abstraction over the server connection.
 *
 * Implemented by {@link SocketServerProxy} (TCP) and {@link RmiServerProxy} (RMI).
 * ClientController and ClientApp use only this interface, so the rest of the
 * client code is completely transport-agnostic.
 */
public interface ServerProxy {

    /**
     * Registers a listener that receives all inbound messages from the server.
     * Must be set before calling {@link #connect}.
     *
     * @param listener callback invoked on the reader thread for each message
     */
    void setOnMessage(Consumer<Message> listener);

    /**
     * Registers a listener invoked when the connection to the server is lost.
     *
     * @param listener callback invoked on connection drop
     */
    void setOnDisconnect(Runnable listener);

    /**
     * Establishes the connection and sends the initial JOIN message.
     *
     * @param nickname    the player's chosen nickname
     * @param numPlayers  desired number of players (used only when creating a session)
     * @throws IOException if the connection cannot be established
     */
    void connect(String nickname, int numPlayers, TotemColour totemColour) throws IOException;

    /**
     * Sends a message to the server.
     * No-op if not connected.
     *
     * @param message the message to send
     */
    void send(Message message);

    /** Closes the connection. Safe to call multiple times. */
    void disconnect();

    /** Returns true if the connection is currently active. */
    boolean isConnected();
}