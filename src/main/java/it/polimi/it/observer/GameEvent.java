package it.polimi.it.observer;

import it.polimi.it.enums.Era;

import java.util.List;

/**
 * Domain events published by Game to its registered listeners.
 *
 * All data carried by events is immutable (PlayerSnapshot, CardSnapshot,
 * OfferTileSnapshot). Listeners never receive direct references to model
 * objects and therefore cannot alter game state.
 *
 * Each event carries everything the Controller and View need to react,
 * so the Controller never needs to query the model after receiving an event:
 * - RoundStarted carries the full turn order and the initial free tiles
 * - TotemPlaced carries the tiles still free after the placement
 * - CardSlotReady carries the available cards inline
 * - PostRoundStarted carries Watchtower players and available cards inline
 */
public sealed interface GameEvent permits
        GameEvent.RoundStarted,
        GameEvent.TotemPlaced,
        GameEvent.CardSlotReady,
        GameEvent.CardAcquired,
        GameEvent.CardAcquisitionFailed,
        GameEvent.TotemReturnedToTurnOrder,
        GameEvent.PostRoundStarted,
        GameEvent.EventResolved,
        GameEvent.EraChanged,
        GameEvent.RoundEnded,
        GameEvent.GameEnded {

    /**
     * A new round has started.
     *
     * @param round          round number (1–10)
     * @param turnOrder      players in placement order (first to last)
     * @param availableTiles offer tiles that are currently free
     */
    record RoundStarted(
            int                      round,
            List<PlayerSnapshot>     turnOrder,
            List<OfferTileSnapshot>  availableTiles
    ) implements GameEvent {}

    /**
     * A player has placed their totem on an offer tile.
     *
     * @param player         snapshot of the player after placement
     * @param tileLetter     letter of the chosen tile
     * @param remainingTiles offer tiles still free after this placement
     */
    record TotemPlaced(
            PlayerSnapshot          player,
            char                    tileLetter,
            List<OfferTileSnapshot> remainingTiles
    ) implements GameEvent {}

    /**
     * Il modello è in attesa di un TakeCardAction o PassAction.
     *
     * @param player         il giocatore che deve scegliere
     * @param isTopRow       true se lo slot è nella riga superiore
     * @param availableCards carte selezionabili (esclusi gli eventi)
     * @param visibleEvents  carte evento visibili nella riga (non acquisibili, solo informative)
     * @param slotsRemaining slot ancora da consumare dopo questo
     */
    record CardSlotReady(
            PlayerSnapshot     player,
            boolean            isTopRow,
            List<CardSnapshot> availableCards,
            List<CardSnapshot> visibleEvents,
            int                slotsRemaining
    ) implements GameEvent {}

    /**
     * A player has acquired a card.
     *
     * @param player snapshot of the player after acquisition
     * @param card   snapshot of the acquired card
     */
    record CardAcquired(
            PlayerSnapshot player,
            CardSnapshot   card
    ) implements GameEvent {}

    /**
     * A player attempted to take a building but had insufficient food.
     * The card stays on the board; the controller may propose an alternative.
     *
     * @param player       snapshot of the player
     * @param card         snapshot of the building that could not be taken
     * @param missingFood  how much food is still needed
     * @param fromTopRow   true if the failed card was in the top row
     * @param alternatives other selectable cards in the same row
     */
    record CardAcquisitionFailed(
            PlayerSnapshot     player,
            CardSnapshot       card,
            int                missingFood,
            boolean            fromTopRow,
            List<CardSnapshot> alternatives
    ) implements GameEvent {}

    /**
     * A player finished their tile action and placed their totem
     * on the Turn Order Tile.
     *
     * @param player   snapshot of the player after repositioning
     * @param position space occupied (1 = first)
     */
    record TotemReturnedToTurnOrder(
            PlayerSnapshot player,
            int            position
    ) implements GameEvent {}

    /**
     * Post-round phase: all totems are on the Turn Order Tile.
     * Only players listed in watchtowerPlayers must respond with PostRoundAction.
     *
     * @param round             current round
     * @param watchtowerPlayers players who own the Watchtower building
     * @param availableCards    top-row cards available for the Watchtower action
     */
    record PostRoundStarted(
            int                  round,
            List<PlayerSnapshot> watchtowerPlayers,
            List<CardSnapshot>   availableCards
    ) implements GameEvent {}

    /**
     * An event card has been resolved.
     *
     * @param eventId ID of the event card
     * @param era     era of the event
     * @param type    type of the event (for display)
     */
    record EventResolved(
            int                        eventId,
            Era                        era,
            it.polimi.it.enums.EventType type
    ) implements GameEvent {}

    /** A new era has begun. */
    record EraChanged(Era newEra) implements GameEvent {}

    /** The round has ended (after events, discard, and row refill). */
    record RoundEnded(int round) implements GameEvent {}

    /**
     * The game is over.
     *
     * @param winners     snapshots of the winning player(s)
     * @param finalScores snapshots of all players with their final scores
     */
    record GameEnded(
            List<PlayerSnapshot> winners,
            List<PlayerSnapshot> finalScores
    ) implements GameEvent {}
}