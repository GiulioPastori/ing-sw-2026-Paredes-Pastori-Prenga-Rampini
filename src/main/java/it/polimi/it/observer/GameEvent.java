package it.polimi.it.observer;

import it.polimi.it.enums.Era;

import java.util.List;

/**
 * Domain events published by Game to its registered listeners.
 *
 * <p>All data carried by events is immutable. Listeners receive snapshots
 * rather than direct model objects, so they cannot alter game state.
 *
 * <p>Each event carries the data the controller and view need to react without
 * querying the model again. Player tribes travel inside {@link PlayerSnapshot},
 * so any event that carries a player snapshot also carries that player's
 * up-to-date tribe.
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
     * @param round round number
     * @param turnOrder players in placement order, from first to last
     * @param board full board state at round start
     */
    record RoundStarted(
            int                  round,
            List<PlayerSnapshot> turnOrder,
            BoardSnapshot        board
    ) implements GameEvent {}

    /**
     * A player has placed their totem on an offer tile.
     *
     * @param player player snapshot after placement
     * @param tileLetter letter of the chosen tile
     * @param board full board state after placement
     */
    record TotemPlaced(
            PlayerSnapshot player,
            char           tileLetter,
            BoardSnapshot  board
    ) implements GameEvent {}

    /**
     * The model is waiting for a TakeCardAction or a PassAction.
     *
     * @param player player who has to choose
     * @param isTopRow true if the slot is in the top row
     * @param availableCards selectable cards in the current row
     * @param visibleEvents event cards visible in the row; they cannot be taken
     * @param slotsRemaining slots left to consume after this one
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
     * @param player player snapshot after the new card was added to the tribe
     * @param card acquired card snapshot
     */
    record CardAcquired(
            PlayerSnapshot player,
            CardSnapshot   card
    ) implements GameEvent {}

    /**
     * A player attempted to take a building but had insufficient food.
     * The card stays on the board; the controller may propose an alternative.
     *
     * @param player player snapshot
     * @param card building card that could not be taken
     * @param missingFood food still needed
     * @param fromTopRow true if the failed card was in the top row
     * @param alternatives alternative cards in the same row
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
     * @param player player snapshot after repositioning
     * @param position occupied turn-order space; 1 is first
     * @param board full board state after the move
     */
    record TotemReturnedToTurnOrder(
            PlayerSnapshot player,
            int            position,
            BoardSnapshot  board
    ) implements GameEvent {}

    /**
     * Post-round phase: all totems are on the Turn Order Tile.
     * Only players listed in watchtowerPlayers must respond with PostRoundAction.
     *
     * @param round current round
     * @param watchtowerPlayers players who own the Watchtower building
     * @param availableCards top-row cards available for the Watchtower action
     */
    record PostRoundStarted(
            int                  round,
            List<PlayerSnapshot> watchtowerPlayers,
            List<CardSnapshot>   availableCards
    ) implements GameEvent {}

    /**
     * An event card has been resolved.
     *
     * @param eventId event card identifier
     * @param era era of the event card
     * @param type event type
     * @param deltas per-player resource changes caused by this event
     */
    record EventResolved(
            int                          eventId,
            Era                          era,
            it.polimi.it.enums.EventType type,
            List<PlayerResourceDelta>    deltas
    ) implements GameEvent {}

    /**
     * A new era has begun.
     *
     * @param newEra newly active era
     */
    record EraChanged(Era newEra) implements GameEvent {}

    /**
     * The round has ended after events, discard, and row refill.
     *
     * @param round ended round number
     */
    record RoundEnded(int round) implements GameEvent {}

    /**
     * The game is over.
     *
     * @param winners winning player snapshots, including final tribes
     * @param finalScores all player snapshots with final scores and tribes
     * @param scoreBreakdowns detailed final-score calculations
     */
    record GameEnded(
            List<PlayerSnapshot> winners,
            List<PlayerSnapshot> finalScores,
            List<FinalScoreBreakdown> scoreBreakdowns
    ) implements GameEvent {}
}
