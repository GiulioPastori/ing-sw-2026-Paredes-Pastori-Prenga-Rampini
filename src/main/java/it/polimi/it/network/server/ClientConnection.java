package it.polimi.it.network.server;

import it.polimi.it.network.message.Message;

/**
 * Abstraction over a connected client, regardless of transport (Socket or RMI).
 *
 * GameSession uses only this interface, making it transport-agnostic.
 * Implementations: {@link SocketClientHandler} (TCP), {@link RmiClientHandler} (RMI).
 */
public interface ClientConnection {

    /** Returns the nickname of the connected player. */
    String getNickname();

    /**
     * Sends a message to this client.
     * Must be non-blocking and thread-safe.
     */
    void send(Message message);

    /** Closes the connection. Safe to call multiple times. */
    void close();
}