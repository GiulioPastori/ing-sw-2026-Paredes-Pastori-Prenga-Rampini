package it.polimi.it;

import it.polimi.it.actions.*;
import it.polimi.it.cards.Building;
import it.polimi.it.cards.Card;
import it.polimi.it.cards.character.Character;
import it.polimi.it.cards.event.Event;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.GameEvent;
import it.polimi.it.observer.GameEventListener;
import it.polimi.it.observer.OfferTileSnapshot;
import it.polimi.it.observer.PlayerSnapshot;
import it.polimi.it.player.Player;
import it.polimi.it.tiles.OfferTile;
import it.polimi.it.tiles.TurnPosition;
import it.polimi.it.validation.ActionValidator;
import it.polimi.it.validation.ValidationResult;

import java.util.*;

/**
 * Pure game model (Model role in MVC).
 *
 * Responsibilities:
 * - Hold the complete game state
 * - Enforce the game rules
 * - Publish immutable GameEvents to registered listeners
 * - Validate and apply PlayerActions received via execute()
 *
 * Does not know the controller, the view, or the network layer.
 *
 * Actions carry string identifiers (player name, card name, tile letter)
 * rather than direct object references, so they are naturally serialisable
 * for network transport. Game resolves identifiers to objects internally.
 *
 * Internal state machine (Phase):
 * WAITING -> PLACING_TOTEMS -> RESOLVING_ACTIONS -> POST_ROUND -> (loop | ENDED)
 */
public class Game {

    public static final int TOTAL_ROUNDS = 10;

    private final List<Player>            players;
    private final Board                   board;
    private final List<GameEventListener> listeners;

    private int     currentRound;
    private boolean gameOver;
    private Phase   currentPhase;

    private TileResolutionState currentTileState;
    private List<Player>        pendingPostRound;

    // Package-private: the state machine is an internal implementation detail.
    enum Phase { WAITING, PLACING_TOTEMS, RESOLVING_ACTIONS, POST_ROUND, ENDED }

    public Game(List<Player> players, Board board) {
        this.players      = new ArrayList<>(players);
        this.board        = board;
        this.listeners    = new ArrayList<>();
        this.currentRound = 0;
        this.gameOver     = false;
        this.currentPhase = Phase.WAITING;
    }

    // ── Observer registration ─────────────────────────────────────────────────

    public void addListener(GameEventListener listener)    { listeners.add(listener); }
    public void removeListener(GameEventListener listener) { listeners.remove(listener); }

    private void publish(GameEvent event) {
        List.copyOf(listeners).forEach(l -> l.onEvent(event));
    }

    // ── Startup ───────────────────────────────────────────────────────────────

    /** Starts the first round. */
    public void start() {
        if (currentPhase != Phase.WAITING) {
            throw new IllegalStateException("Game already started.");
        }
        beginRound();
    }

    // ── Action execution ─────────────────────────────────────────────────────

    /**
     * Validates and applies a player action.
     *
     * Actions carry string identifiers (player name, card name, tile letter)
     * so they can be built from network messages without holding references
     * to model objects. Game resolves identifiers internally.
     *
     * Returns ValidationResult.ok() if applied, Fail with a readable message
     * otherwise. Never throws for invalid user input.
     *
     * @param action the action sent by the player
     * @return validation outcome
     */
    public ValidationResult execute(PlayerAction action) {
        Player player;
        try {
            player = findPlayer(action.playerName());
        } catch (IllegalArgumentException e) {
            return ValidationResult.fail(e.getMessage());
        }
        return switch (action) {
            case PlaceTotemAction a -> handlePlaceTotem(player, a);
            case TakeCardAction   a -> handleTakeCard(player, a);
            case PassAction       a -> handlePass(player, a);
            case PostRoundAction  a -> handlePostRound(player, a);
        };
    }

    // ── Phase 1: totem placement ──────────────────────────────────────────────

    private ValidationResult handlePlaceTotem(Player player, PlaceTotemAction action) {
        if (currentPhase != Phase.PLACING_TOTEMS) {
            return ValidationResult.fail("Not the time to place a totem.");
        }
        List<OfferTile> free = board.getOfferTiles().getFreeTiles();
        ValidationResult v = ActionValidator.validate(action, player, free);
        if (!v.isOk()) return v;

        OfferTile tile = findTile(action.tileLetter());
        board.getOfferTiles().placeTotem(player, tile);

        List<OfferTileSnapshot> remaining = board.getOfferTiles().getFreeTiles().stream()
                .map(OfferTileSnapshot::of).toList();

        publish(new GameEvent.TotemPlaced(
                PlayerSnapshot.of(player),
                action.tileLetter(),
                remaining));

        if (remaining.isEmpty()) {
            beginActionPhase();
        }
        return ValidationResult.ok();
    }

    // ── Phase 2: action resolution ────────────────────────────────────────────

    private void beginActionPhase() {
        currentPhase     = Phase.RESOLVING_ACTIONS;
        currentTileState = null;
        advanceToNextOccupiedTile();
    }

