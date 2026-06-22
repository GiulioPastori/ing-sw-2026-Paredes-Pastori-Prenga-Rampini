package it.polimi.it;

import it.polimi.it.actions.*;
import it.polimi.it.cards.building.Building;
import it.polimi.it.cards.Card;
import it.polimi.it.cards.character.Character;
import it.polimi.it.cards.events.Event;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.observer.BoardSnapshot;
import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.FinalScoreBreakdown;
import it.polimi.it.observer.GameEvent;
import it.polimi.it.observer.GameEventListener;
import it.polimi.it.observer.PlayerSnapshot;
import it.polimi.it.player.Player;
import it.polimi.it.tiles.OfferTile;
import it.polimi.it.tiles.TurnPosition;
import it.polimi.it.validation.ActionValidator;
import it.polimi.it.validation.ValidationResult;

import java.util.*;
import java.util.logging.Logger;

/**
 * Pure game model (Model role in MVC).
 * Responsibilities:
 * - Hold the complete game state
 * - Enforce the game rules
 * - Publish immutable GameEvents to registered listeners
 * - Validate and apply PlayerActions received via execute()
 * Does not know the controller, the view, or the network layer.
 * Internal state machine (Phase):
 * WAITING -> PLACING_TOTEMS -> RESOLVING_ACTIONS -> POST_ROUND -> (loop | ENDED)
 * Offer Track note: the number of tiles is always players+1 (one tile is never
 * used each round). The action phase starts when placed totems == players.size(),
 * not when no free tiles remain.
 */
public class Game {

    private static final Logger LOG = Logger.getLogger(Game.class.getName());

    public static final int TOTAL_ROUNDS = 10;

    private final List<Player>            players;
    private final Board                   board;
    private final List<GameEventListener> listeners;

    private int     currentRound;
    private boolean gameOver;
    private final Set<String> disconnectedPlayers = new HashSet<>();
    /** Giocatori riconnessi durante RESOLVING_ACTIONS/POST_ROUND: rientrano al prossimo beginRound. */
    private final Set<String> pendingReconnect     = new HashSet<>();
    private boolean testMode = false;
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

    public void start() {
        if (currentPhase != Phase.WAITING) {
            throw new IllegalStateException("Game already started.");
        }
        beginRound();
    }

    // ── Action execution ──────────────────────────────────────────────────────

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

        // Bug fix 1: enforce turn order (rulebook p.4).
        // Players place in Turn Order Tile sequence (top to bottom).
        // Disconnected players are skipped — they were placed automatically
        // by autoPlaceDisconnectedTotems() at round start.
        long alreadyPlaced = board.getOfferTiles().getAllTiles().stream()
                .filter(t -> !t.isFree()).count();
        List<Player> connectedOrder = getPlacementOrder().stream()
                .filter(p -> !disconnectedPlayers.contains(p.getName()))
                .toList();
        long connectedPlaced = board.getOfferTiles().getAllTiles().stream()
                .filter(t -> !t.isFree())
                .map(t -> getPlayerByTotem(t.getPlacedTotem()))
                .filter(p -> !disconnectedPlayers.contains(p.getName()))
                .count();
        if ((int) connectedPlaced < connectedOrder.size()) {
            Player expected = connectedOrder.get((int) connectedPlaced);
            if (!expected.equals(player)) {
                return ValidationResult.fail("It is " + expected.getName()
                        + "'s turn to place a totem, not " + player.getName() + ".");
            }
        }

        List<OfferTile> free = board.getOfferTiles().getFreeTiles();
        ValidationResult v = ActionValidator.validate(action, player, free);
        if (!v.isOk()) return v;

        OfferTile tile = findTile(action.tileLetter());
        board.getOfferTiles().placeTotem(player, tile);

        publish(new GameEvent.TotemPlaced(
                PlayerSnapshot.of(player),
                action.tileLetter(),
                BoardSnapshot.of(board)));

        // Start action phase when all CONNECTED players have placed.
        // Disconnected players skip placement, so count only connected ones.
        long connTotal = players.stream()
                .filter(p -> !disconnectedPlayers.contains(p.getName())).count();
        long connPlaced = board.getOfferTiles().getAllTiles().stream()
                .filter(t -> !t.isFree())
                .map(t -> getPlayerByTotem(t.getPlacedTotem()))
                .filter(p -> !disconnectedPlayers.contains(p.getName()))
                .count();
        if (connTotal == 0 || connPlaced == connTotal) {
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

        LOG.info("[advanceToNext] prossima tessera occupata: "
                + (next == null ? "nessuna" : next.getLetter()));

        if (next == null) {
            beginPostRoundPhase();
            return;
        }

        Player actor = getPlayerByTotem(next.getPlacedTotem());
        LOG.info("[advanceToNext] giocatore attivo: " + actor.getName()
                + " tessera=" + next.getLetter()
                + " isFoodOnly=" + next.isFoodOnly()
                + " top=" + next.getTopRowCards()
                + " bot=" + next.getBottomRowCards());

        // Auto-skip: if the tile's owner is disconnected, return their totem
        // immediately (no card slot, no input wait) and move to the next tile.
        if (disconnectedPlayers.contains(actor.getName())) {
            LOG.info("[advanceToNext] " + actor.getName()
                    + " e' disconnesso — tessera saltata automaticamente.");
            returnTotemToTurnOrder(actor, next);
            advanceToNextOccupiedTile();
            return;
        }

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
        buildCurrentCardSlotReadyEvent().ifPresent(this::publish);
    }

