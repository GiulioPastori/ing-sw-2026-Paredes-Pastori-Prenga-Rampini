package it.polimi.it.network.server;

import it.polimi.it.CardLoader;
import it.polimi.it.cards.Card;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

/**
 * Server entry point.
 *
 * Starts both the Socket server and the RMI registry so that clients
 * using either technology can connect to the same game session.
 *
 * Usage:
 *   java -jar mesos-server.jar [socketPort] [rmiPort] [cardsJsonPath]
 *
 * Defaults:
 *   socketPort   = 12345
 *   rmiPort      = 12346
 *   cardsJsonPath = src/main/resources/cards.json
 */
public class ServerApp {

    private static final Logger LOG              = Logger.getLogger(ServerApp.class.getName());
    private static final int    DEFAULT_SOCKET   = 12345;
    private static final int    DEFAULT_RMI      = 12346;
    private static final String DEFAULT_CARDS    = "src/main/resources/cards.json";

    /** Card list loaded once at startup; shared by all GameSessions. */
    private static List<Card> cardList;

    public static List<Card> getCardList() { return cardList; }

    public static void main(String[] args) {
        int    socketPort = DEFAULT_SOCKET;
        int    rmiPort    = DEFAULT_RMI;
        String cardsPath  = DEFAULT_CARDS;

        if (args.length >= 1) socketPort = parseIntOrDefault(args[0], DEFAULT_SOCKET);
        if (args.length >= 2) rmiPort    = parseIntOrDefault(args[1], DEFAULT_RMI);
        if (args.length >= 3) cardsPath  = args[2];

        // Load cards from JSON
        try {
            cardList = CardLoader.loadAll(Path.of(cardsPath));
            LOG.info("Loaded " + cardList.size() + " cards from " + cardsPath);
        } catch (IOException e) {
            LOG.severe("Cannot load cards: " + e.getMessage());
            System.exit(1);
        }

        LobbyServer lobbyServer = new LobbyServer(socketPort);

        // Start RMI server alongside Socket server
        try {
            new RmiServerImpl(lobbyServer, rmiPort);
        } catch (Exception e) {
            LOG.warning("RMI server could not start: " + e.getMessage());
        }

        // Graceful shutdown on Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(lobbyServer::stop));

        LOG.info("Mesos server started  Socket=" + socketPort + "  RMI=" + rmiPort);
        try {
            lobbyServer.start();
        } catch (IOException e) {
            LOG.severe("Server failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private static int parseIntOrDefault(String s, int def) {
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return def; }
    }
}