    private void advanceToNextOccupiedTile() {
        OfferTile next = board.getOfferTiles().getAllTiles().stream()
                .filter(t -> !t.isFree())
                .findFirst()
                .orElse(null);

        if (next == null) {
            beginPostRoundPhase();
            return;
        }

        Player actor = getPlayerByTotem(next.getPlacedTotem());

        if (next.isFoodOnly()) {
            actor.addFood(3);
            returnTotemToTurnOrder(actor, next);
            advanceToNextOccupiedTile();
            return;
        }

        currentTileState = new TileResolutionState(actor, next);
        publishNextSlotReady();
    }

    private void publishNextSlotReady() {
        if (currentTileState == null || currentTileState.isExhausted()) return;

        boolean isTop  = currentTileState.remainingTopCards() > 0;
        List<Card> row = isTop ? board.getTopRow() : board.getBottomRow();

        List<CardSnapshot> available = row.stream()
                .filter(c -> !(c instanceof Event))
                .map(CardSnapshot::of)
                .toList();

        publish(new GameEvent.CardSlotReady(
                PlayerSnapshot.of(currentTileState.getActingPlayer()),
                isTop,
                available,
                currentTileState.totalSlotsRemaining() - 1));
    }

    private ValidationResult handleTakeCard(Player player, TakeCardAction action) {
        if (currentPhase != Phase.RESOLVING_ACTIONS) {
            return ValidationResult.fail("Not the time to take a card.");
        }
        ValidationResult v = ActionValidator.validate(action, player, currentTileState);
        if (!v.isOk()) return v;

        // Resolve card name to the concrete object
        List<Card> row = action.fromTopRow() ? board.getTopRow() : board.getBottomRow();
        Card card = findCardInRow(action.cardId(), row);
        if (card == null) {
            return ValidationResult.fail("Card ID " + action.cardId() + " not found in the indicated row.");
        }
        if (card instanceof Event) {
            return ValidationResult.fail("Event cards cannot be acquired by players.");
        }

        boolean acquired = acquireCard(player, card);

        if (acquired) {
            publish(new GameEvent.CardAcquired(
                    PlayerSnapshot.of(player),
                    CardSnapshot.of(card)));
            consumeSlot(action.fromTopRow());
        } else {
            // Building not affordable
            int cost    = ((Building) card).getEffectiveCost(player.getTribe());
            int missing = cost - player.getFood();

            List<CardSnapshot> alternatives = row.stream()
                    .filter(c -> !(c instanceof Event))
                    .filter(c -> c.getId() != action.cardId())
                    .map(CardSnapshot::of)
                    .toList();

            publish(new GameEvent.CardAcquisitionFailed(
                    PlayerSnapshot.of(player),
                    CardSnapshot.of(card),
                    missing,
                    action.fromTopRow(),
                    alternatives));

            return ValidationResult.fail(
                    "Insufficient food: need " + missing + " more for card ID " + card.getId());
        }
        return ValidationResult.ok();
    }

    private ValidationResult handlePass(Player player, PassAction action) {
        if (currentPhase != Phase.RESOLVING_ACTIONS) {
            return ValidationResult.fail("Not the time to pass.");
        }
        ValidationResult v = ActionValidator.validate(action, player, currentTileState);
        if (!v.isOk()) return v;

        boolean fromTop = currentTileState.remainingBottomCards() == 0;
        consumeSlot(fromTop);
        return ValidationResult.ok();
    }

    private void consumeSlot(boolean fromTop) {
        if (fromTop) {
            currentTileState.consumeTopSlot();
        } else {
            currentTileState.consumeBottomSlot();
        }
        if (currentTileState.isExhausted()) {
            returnTotemToTurnOrder(currentTileState.getActingPlayer(), currentTileState.getTile());
            currentTileState = null;
            advanceToNextOccupiedTile();
        } else {
            publishNextSlotReady();
        }
    }

    private void returnTotemToTurnOrder(Player player, OfferTile tile) {
        tile.removeTotem();
        TurnPosition pos = board.getTurnOrderTile().placeTotem(player);
        publish(new GameEvent.TotemReturnedToTurnOrder(
                PlayerSnapshot.of(player), pos.getPosition()));
    }

    // ── Post-round phase (Watchtower building) ────────────────────────────────

    private void beginPostRoundPhase() {
        pendingPostRound = new ArrayList<>();
        for (Player p : players) {
            if (p.getTribe().getBuildings().stream().anyMatch(Building::hasPostRoundAction)) {
                pendingPostRound.add(p);
            }
        }
        if (pendingPostRound.isEmpty()) {
            endRound();
            return;
        }

        currentPhase = Phase.POST_ROUND;

        List<CardSnapshot> available = board.getTopRow().stream()
                .filter(c -> !(c instanceof Event))
                .map(CardSnapshot::of)
                .toList();

        publish(new GameEvent.PostRoundStarted(
                currentRound,
                pendingPostRound.stream().map(PlayerSnapshot::of).toList(),
                available));
    }

