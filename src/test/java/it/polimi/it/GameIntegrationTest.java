package it.polimi.it;


import it.polimi.it.actions.*;
import it.polimi.it.cards.building.Building;
import it.polimi.it.cards.Card;
import it.polimi.it.cards.character.*;
import it.polimi.it.cards.events.*;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.GameEvent;
import it.polimi.it.observer.PlayerSnapshot;
import it.polimi.it.player.Player;
import it.polimi.it.tiles.OfferTile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Game - full game integration")
class GameIntegrationTest {

    private static final long SEED = 12345L;
    private static final AtomicInteger TEST_ID = new AtomicInteger(9000);
    private static int nextId() { return TEST_ID.incrementAndGet(); }

    private GameBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new GameBuilder()
                .withPlayers(List.of("Alice", "Bob"),
                        List.of(TotemColour.RED, TotemColour.BLUE))
                .withCards(buildMinimalCardSet())
                .withRandom(new Random(SEED));
    }

    @Test
    @DisplayName("game completes all 10 rounds without throwing")
    void gameRunsToCompletion() {
        Game game = builder.build();
        assertDoesNotThrow(() -> runGame(game));
        assertTrue(game.isGameOver());
    }

    @Test
    @DisplayName("game ends after exactly 10 rounds")
    void gameEndsAfterTenRounds() {
        Game game = builder.build();
        runGame(game);
        assertEquals(Game.TOTAL_ROUNDS, game.getCurrentRound());
    }

    @Test
    @DisplayName("at least one winner with non-negative final score")
    void atLeastOneWinnerWithNonNegativeScore() {
        Game game = builder.build();
        runGame(game);
        List<Player> winners = game.getWinners();
        assertFalse(winners.isEmpty());
        winners.forEach(w -> assertTrue(w.calculateFinalScore() >= 0));
    }

    @Test
    @DisplayName("GameEnded event is published exactly once")
    void gameEndedEventPublishedOnce() {
        Game game = builder.build();
        List<GameEvent> ended = new ArrayList<>();
        game.addListener(e -> { if (e instanceof GameEvent.GameEnded) ended.add(e); });
        runGame(game);
        assertEquals(1, ended.size());
    }

    @Test
    @DisplayName("GameEnded event carries all players and at least one winner")
    void gameEndedEventPayload() {
        Game game = builder.build();
        GameEvent.GameEnded[] ref = new GameEvent.GameEnded[1];
        game.addListener(e -> { if (e instanceof GameEvent.GameEnded ge) ref[0] = ge; });
        runGame(game);
        assertNotNull(ref[0]);
        assertEquals(2, ref[0].finalScores().size());
        assertFalse(ref[0].winners().isEmpty());
    }

    @Test
    @DisplayName("RoundStarted events published in order 1..10")
    void roundNumbersAreMonotonic() {
        Game game = builder.build();
        List<Integer> rounds = new ArrayList<>();
        game.addListener(e -> {
            if (e instanceof GameEvent.RoundStarted r) rounds.add(r.round());
        });
        runGame(game);
        assertEquals(10, rounds.size());
        for (int i = 0; i < rounds.size(); i++) assertEquals(i + 1, rounds.get(i));
    }

    @Test
    @DisplayName("same seed produces identical final scores")
    void sameSeedProducesSameFinalScores() {
        Game g1 = new GameBuilder()
                .withPlayers(List.of("Alice", "Bob"),
                        List.of(TotemColour.RED, TotemColour.BLUE))
                .withCards(buildMinimalCardSet())
                .withRandom(new Random(SEED)).build();
        Game g2 = new GameBuilder()
                .withPlayers(List.of("Alice", "Bob"),
                        List.of(TotemColour.RED, TotemColour.BLUE))
                .withCards(buildMinimalCardSet())
                .withRandom(new Random(SEED)).build();
        runGame(g1);
        runGame(g2);
        int s1 = g1.getPlayers().stream()
                .filter(p -> p.getName().equals("Alice"))
                .mapToInt(Player::calculateFinalScore).findFirst().orElse(-1);
        int s2 = g2.getPlayers().stream()
                .filter(p -> p.getName().equals("Alice"))
                .mapToInt(Player::calculateFinalScore).findFirst().orElse(-2);
        assertEquals(s1, s2);
    }

    // ── Driver ────────────────────────────────────────────────────────────────

    /**
     * Drives a full 10-round game using testMode.
     *
     * testMode prevents Game.endRound() from auto-calling beginRound(), so we
     * control each round explicitly from outside without re-entrancy issues.
     *
     * Game.publish() is synchronous: CardSlotReady events fired inside
     * execute() are captured immediately in slotState[] and processed in the
     * next loop iteration.
     */
    private static void runGame(Game game) {
        game.setTestMode(true);

        CardSlotState[]  slotState = {null};
        PostRoundState[] postState = {null};

        game.addListener(event -> {
            switch (event) {
                case GameEvent.CardSlotReady e ->
                        slotState[0] = new CardSlotState(
                                e.player().name(), e.availableCards(), e.isTopRow());
                case GameEvent.CardAcquisitionFailed e ->
                        slotState[0] = new CardSlotState(
                                e.player().name(), List.of(), e.fromTopRow());
                case GameEvent.PostRoundStarted e ->
                        postState[0] = new PostRoundState(e.watchtowerPlayers());
                default -> {}
            }
        });

        game.start();

        while (!game.isGameOver()) {
            driveRound(game, slotState, postState);
            if (!game.isGameOver()) {
                game.nextRound();
            }
        }
    }

    /**
     * Drives a single round to completion.
     *
     * Phase 1 - PLACING_TOTEMS: places one totem at a time for each player
     * in turn order, preferring non-food tiles. The phase transition to
     * RESOLVING_ACTIONS is triggered inside Game when all players have placed.
     *
     * Phase 2 - RESOLVING_ACTIONS: each CardSlotReady captured in slotState
     * is consumed by taking the first available card or passing. The next
     * CardSlotReady (if any) is set synchronously inside execute().
     *
     * Phase 3 - POST_ROUND: Watchtower players always decline (-1).
     */
    private static void driveRound(Game game,
                                   CardSlotState[]  slotState,
                                   PostRoundState[] postState) {
        // Phase 1: place all totems
        while (game.getCurrentPhase() == Game.Phase.PLACING_TOTEMS) {
            List<OfferTile> free = game.getFreeTiles();
            if (free.isEmpty()) break;

            boolean placed = false;
            for (Player p : game.getPlacementOrder()) {
                boolean alreadyPlaced = game.getBoard().getOfferTiles()
                        .getAllTiles().stream()
                        .anyMatch(t -> !t.isFree()
                                && t.getPlacedTotem() == p.getTotemColour());
                if (!alreadyPlaced) {
                    OfferTile chosen = free.stream()
                            .filter(t -> !t.isFoodOnly())
                            .findFirst()
                            .orElse(free.get(0));
                    game.execute(new PlaceTotemAction(p.getName(), chosen.getLetter()));
                    placed = true;
                    break;
                }
            }
            if (!placed) break;
        }

        // Phase 2: handle card slots
        while (game.getCurrentPhase() == Game.Phase.RESOLVING_ACTIONS
                && slotState[0] != null) {
            String playerName = slotState[0].playerName();
            List<CardSnapshot> cards = slotState[0].cards();
            boolean fromTop = slotState[0].fromTop();
            slotState[0] = null;

            if (cards.isEmpty()) {
                game.execute(new PassAction(playerName));
            } else {
                var result = game.execute(new TakeCardAction(
                        playerName, cards.get(0).id(), fromTop));
                if (!result.isOk()) {
                    game.execute(new PassAction(playerName));
                }
            }
        }

        // Phase 3: Watchtower post-round action (always decline)
        if (game.getCurrentPhase() == Game.Phase.POST_ROUND
                && postState[0] != null) {
            PostRoundState post = postState[0];
            postState[0] = null;
            for (PlayerSnapshot p : post.players()) {
                game.execute(new PostRoundAction(p.name(), -1));
            }
        }
    }

    private record CardSlotState(String playerName, List<CardSnapshot> cards, boolean fromTop) {}
    private record PostRoundState(List<PlayerSnapshot> players) {}

    // ── Card set ──────────────────────────────────────────────────────────────

    private static List<Card> buildMinimalCardSet() {
        List<Card> c = new ArrayList<>();

        for (int i = 0; i < 10; i++) c.add(new Hunter( nextId(), Era.ERA_I,  i%3==0));
        for (int i = 0; i < 6;  i++) c.add(new Gatherer(nextId(), Era.ERA_I));
        for (int i = 0; i < 4;  i++) c.add(new Shaman(  nextId(), Era.ERA_I, (i%3)+1));
        for (int i = 0; i < 4;  i++) c.add(new Builder( nextId(), Era.ERA_I, 1, 3));
        for (int i = 0; i < 4;  i++) c.add(new Artist(  nextId(), Era.ERA_I));
        for (int i = 0; i < 4;  i++) c.add(new Inventor(nextId(), Era.ERA_I,
                new String[]{"fire","arrow","wheel","rope"}[i]));
        c.add(new Event(nextId(), Era.ERA_I, EventType.SUSTENANCE,     1, new Sustenance(1)));
        c.add(new Event(nextId(), Era.ERA_I, EventType.HUNT,           1, new Hunt(1)));
        c.add(new Event(nextId(), Era.ERA_I, EventType.SHAMANIC_RITUAL,1, new ShamanicRitual(5,3)));
        c.add(new Event(nextId(), Era.ERA_I, EventType.CAVE_PAINTINGS, 1, new CavePaintings(2,3,2)));

        for (int i = 0; i < 10; i++) c.add(new Hunter( nextId(), Era.ERA_II, i%4==0));
        for (int i = 0; i < 6;  i++) c.add(new Gatherer(nextId(), Era.ERA_II));
        for (int i = 0; i < 4;  i++) c.add(new Shaman(  nextId(), Era.ERA_II, (i%3)+1));
        for (int i = 0; i < 4;  i++) c.add(new Builder( nextId(), Era.ERA_II, 1, 4));
        for (int i = 0; i < 4;  i++) c.add(new Artist(  nextId(), Era.ERA_II));
        for (int i = 0; i < 4;  i++) c.add(new Inventor(nextId(), Era.ERA_II,
                new String[]{"pottery","spear","loom","canoe"}[i]));
        c.add(new Event(nextId(), Era.ERA_II, EventType.SUSTENANCE,     2, new Sustenance(2)));
        c.add(new Event(nextId(), Era.ERA_II, EventType.HUNT,           2, new Hunt(2)));
        c.add(new Event(nextId(), Era.ERA_II, EventType.SHAMANIC_RITUAL,2, new ShamanicRitual(10,5)));
        c.add(new Event(nextId(), Era.ERA_II, EventType.CAVE_PAINTINGS, 2, new CavePaintings(2,5,3)));

        for (int i = 0; i < 10; i++) c.add(new Hunter( nextId(), Era.ERA_III, i%4==0));
        for (int i = 0; i < 6;  i++) c.add(new Gatherer(nextId(), Era.ERA_III));
        for (int i = 0; i < 4;  i++) c.add(new Shaman(  nextId(), Era.ERA_III, (i%3)+1));
        for (int i = 0; i < 4;  i++) c.add(new Builder( nextId(), Era.ERA_III, 2, 5));
        for (int i = 0; i < 4;  i++) c.add(new Artist(  nextId(), Era.ERA_III));
        for (int i = 0; i < 4;  i++) c.add(new Inventor(nextId(), Era.ERA_III,
                new String[]{"fire","arrow","wheel","rope"}[i]));
        c.add(new Event(nextId(), Era.ERA_III, EventType.SUSTENANCE,     3, new Sustenance(3)));
        c.add(new Event(nextId(), Era.ERA_III, EventType.HUNT,           3, new Hunt(3)));
        c.add(new Event(nextId(), Era.ERA_III, EventType.SHAMANIC_RITUAL,3, new ShamanicRitual(15,8)));
        c.add(new Event(nextId(), Era.ERA_III, EventType.CAVE_PAINTINGS, 3, new CavePaintings(2,7,4)));

        c.add(new FinalEvent(nextId(), EventType.HUNT,       4, new Hunt(4)));
        c.add(new FinalEvent(nextId(), EventType.SUSTENANCE, 4, new Sustenance(4)));

        for (int i = 0; i < 3; i++) c.add(new Building(301, Era.ERA_I,   2, 2, "25 fixed PP"));
        for (int i = 0; i < 4; i++) c.add(new Building(205, Era.ERA_II,  3, 3, "6 PP per set"));
        for (int i = 0; i < 5; i++) c.add(new Building(301, Era.ERA_III, 4, 5, "25 fixed PP"));

        return c;
    }
}