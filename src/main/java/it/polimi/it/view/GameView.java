package it.polimi.it.view;

import it.polimi.it.enums.Era;
import it.polimi.it.network.server.persistence.LeaderboardEntry;
import it.polimi.it.observer.BoardSnapshot;
import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.FinalScoreBreakdown;
import it.polimi.it.observer.PlayerSnapshot;
import it.polimi.it.observer.PlayerResourceDelta;

import java.util.List;
import java.util.Map;

/**
 * View interface (View role in MVC).
 *
 * Implemented by the TUI, the GUI (JavaFX), and stub implementations
 * used in tests. The view knows nothing about Game, Board, Player, or Card:
 * it receives and displays only snapshot objects (DTO).
 *
 * In the client-server setup, a concrete implementation of this interface
 * lives on each client. The network layer delivers GameEvents serialised
 * from the server; the client's controller unpacks them and calls the
 * appropriate show/request method here.
 *
 * Methods fall into two groups:
 * - showXxx(): display a state update (push from model to view)
 * - requestXxx(): ask the user to make a choice (only relevant for
 *   local/single-player play; in the networked case the client sends
 *   a PlayerAction message back to the server independently)
 *
 * <p>Player tribes (acquired characters and buildings) live inside
 * {@link PlayerSnapshot}, so any view method that receives a player
 * snapshot also receives that player's up-to-date tribe.
 */
public interface GameView {

    // ── Display methods ───────────────────────────────────────────────────────

    void showRoundStarted(int round, List<PlayerSnapshot> turnOrder,
                          BoardSnapshot board);

    void showTotemPlaced(PlayerSnapshot player, char tileLetter,
                         BoardSnapshot board);

    /**
     * Shows the cards available in a row for the active player.
     *
     * @param visibleEvents event cards in the same row; they are visible but
     *                      cannot be acquired
     */
    void showCardSlotReady(PlayerSnapshot player, boolean isTopRow,
                           List<CardSnapshot> availableCards,
                           List<CardSnapshot> visibleEvents);

    void showCardAcquired(PlayerSnapshot player, CardSnapshot card);

    void showCardAcquisitionFailed(PlayerSnapshot player, CardSnapshot card, int missingFood);

    void showTotemReturnedToTurnOrder(PlayerSnapshot player, int position,
                                      BoardSnapshot board);

    void showPostRoundStarted(List<PlayerSnapshot> watchtowerPlayers,
                              List<CardSnapshot> availableCards);

    void showEventResolved(int eventId, Era era, it.polimi.it.enums.EventType type);

    default void showEventResolved(int eventId, Era era,
                                   it.polimi.it.enums.EventType type,
                                   List<PlayerResourceDelta> deltas) {
        showEventResolved(eventId, era, type);
    }

    void showEraChanged(Era newEra);

    void showRoundEnded(int round);

    void showInvalidAction(String playerName, String reason);

    /** Called when another player disconnects — game continues. */
    default void showPlayerDisconnected(String nickname) {
        showInvalidAction("DISCONNECT", nickname + " si è disconnesso. Il gioco continua.");
    }

    /** Called when a disconnected player reconnects. */
    default void showPlayerReconnected(String nickname) {
        showInvalidAction("RECONNECT", nickname + " si è riconnesso.");
    }

    /** Called with lobby player list while waiting for the game to start. */
    default void showLobbyUpdate(java.util.List<String> players, int needed) {}

    void showGameEnded(List<PlayerSnapshot> winners, List<PlayerSnapshot> finalScores);

    default void showGameEnded(List<PlayerSnapshot> winners,
                               List<PlayerSnapshot> finalScores,
                               List<FinalScoreBreakdown> scoreBreakdowns) {
        showGameEnded(winners, finalScores);
    }

    /**
     * Displays the persistent leaderboard delivered by the server after a
     * game ends (DB leaderboard feature).
     *
     * <p>The same method is invoked on every client: each instance receives
     * its own {@code recipient}, score and rank, plus the same global
     * leaderboard restricted to games with the same number of players.
     *
     * @param recipient    nickname this leaderboard message is addressed to
     * @param myScore      final score the recipient obtained in this game
     * @param myRank       1-based position of the recipient among all the
     *                     games with the given size; -1 if unknown
     * @param totalEntries total number of recorded scores for that size
     * @param numPlayers   number of players the game had
     * @param leaderboard  ranked entries for games with that many players
     */
    default void showLeaderboard(String recipient,
                                 int myScore,
                                 int myRank,
                                 int totalEntries,
                                 int numPlayers,
                                 List<LeaderboardEntry> leaderboard) {
        // Default: no-op so implementations that don't care about the
        // leaderboard (e.g. legacy stubs) keep compiling unchanged.
    }

    default void showLeaderboard(String recipient,
                                 int myScore,
                                 int myRank,
                                 int totalEntries,
                                 int numPlayers,
                                 List<LeaderboardEntry> leaderboard,
                                 Map<String, Integer> playerRanks) {
        showLeaderboard(recipient, myScore, myRank, totalEntries, numPlayers, leaderboard);
    }


    // ── Input methods (local / single-player use) ─────────────────────────────

    /**
     * Asks the player which free offer tile to place their totem on.
     * The full {@link BoardSnapshot} is passed so the view can display the
     * complete state (including tiles occupied by other players' totems);
     * the choice itself must still resolve to a free tile.
     *
     * @return the letter of the chosen tile.
     */
    char requestOfferTileChoice(PlayerSnapshot player, BoardSnapshot board);

    /**
     * Asks the player which card to take.
     * Returns the card ID, or -1 to pass (only valid when {@code canPass} is true).
     *
     * @param canPass true when the UI must offer the player the option to
     *                skip the current slot instead of acquiring a card.
     */
    int requestCardChoice(PlayerSnapshot player, List<CardSnapshot> available, boolean canPass);

    /**
     * Asks the Watchtower-owning player which card to take from the top row.
     * Returns the card ID, or -1 to decline.
     */
    int requestPostRoundCardChoice(PlayerSnapshot player, List<CardSnapshot> available);

}