    private ValidationResult handlePostRound(Player player, PostRoundAction action) {
        if (currentPhase != Phase.POST_ROUND) {
            return ValidationResult.fail("Not the time for the post-round action.");
        }
        ValidationResult v = ActionValidator.validate(action, player, pendingPostRound);
        if (!v.isOk()) return v;

        if (action.cardId() != -1) {
            Card card = findCardInRow(action.cardId(), board.getTopRow());
            if (card != null && !(card instanceof Event)) {
                acquireCard(player, card);
                publish(new GameEvent.CardAcquired(
                        PlayerSnapshot.of(player),
                        CardSnapshot.of(card)));
            }
        }
        pendingPostRound.remove(player);
        if (pendingPostRound.isEmpty()) endRound();
        return ValidationResult.ok();
    }

    // ── End of round ──────────────────────────────────────────────────────────

    private void endRound() {
        boolean isLastRound = (currentRound == TOTAL_ROUNDS);

        if (isLastRound) {
            board.resolveAllVisibleEvents(players)
                    .forEach(e -> publish(new GameEvent.EventResolved(e.getId(), e.getEra(), e.getType())));
        } else {
            board.resolveBottomRowEvents(players)
                    .forEach(e -> publish(new GameEvent.EventResolved(e.getId(), e.getEra(), e.getType())));
            board.clearBottomRowExceptBuildings();
            board.moveTopToBottom();
            boolean eraChanged = board.refillTopRow(players.size());
            if (eraChanged) handleEraChange();
        }

        board.getTurnOrderTile().reset();
        publish(new GameEvent.RoundEnded(currentRound));

        if (isLastRound) {
            endGame();
        } else {
            beginRound();
        }
    }

    private void handleEraChange() {
        Era current = board.getCurrentEra();
        Era next    = (current == Era.ERA_I) ? Era.ERA_II : Era.ERA_III;
        board.advanceEra(next);
        publish(new GameEvent.EraChanged(next));
    }

    private void endGame() {
        currentPhase = Phase.ENDED;
        gameOver     = true;
        List<Player> winners = getWinners();
        publish(new GameEvent.GameEnded(
                winners.stream().map(PlayerSnapshot::of).toList(),
                players.stream().map(PlayerSnapshot::of).toList()));
    }

    // ── Card acquisition ──────────────────────────────────────────────────────

    private boolean acquireCard(Player player, Card card) {
        if (card instanceof Building b) {
            boolean taken = player.takeBuildingCard(b);
            if (taken) board.removeCard(b);
            return taken;
        }
        if (card instanceof Character c) {
            player.takeCharacterCard(c);
            board.removeCard(c);
            return true;
        }
        return false;
    }

    // ── Round startup ─────────────────────────────────────────────────────────

    private void beginRound() {
        currentRound++;
        currentPhase = Phase.PLACING_TOTEMS;

        List<PlayerSnapshot> order = getPlacementOrder().stream()
                .map(PlayerSnapshot::of).toList();
        List<OfferTileSnapshot> freeTiles = board.getOfferTiles().getFreeTiles().stream()
                .map(OfferTileSnapshot::of).toList();

        publish(new GameEvent.RoundStarted(currentRound, order, freeTiles));
    }

    // ── Winner calculation ────────────────────────────────────────────────────

    /**
     * Returns the winner(s): highest final score; tiebreaker is most food;
     * absolute tie means shared victory (rulebook p.7).
     */
    public List<Player> getWinners() {
        int maxScore = players.stream().mapToInt(Player::calculateFinalScore).max().orElse(0);
        List<Player> top = players.stream()
                .filter(p -> p.calculateFinalScore() == maxScore).toList();
        if (top.size() == 1) return top;
        int maxFood = top.stream().mapToInt(Player::getFood).max().orElse(0);
        return top.stream().filter(p -> p.getFood() == maxFood).toList();
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private Player findPlayer(String name) {
        return players.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + name));
    }

    private Player getPlayerByTotem(TotemColour totem) {
        return players.stream()
                .filter(p -> p.getTotemColour() == totem)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No player with totem: " + totem));
    }

    private OfferTile findTile(char letter) {
        return board.getOfferTiles().getAllTiles().stream()
                .filter(t -> t.getLetter() == letter)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tile not found: " + letter));
    }

    private Card findCardInRow(int cardId, List<Card> row) {
        return row.stream()
                .filter(c -> c.getId() == cardId)
                .findFirst()
                .orElse(null);
    }

    // Package-private: used by tests.
    List<Player> getPlacementOrder() {
        List<Player> ordered = new ArrayList<>(players);
        ordered.sort(Comparator.comparingInt(Player::getTurnOrder));
        return ordered;
    }

    // ── Public getters ────────────────────────────────────────────────────────

    public boolean      isGameOver()    { return gameOver; }
    public int          getCurrentRound() { return currentRound; }
    public List<Player> getPlayers()    { return List.copyOf(players); }
}