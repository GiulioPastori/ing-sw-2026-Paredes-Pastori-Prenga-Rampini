package it.polimi.it.network.server;

import it.polimi.it.CardLoader;
import it.polimi.it.cards.Card;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

/**
 * GameServer encapsulates the complete server startup:
 * - Loads cards from JSON
 * - Starts the LobbyServer (Socket TCP)
 * - Starts the RMI registry alongside
 *
 * Both Socket and RMI clients can connect to the same LobbyServer
 * and play in the same game session.
 *
 * Usage:
 *   GameServer server = new GameServer(12345, 12346, "src/main/resources/cards.json");
 *   server.start(); // blocks until stop() is called
 */
public class GameServer {

    private static final Logger LOG = Logger.getLogger(GameServer.class.getName());

    private final int    socketPort;
    private final int    rmiPort;
    private final String cardsJsonPath;

    private LobbyServer lobbyServer;

    public GameServer(int socketPort, int rmiPort, String cardsJsonPath) {
        this.socketPort    = socketPort;
        this.rmiPort       = rmiPort;
        this.cardsJsonPath = cardsJsonPath;
    }

    /**
     * Initialises and starts the server.
     * Blocks until {@link #stop()} is called.
     *
     * @throws IOException if the cards file cannot be read or the socket port
     *                     cannot be opened
     */
    public void start() throws IOException {
        // 1. Load cards
        List<Card> cards = CardLoader.loadAll(Path.of(cardsJsonPath));
        ServerApp.setCardList(cards);
        LOG.info("Loaded " + cards.size() + " cards from " + cardsJsonPath);

        // 2. Create lobby server (manages sessions for both Socket and RMI)
        lobbyServer = new LobbyServer(socketPort);

        // 3. Start RMI registry alongside
        try {
            new RmiServerImpl(lobbyServer, rmiPort);
            LOG.info("RMI registry started on port " + rmiPort);
        } catch (Exception e) {
            LOG.warning("RMI could not start: " + e.getMessage()
                    + " — Socket-only mode.");
        }

        // 4. Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        // 5. Start accepting TCP connections (blocks here)
        LOG.info("Mesos server ready  Socket=" + socketPort + "  RMI=" + rmiPort);
        lobbyServer.start();
    }

    /**
     * Stops the server gracefully.
     * Safe to call from a shutdown hook.
     */
    public void stop() {
        if (lobbyServer != null) lobbyServer.stop();
        LOG.info("Server stopped.");
    }
}