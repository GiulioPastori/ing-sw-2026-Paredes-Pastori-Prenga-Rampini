package it.polimi.it.observer;

import it.polimi.it.enums.Era;

import java.util.List;

/** Domain events published by Game to its registered listeners.

 * All data carried by events is immutable (PlayerSnapshot, CardSnapshot,
 * BoardSnapshot, OfferTileSnapshot, TurnOrderTileSnapshot). Listeners never
 * receive direct references to model objects and therefore cannot alter
 * game state.

 * Each event carries everything the Controller and View need to react,
 * so the Controller never needs to query the model after receiving an event:
      - RoundStarted carries the full turn order and a BoardSnapshot.
     - TotemPlaced carries the updated BoardSnapshot.
     - TotemReturnedToTurnOrder carries the updated BoardSnapshot.
     - CardSlotReady carries the available cards inline.
     - PostRoundStarted carries Watchtower players and available cards inline.

 * Player tribes (acquired characters and buildings) travel inside{ PlayerSnapshot},
 *  so any event that already carries a player snapshot also carries that player's up-to-date tribe. */
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

    /** A new round has started.
     * round=round number (1–10)
     * turnOrder=players in placement order (first to last); each
     *                  player snapshot carries their full tribe
     * board = full snapshot of the board state at round start */
    record RoundStarted(
            int                  round,
            List<PlayerSnapshot> turnOrder,
            BoardSnapshot        board
    ) implements GameEvent {}

    /** A player has placed their totem on an offer tile.
     *  player=snapshot of the player after placement
     * tileLetter=letter of the chosen tile
     * board=full snapshot of the board state after the placement */

    record TotemPlaced(
            PlayerSnapshot player,
            char           tileLetter,
            BoardSnapshot  board
    ) implements GameEvent {}

    /** The model is waiting for a TakeCardAction or a PassAction.
     * player=the player who has to choose
     * isTopRow =true if the slot is in the top row
     * availableCards= the cards that can be selected
     * visibleEvents =the event cards visible in the row (cannot be taken, shown for info only)
     * slotsRemaining=the slots still left to consume after this one */

    record CardSlotReady(
            PlayerSnapshot     player,
            boolean            isTopRow,
            List<CardSnapshot> availableCards,
            List<CardSnapshot> visibleEvents,
            int                slotsRemaining
    ) implements GameEvent {}

    /** A player has acquired a card. The { PlayerSnapshot} reflects
     * the tribe AFTER the new card was added.
     * player is a snapshot of the player after acquisition
     * card is snapshot of the acquired card */


    record CardAcquired(
            PlayerSnapshot player,
            CardSnapshot   card
    ) implements GameEvent {}

    /** A player attempted to take a building but had insufficient food.
     * The card stays on the board; the controller may propose an alternative.
     * player=snapshot of the player
     * card=snapshot of the building that could not be taken
     * missingFood= how much food is still needed
     * fromTopRow=true if the failed card was in the top row */


    record CardAcquisitionFailed(
            PlayerSnapshot     player,
            CardSnapshot       card,
            int                missingFood,
            boolean            fromTopRow,
            List<CardSnapshot> alternatives
    ) implements GameEvent {}

    /** A player finished their tile action and placed their totem
     * on the Turn Order Tile.
     * player =snapshot of the player after repositioning
     * position =space occupied (1 = first)
     * board = full snapshot of the board state after the move */
    record TotemReturnedToTurnOrder(
            PlayerSnapshot player,
            int            position,
            BoardSnapshot  board
    ) implements GameEvent {}

    /**
     * Post-round phase: all totems are on the Turn Order Tile.
     * Only players listed in watchtowerPlayers must respond with PostRoundAction.
     * round =current round
     * watchtowerPlayers= players who own the Watchtower building
     * availableCards= top-row cards available for the Watchtower action */


    record PostRoundStarted(
            int                  round,
            List<PlayerSnapshot> watchtowerPlayers,
            List<CardSnapshot>   availableCards
    ) implements GameEvent {}

    /** An event card has been resolved.
     * eventId= ID of the event card
     * era=era of the event
     * type= type of the event
     *  deltas =per-player resource changes caused by this event */
    record EventResolved(
            int                          eventId,
            Era                          era,
            it.polimi.it.enums.EventType type,
            List<PlayerResourceDelta>    deltas
    ) implements GameEvent {}

    /** A new era has begun. */
    record EraChanged(Era newEra) implements GameEvent {}

    /** The round has ended (after events, discard, and row refill). */
    record RoundEnded(int round) implements GameEvent {}

    /** The game is over.
     * winners=snapshots of the winning player(s); each carries the final tribe
     *  finalScores=snapshots of all players with their final scores and tribes
     * scoreBreakdowns=detailed final score calculation for every players */
    record GameEnded(
            List<PlayerSnapshot> winners,
            List<PlayerSnapshot> finalScores,
            List<FinalScoreBreakdown> scoreBreakdowns
    ) implements GameEvent {}
}
