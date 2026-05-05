package it.polimi.it.network.server;

import it.polimi.it.cards.Card;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Server entry point.
 *
 * Delegates all startup logic to {@link GameServer}.
 * This class only parses command-line arguments and holds
 * the shared card list used by all GameSessions.
 *
 * Usage:
 *   java -jar mesos-server.jar [socketPort] [rmiPort] [cardsJsonPath]
 *
 * Defaults:
 *   socketPort    = 12345
 *   rmiPort       = 12346
 *   cardsJsonPath = src/main/resources/cards.json
 */
public class ServerApp {

    private static final Logger LOG           = Logger.getLogger(ServerApp.class.getName());
    private static final int    DEFAULT_SOCKET = 12345;
    private static final int    DEFAULT_RMI    = 12346;
    private static final String DEFAULT_CARDS  = "src/main/resources/cards.json";

    /** Shared card list — loaded once at startup, read by every GameSession. */
    private static List<Card> cardList;

    public static List<Card> getCardList()             { return cardList; }
    public static void       setCardList(List<Card> c) { cardList = c; }

    public static void main(String[] args) {
        int    socketPort = parseIntArg(args, 0, DEFAULT_SOCKET);
        int    rmiPort    = parseIntArg(args, 1, DEFAULT_RMI);
        String cardsPath  = args.length >= 3 ? args[2] : DEFAULT_CARDS;

        GameServer server = new GameServer(socketPort, rmiPort, cardsPath);
        try {
            server.start();
        } catch (IOException e) {
            LOG.severe("Server failed to start: " + e.getMessage());
            System.exit(1);
        }
    }

    private static int parseIntArg(String[] args, int index, int defaultValue) {
        if (args.length <= index) return defaultValue;
        try { return Integer.parseInt(args[index]); }
        catch (NumberFormatException e) {

            LOG.warning("Invalid argument at index " + index + ", using default " + defaultValue);
            return defaultValue;
        }
    }
}