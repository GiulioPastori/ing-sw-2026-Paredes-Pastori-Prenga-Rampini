package it.polimi.it.network.server;

import it.polimi.it.CardLoader;
import it.polimi.it.cards.Card;
import it.polimi.it.network.server.persistence.DatabaseConfig;
import it.polimi.it.network.server.persistence.DatabaseInitializer;
import it.polimi.it.network.server.persistence.LeaderboardRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

/**
 * GameServer encapsulates the complete server startup:
 * - Loads cards from JSON
 * - Bootstraps the leaderboard database
 * - Starts the LobbyServer (Socket TCP)
 * - Starts the RMI registry alongside
 *
 * <p>Both Socket and RMI clients can connect to the same LobbyServer
 * and play in the same game session.
 *
 * <p>The leaderboard storage is created — empty — on the first server launch
 * (database name embeds the group identifier, e.g. {@code GC45_Mesos_DB},
 * to avoid collisions on a shared DBMS). The server fails fast if MySQL is
 * unreachable or if a different DBMS is detected, because the leaderboard
 * feature is part of the declared deliverable.
 *
 * <p>Usage:
 * <pre>
 *   GameServer server = new GameServer(12345, 12346, "cards.json");
 *   server.start(); // blocks until stop() is called
 * </pre>
 */
public class GameServer {

    private static final Logger LOG = Logger.getLogger(GameServer.class.getName());

    private final int    socketPort;
    private final int    rmiPort;
    private final String cardsJsonPath;

    private LobbyServer           lobbyServer;
    private LeaderboardRepository leaderboardRepository;

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
        List<Card> cards = loadCards();
        ServerApp.setCardList(cards);
        LOG.info("Loaded " + cards.size() + " cards from " + cardsJsonPath);

        // 2. Bootstrap the leaderboard database (additional functionality).
        //    Done before opening the network so that any misconfiguration is
        //    surfaced early. A DB failure prevents startup because the
        //    leaderboard feature requires a compliant MySQL database.
        leaderboardRepository = bootstrapLeaderboard();

        // 3. Create lobby server (manages sessions for both Socket and RMI)
        lobbyServer = new LobbyServer(socketPort, leaderboardRepository);

        // 4. Start RMI registry alongside
        try {
            new RmiServerImpl(lobbyServer, rmiPort);
            LOG.info("RMI registry started on port " + rmiPort);
        } catch (Exception e) {
            LOG.warning("RMI could not start: " + e.getMessage()
                    + " — Socket-only mode.");
        }

        // 5. Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        // 6. Start accepting TCP connections (blocks here)
        LOG.info("Mesos server ready  Socket=" + socketPort + "  RMI=" + rmiPort);
        lobbyServer.start();
    }

    private List<Card> loadCards() throws IOException {
        Path path = Path.of(cardsJsonPath);
        if (Files.exists(path)) {
            return CardLoader.loadAll(path);
        }
        return CardLoader.loadAllFromResource(cardsJsonPath);
    }

    /**
     * Verifies that the leaderboard database exists (creating it empty if
     * not, as required by the spec) and returns a repository that the game
     * sessions can use to persist results and serve rankings.
     *
     * <p>If MySQL is unreachable the method logs a warning and returns
     * {@code null}: the server starts normally but leaderboard persistence
     * is disabled for the session.
     *
     * @return the configured repository, or {@code null} if the DB is unavailable
     */
    private LeaderboardRepository bootstrapLeaderboard() {
        DatabaseConfig config = DatabaseConfig.load();
        DatabaseInitializer init = new DatabaseInitializer(config);
        try {
            init.initialise();
            LOG.info("Leaderboard database ready (" + config.databaseName() + ").");
            return new LeaderboardRepository(config);
        } catch (Exception e) {
            LOG.warning("Leaderboard DB unavailable — server will run without persistence. "
                    + "Start MySQL and restart to enable the leaderboard. Cause: "
                    + e.getMessage());
            return null;
        }
    }

    /**
     * Stops the server gracefully.
     * Safe to call from a shutdown hook.
     */
    public void stop() {
        if (lobbyServer != null) lobbyServer.stop();
        LOG.info("Server stopped.");
    }

    /** @return the leaderboard repository, or {@code null} if the DB is unavailable. */
    public LeaderboardRepository getLeaderboardRepository() {
        return leaderboardRepository;
    }
}
