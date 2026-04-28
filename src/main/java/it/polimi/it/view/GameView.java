package it.polimi.it.view;

import it.polimi.it.enums.Era;
import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.OfferTileSnapshot;
import it.polimi.it.observer.PlayerSnapshot;

import java.util.List;

/**
 * View interface (View role in MVC).
 *
 * Implemented by the TUI, the GUI (Swing/JavaFX), and stub implementations
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
 */
public interface GameView {

    // ── Display methods ───────────────────────────────────────────────────────

    void showRoundStarted(int round, List<PlayerSnapshot> turnOrder,
                          List<OfferTileSnapshot> availableTiles);

    void showTotemPlaced(PlayerSnapshot player, char tileLetter);

    /** Mostra le carte disponibili in una riga per il giocatore attivo.
     *  visibleEvents contiene gli eventi presenti nella stessa riga (non acquisibili). */
    void showCardSlotReady(PlayerSnapshot player, boolean isTopRow,
                           List<CardSnapshot> availableCards,
                           List<CardSnapshot> visibleEvents);

    void showCardAcquired(PlayerSnapshot player, CardSnapshot card);

    void showCardAcquisitionFailed(PlayerSnapshot player, CardSnapshot card, int missingFood);

    void showTotemReturnedToTurnOrder(PlayerSnapshot player, int position);

    void showPostRoundStarted(List<PlayerSnapshot> watchtowerPlayers,
                              List<CardSnapshot> availableCards);

    void showEventResolved(int eventId, Era era, it.polimi.it.enums.EventType type);

    void showEraChanged(Era newEra);

    void showRoundEnded(int round);

    void showInvalidAction(String playerName, String reason);

    void showGameEnded(List<PlayerSnapshot> winners, List<PlayerSnapshot> finalScores);

    // ── Input methods (local / single-player use) ─────────────────────────────

    /**
     * Asks the player which free offer tile to place their totem on.
     * Returns the letter of the chosen tile.
     */
    char requestOfferTileChoice(PlayerSnapshot player, List<OfferTileSnapshot> freeTiles);

    /**
     * Asks the player which card to take.
     * Returns the card ID, or -1 to pass.
     */
    int requestCardChoice(PlayerSnapshot player, List<CardSnapshot> available);

    /**
     * Asks the Watchtower-owning player which card to take from the top row.
     * Returns the card ID, or -1 to decline.
     */
    int requestPostRoundCardChoice(PlayerSnapshot player, List<CardSnapshot> available);
}