    private Optional<GameEvent.CardSlotReady> buildCurrentCardSlotReadyEvent() {
        if (currentTileState == null || currentTileState.isExhausted()) return Optional.empty();
        boolean    isTop = currentTileState.remainingTopCards() > 0;
        List<Card> row   = isTop ? board.getTopRow() : board.getBottomRow();

        // Acquirable cards only; event cards remain visible but cannot be taken.
        List<CardSnapshot> available = row.stream()
                .filter(c -> !(c instanceof Event))
                .map(CardSnapshot::of)
                .toList();

        // Event cards visible in the same row.
        List<CardSnapshot> visibleEvents = row.stream()
                .filter(c -> c instanceof Event)
                .map(CardSnapshot::of)
                .toList();

        return Optional.of(new GameEvent.CardSlotReady(
                PlayerSnapshot.of(currentTileState.getActingPlayer()),
                isTop,
                available,
                visibleEvents,
                currentTileState.totalSlotsRemaining() - 1));
    }

    private ValidationResult handleTakeCard(Player player, TakeCardAction action) {
        if (currentPhase != Phase.RESOLVING_ACTIONS) {
            return ValidationResult.fail("Not the time to take a card.");
        }
        ValidationResult v = ActionValidator.validate(action, player, currentTileState);
        if (!v.isOk()) return v;

        List<Card> row = action.fromTopRow() ? board.getTopRow() : board.getBottomRow();
        Card card = findCardInRow(action.cardId(), row);
        if (card == null) {
            return ValidationResult.fail("Card ID " + action.cardId()
                    + " not found in the indicated row.");
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
                    "Insufficient food: need " + missing
                            + " more for card ID " + card.getId());
        }
        return ValidationResult.ok();
    }

    private ValidationResult handlePass(Player player, PassAction action) {
        if (currentPhase != Phase.RESOLVING_ACTIONS) {
            return ValidationResult.fail("Not the time to pass.");
        }
        ValidationResult v = ActionValidator.validate(action, player, currentTileState);
        if (!v.isOk()) return v;

        // If top slots are still pending, the current choice belongs to the top
        // row; otherwise it belongs to the bottom row. Do not use
        // "remainingBottomCards == 0": on tile E both counters can be positive
        // and that formula would consume the bottom slot too early.
        boolean fromTop = currentTileState.remainingTopCards() > 0;
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
            returnTotemToTurnOrder(
                    currentTileState.getActingPlayer(), currentTileState.getTile());
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
                PlayerSnapshot.of(player),
                pos.getPosition(),
                BoardSnapshot.of(board)));
    }

    // ── Post-round phase (Watchtower building) ────────────────────────────────

    private void beginPostRoundPhase() {
        LOG.info("[beginPostRound] inizio fase post-round");
        pendingPostRound = new ArrayList<>();
        for (Player p : players) {
            if (p.getTribe().getBuildings().stream()
                    .anyMatch(Building::hasPostRoundAction)) {
                pendingPostRound.add(p);
            }
        }
        LOG.info("[beginPostRound] giocatori con Torre di Guardia: " + pendingPostRound.size());
        if (pendingPostRound.isEmpty()) {
            endRound();
            return;
        }

        currentPhase = Phase.POST_ROUND;
        publish(buildPostRoundStartedEvent());
    }

    private GameEvent.PostRoundStarted buildPostRoundStartedEvent() {
        List<CardSnapshot> available = board.getTopRow().stream()
                .filter(c -> !(c instanceof Event))
                .map(CardSnapshot::of)
                .toList();

        return new GameEvent.PostRoundStarted(
                currentRound,
                pendingPostRound.stream().map(PlayerSnapshot::of).toList(),
                available);
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
        LOG.info("[endRound] Inizio fine round " + currentRound);
        boolean isLastRound = (currentRound == TOTAL_ROUNDS);

        if (isLastRound) {
            LOG.info("[endRound] Ultimo round - risolvo tutti gli eventi visibili");
            board.resolveAllVisibleEvents(players)
                    .forEach(r -> publish(new GameEvent.EventResolved(
                            r.event().getId(), r.event().getEra(), r.event().getType(), r.deltas())));
        } else {
            LOG.info("[endRound] Risolvo eventi riga inferiore");
            board.resolveBottomRowEvents(players)
                    .forEach(r -> publish(new GameEvent.EventResolved(
                            r.event().getId(), r.event().getEra(), r.event().getType(), r.deltas())));
            LOG.info("[endRound] clearBottomRow");
            board.clearBottomRowExceptBuildings();
            LOG.info("[endRound] moveTopToBottom");
            board.moveTopToBottom();
            LOG.info("[endRound] refillTopRow");
            boolean eraChanged = board.refillTopRow(players.size());
            LOG.info("[endRound] refillTopRow completato, eraChanged=" + eraChanged);
            if (eraChanged) handleEraChange();
        }

        LOG.info("[endRound] TurnOrderTile.reset()");
        board.getTurnOrderTile().reset();
        LOG.info("[endRound] publish RoundEnded");
        publish(new GameEvent.RoundEnded(currentRound));
        LOG.info("[endRound] Fine round " + currentRound + " completata");

        if (isLastRound) {
            endGame();
        } else if (!testMode) {
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

        // Apply end-of-game bonuses before taking snapshots and determining
        // the winner; keep a breakdown so the view can explain the total.
        List<FinalScoreBreakdown> breakdowns = new ArrayList<>();
        for (Player p : players) {
            int inGamePrestige = p.getPrestigePoints();
            int builderPoints  = p.getTribe().computeBuilderPoints();
            int inventorPoints = p.getTribe().computeInventorPoints();
            int artistPoints   = p.getTribe().computeArtistPoints();
            int buildingPoints = p.getTribe().computeBuildingPoints(p);
            int totalPrestige  = inGamePrestige + builderPoints + inventorPoints
                    + artistPoints + buildingPoints;
            breakdowns.add(new FinalScoreBreakdown(
                    p.getName(),
                    inGamePrestige,
                    builderPoints,
                    inventorPoints,
                    artistPoints,
                    buildingPoints,
                    totalPrestige,
                    p.getFood()));
            p.addPrestigePoints(totalPrestige - inGamePrestige);
        }

        List<Player> winners = getWinners();
        publish(new GameEvent.GameEnded(
                winners.stream().map(PlayerSnapshot::of).toList(),
                players.stream().map(PlayerSnapshot::of).toList(),
                breakdowns));
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
        LOG.info("[beginRound] starting round " + currentRound);

        // Bring back players who reconnected during the previous round. They
        // stayed in pendingReconnect to avoid changing the active phase.
        if (!pendingReconnect.isEmpty()) {
            LOG.info("[beginRound] restoring pending players: " + pendingReconnect);
            disconnectedPlayers.removeAll(pendingReconnect);
            pendingReconnect.clear();
        }

        List<PlayerSnapshot> order = getPlacementOrder().stream()
                .map(PlayerSnapshot::of).toList();

        BoardSnapshot snapshot = BoardSnapshot.of(board);

        LOG.info("[beginRound] total tiles: " + snapshot.offerTiles().size()
                + " (free: " + snapshot.offerTiles().stream()
                .filter(t -> t.isFree()).count() + ")"
                + " top=" + board.getTopRow().size() + " bottom=" + board.getBottomRow().size()
                + " deck=" + snapshot.tribeDeckSize());
        publish(new GameEvent.RoundStarted(currentRound, order, snapshot));
        autoPlaceDisconnectedTotems();
    }

    // ── Winner calculation ────────────────────────────────────────────────────

    /**
     * Returns the winner(s) based on prestigePoints (which include end-game
     * bonuses after endGame() has applied them).
     * Tiebreaker: most food. Absolute tie: shared victory (rulebook p.7).
     * Must only be called from endGame().
     */
    public List<Player> getWinners() {
        int maxScore = players.stream()
                .mapToInt(Player::getPrestigePoints).max().orElse(0);
        List<Player> top = players.stream()
                .filter(p -> p.getPrestigePoints() == maxScore).toList();
        if (top.size() == 1) return top;
        int maxFood = top.stream().mapToInt(Player::getFood).max().orElse(0);
        return top.stream().filter(p -> p.getFood() == maxFood).toList();
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private Player findPlayer(String name) {
        return players.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Player not found: " + name));
    }

    private Player getPlayerByTotem(TotemColour totem) {
        return players.stream()
                .filter(p -> p.getTotemColour() == totem)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No player with totem: " + totem));
    }

    private OfferTile findTile(char letter) {
        return board.getOfferTiles().getAllTiles().stream()
                .filter(t -> t.getLetter() == letter)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tile not found: " + letter));
    }

    private Card findCardInRow(int cardId, List<Card> row) {
        return row.stream()
                .filter(c -> c.getId() == cardId)
                .findFirst()
                .orElse(null);
    }

    // Package-private: used by tests
    void  setTestMode(boolean testMode) { this.testMode = testMode; }
    void  nextRound()                   { if (!gameOver) beginRound(); }
    Phase getCurrentPhase()             { return currentPhase; }
    Board getBoard()                    { return board; }

    List<Player> getPlacementOrder() {
        List<Player> ordered = new ArrayList<>(players);
        ordered.sort(Comparator.comparingInt(Player::getTurnOrder));
        return ordered;
    }

    // ── Resilience: disconnect/reconnect support ──────────────────────────────

    public void markDisconnected(String nickname) {
        disconnectedPlayers.add(nickname);

        if (currentPhase == Phase.PLACING_TOTEMS) {
            // Disconnected player skips placement:
            // - not yet placed → totem stays in TurnOrderTile (nothing to do)
            // - already placed → totem stays on tile; advanceToNextOccupiedTile skips it
            // Either way, check if all remaining connected players have placed.
            long connTotal = players.stream()
                    .filter(p -> !disconnectedPlayers.contains(p.getName())).count();
            long connPlaced = board.getOfferTiles().getAllTiles().stream()
                    .filter(t -> !t.isFree())
                    .map(t -> getPlayerByTotem(t.getPlacedTotem()))
                    .filter(p -> !disconnectedPlayers.contains(p.getName()))
                    .count();
            if (connTotal == 0 || connPlaced == connTotal) {
                beginActionPhase();
            }

        } else if (currentPhase == Phase.RESOLVING_ACTIONS) {
            // Case A: the disconnected player is the current actor — skip their tile.
            if (currentTileState != null
                    && currentTileState.getActingPlayer().getName().equals(nickname)) {
                returnTotemToTurnOrder(
                        currentTileState.getActingPlayer(), currentTileState.getTile());
                currentTileState = null;
                advanceToNextOccupiedTile();
            }
            // Case B: the player is waiting in queue (their totem is on a later tile).
            // advanceToNextOccupiedTile() already checks disconnectedPlayers and auto-skips,
            // so no extra action is needed here — the skip happens when we reach their tile.

        } else if (currentPhase == Phase.POST_ROUND && pendingPostRound != null) {
            pendingPostRound.removeIf(p -> p.getName().equals(nickname));
            if (pendingPostRound.isEmpty()) endRound();
        }
    }

    public void markReconnected(String nickname) {
        // If reconnection happens during RESOLVING_ACTIONS or POST_ROUND, the
        // player cannot re-enter the current round: their totem was already
        // auto-skipped or was never placed. Keep the player in
        // disconnectedPlayers until beginRound() removes them at the start of
        // the next round, when everyone can place again.
        if (currentPhase == Phase.RESOLVING_ACTIONS || currentPhase == Phase.POST_ROUND) {
            // Mark the player as pending re-entry: the model still treats them
            // as disconnected for this round, while GameSession already knows
            // that the transport has reconnected.
            pendingReconnect.add(nickname);
        } else {
            disconnectedPlayers.remove(nickname);
        }
    }

    public boolean isDisconnected(String nickname) {
        return disconnectedPlayers.contains(nickname);
    }

    private void autoPlaceDisconnectedTotems() {
        // Disconnected players do NOT place a totem:
        // - Before placing: totem stays in TurnOrderTile.
        // - After placing: totem stays on the chosen tile; action phase skips them.
        // Edge case: if everyone is disconnected, start action phase to unblock.
        long connected = players.stream()
                .filter(p -> !disconnectedPlayers.contains(p.getName())).count();
        if (connected == 0) beginActionPhase();
    }

    // ── Public getters ────────────────────────────────────────────────────────

    public boolean      isGameOver()      { return gameOver; }
    public int          getCurrentRound() { return currentRound; }
    public List<Player> getPlayers()      { return List.copyOf(players); }

    public BoardSnapshot getBoardSnapshot() { return BoardSnapshot.of(board); }

    public List<it.polimi.it.observer.PlayerSnapshot> getTurnOrderSnapshots() {
        return getPlacementOrder().stream()
                .map(it.polimi.it.observer.PlayerSnapshot::of)
                .toList();
    }

    public String getCurrentPhaseName() {
        return currentPhase.name();
    }

    public Optional<GameEvent> currentPromptEvent() {
        return switch (currentPhase) {
            case RESOLVING_ACTIONS -> buildCurrentCardSlotReadyEvent().map(e -> (GameEvent) e);
            case POST_ROUND -> pendingPostRound == null || pendingPostRound.isEmpty()
                    ? Optional.empty()
                    : Optional.of(buildPostRoundStartedEvent());
            default -> Optional.empty();
        };
    }
}
