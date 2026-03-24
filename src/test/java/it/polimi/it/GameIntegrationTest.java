package it.polimi.it;

import it.polimi.it.cards.Building;
import it.polimi.it.cards.Card;
import it.polimi.it.cards.character.*;
import it.polimi.it.cards.event.*;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.GameEvent;
import it.polimi.it.observer.OfferTileSnapshot;
import it.polimi.it.observer.PlayerSnapshot;
import it.polimi.it.player.Player;
import it.polimi.it.view.GameView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: simulates a complete 2-player game using GameController
 * with a deterministic stub view (always picks the first available option).
 *
 * Cards are assigned IDs starting from 9000 (above all real game IDs) so
 * they never collide with real IDs from BuildingEffectRegistry.
 * A fixed Random seed makes the test fully reproducible.
 */
@DisplayName("Game – full game integration")
class GameIntegrationTest {

    private static final long SEED = 12345L;

    /** Counter for test-only character/event IDs (above all real ranges). */
    private static final AtomicInteger TEST_ID = new AtomicInteger(9000);
    private static int nextId() { return TEST_ID.incrementAndGet(); }

    private List<Card>  cards;
    private GameBuilder builder;

    @BeforeEach
    void setUp() {
        cards = buildMinimalCardSet();
        builder = new GameBuilder()
                .withPlayers(List.of("Alice", "Bob"),
                             List.of(TotemColour.RED, TotemColour.BLUE))
                .withCards(cards)
                .withRandom(new Random(SEED));
    }

    @Test
    @DisplayName("game completes all 10 rounds without throwing")
    void gameRunsToCompletion() {
        Game game = builder.build();
        assertDoesNotThrow(() -> new GameController(game, new FirstChoiceView()).startGame());
        assertTrue(game.isGameOver());
    }

    @Test
    @DisplayName("game ends after exactly 10 rounds")
    void gameEndsAfterTenRounds() {
        Game game = builder.build();
        new GameController(game, new FirstChoiceView()).startGame();
        assertEquals(Game.TOTAL_ROUNDS, game.getCurrentRound());
    }

    @Test
    @DisplayName("at least one winner is declared with a non-negative final score")
    void atLeastOneWinnerWithNonNegativeScore() {
        Game game = builder.build();
        new GameController(game, new FirstChoiceView()).startGame();
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
        new GameController(game, new FirstChoiceView()).startGame();
        assertEquals(1, ended.size());
    }

    @Test
    @DisplayName("GameEnded event carries all players and at least one winner")
    void gameEndedEventPayload() {
        Game game = builder.build();
        GameEvent.GameEnded[] ref = new GameEvent.GameEnded[1];
        game.addListener(e -> { if (e instanceof GameEvent.GameEnded ge) ref[0] = ge; });
        new GameController(game, new FirstChoiceView()).startGame();
        assertNotNull(ref[0]);
        assertEquals(2, ref[0].finalScores().size());
        assertFalse(ref[0].winners().isEmpty());
    }

    @Test
    @DisplayName("each player has acquired at least one card after the game")
    void eachPlayerHasNonEmptyTribe() {
        Game game = builder.build();
        new GameController(game, new FirstChoiceView()).startGame();
        for (Player p : game.getPlayers()) {
            assertTrue(p.getTribe().getCharacterCount() > 0
                       || !p.getTribe().getBuildings().isEmpty(),
                       p.getName() + " must have acquired at least one card");
        }
    }

    @Test
    @DisplayName("RoundStarted events are published in order 1..10")
    void roundNumbersAreMonotonic() {
        Game game = builder.build();
        List<Integer> rounds = new ArrayList<>();
        game.addListener(e -> { if (e instanceof GameEvent.RoundStarted r) rounds.add(r.round()); });
        new GameController(game, new FirstChoiceView()).startGame();
        assertEquals(10, rounds.size());
        for (int i = 0; i < rounds.size(); i++) assertEquals(i + 1, rounds.get(i));
    }

    @Test
    @DisplayName("same seed produces identical final scores")
    void sameSeedProducesSameFinalScores() {
        Game g1 = new GameBuilder()
                .withPlayers(List.of("Alice","Bob"), List.of(TotemColour.RED, TotemColour.BLUE))
                .withCards(buildMinimalCardSet()).withRandom(new Random(SEED)).build();
        Game g2 = new GameBuilder()
                .withPlayers(List.of("Alice","Bob"), List.of(TotemColour.RED, TotemColour.BLUE))
                .withCards(buildMinimalCardSet()).withRandom(new Random(SEED)).build();

        new GameController(g1, new FirstChoiceView()).startGame();
        new GameController(g2, new FirstChoiceView()).startGame();

        int s1 = g1.getPlayers().stream()
                .filter(p -> p.getName().equals("Alice"))
                .mapToInt(Player::calculateFinalScore).findFirst().orElse(-1);
        int s2 = g2.getPlayers().stream()
                .filter(p -> p.getName().equals("Alice"))
                .mapToInt(Player::calculateFinalScore).findFirst().orElse(-2);
        assertEquals(s1, s2);
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds a card set sufficient for a full 10-round 2-player game.
     *
     * Character and event cards get unique test-only IDs (>= 9000).
     * Building cards reuse real effect IDs (101-399) so that
     * BuildingEffectRegistry resolves their effects correctly.
     */
    private static List<Card> buildMinimalCardSet() {
        List<Card> c = new ArrayList<>();

        // Era I
        for (int i = 0; i < 8; i++) c.add(new Hunter( nextId(), Era.ERA_I,   i%3==0));
        for (int i = 0; i < 5; i++) c.add(new Gatherer(nextId(), Era.ERA_I));
        for (int i = 0; i < 3; i++) c.add(new Shaman(  nextId(), Era.ERA_I,   (i%3)+1));
        for (int i = 0; i < 3; i++) c.add(new Builder( nextId(), Era.ERA_I,   1, 3));
        for (int i = 0; i < 3; i++) c.add(new Artist(  nextId(), Era.ERA_I));
        String[] icI  = {"fire","arrow","wheel","rope","net"};
        for (int i = 0; i < 5; i++) c.add(new Inventor(nextId(), Era.ERA_I,   icI[i]));

        // Era II
        for (int i = 0; i < 8; i++) c.add(new Hunter( nextId(), Era.ERA_II,  i%4==0));
        for (int i = 0; i < 5; i++) c.add(new Gatherer(nextId(), Era.ERA_II));
        for (int i = 0; i < 3; i++) c.add(new Shaman(  nextId(), Era.ERA_II,  (i%3)+1));
        for (int i = 0; i < 3; i++) c.add(new Builder( nextId(), Era.ERA_II,  1, 4));
        for (int i = 0; i < 3; i++) c.add(new Artist(  nextId(), Era.ERA_II));
        String[] icII = {"pottery","spear","loom","canoe"};
        for (int i = 0; i < 4; i++) c.add(new Inventor(nextId(), Era.ERA_II,  icII[i]));

        // Era III
        for (int i = 0; i < 8; i++) c.add(new Hunter( nextId(), Era.ERA_III, i%4==0));
        for (int i = 0; i < 5; i++) c.add(new Gatherer(nextId(), Era.ERA_III));
        for (int i = 0; i < 3; i++) c.add(new Shaman(  nextId(), Era.ERA_III, (i%3)+1));
        for (int i = 0; i < 3; i++) c.add(new Builder( nextId(), Era.ERA_III, 2, 5));
        for (int i = 0; i < 3; i++) c.add(new Artist(  nextId(), Era.ERA_III));
        String[] icIII = {"fire","arrow","wheel","rope"};
        for (int i = 0; i < 4; i++) c.add(new Inventor(nextId(), Era.ERA_III, icIII[i]));

        // Events (4 per era + 2 finals)
        c.add(new Event(nextId(), Era.ERA_I,   EventType.SUSTENANCE,     1, new Sustenance(1)));
        c.add(new Event(nextId(), Era.ERA_I,   EventType.HUNT,           1, new Hunt(1)));
        c.add(new Event(nextId(), Era.ERA_I,   EventType.SHAMANIC_RITUAL,1, new ShamanicRitual(5,3)));
        c.add(new Event(nextId(), Era.ERA_I,   EventType.CAVE_PAINTINGS, 1, new CavePaintings(2,3,2)));
        c.add(new Event(nextId(), Era.ERA_II,  EventType.SUSTENANCE,     2, new Sustenance(2)));
        c.add(new Event(nextId(), Era.ERA_II,  EventType.HUNT,           2, new Hunt(2)));
        c.add(new Event(nextId(), Era.ERA_II,  EventType.SHAMANIC_RITUAL,2, new ShamanicRitual(10,5)));
        c.add(new Event(nextId(), Era.ERA_II,  EventType.CAVE_PAINTINGS, 2, new CavePaintings(2,5,3)));
        c.add(new Event(nextId(), Era.ERA_III, EventType.SUSTENANCE,     3, new Sustenance(3)));
        c.add(new Event(nextId(), Era.ERA_III, EventType.HUNT,           3, new Hunt(3)));
        c.add(new Event(nextId(), Era.ERA_III, EventType.SHAMANIC_RITUAL,3, new ShamanicRitual(15,8)));
        c.add(new Event(nextId(), Era.ERA_III, EventType.CAVE_PAINTINGS, 3, new CavePaintings(2,7,4)));
        c.add(new FinalEvent(nextId(), EventType.HUNT,       4, new Hunt(4)));
        c.add(new FinalEvent(nextId(), EventType.SUSTENANCE, 4, new Sustenance(4)));

        // Buildings use real effect IDs so BuildingEffectRegistry works correctly.
        // Duplicate IDs are intentional: the game uses multiple copies of the same card type.
        for (int i = 0; i < 4; i++) c.add(new Building(301, Era.ERA_I,   2, 2, "25 fixed PP"));
        for (int i = 0; i < 5; i++) c.add(new Building(205, Era.ERA_II,  3, 3, "6 PP per set"));
        for (int i = 0; i < 6; i++) c.add(new Building(301, Era.ERA_III, 4, 5, "25 fixed PP"));

        return c;
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Silent GameView stub that always picks the first available option.
     * Uses the updated interface: requestXxx methods return primitives.
     */
    private static class FirstChoiceView implements GameView {

        @Override
        public char requestOfferTileChoice(PlayerSnapshot player,
                                           List<OfferTileSnapshot> freeTiles) {
            return freeTiles.get(0).letter();
        }

        @Override
        public int requestCardChoice(PlayerSnapshot player, List<CardSnapshot> available) {
            return available.isEmpty() ? -1 : available.get(0).id();
        }

        @Override
        public int requestPostRoundCardChoice(PlayerSnapshot player,
                                              List<CardSnapshot> available) {
            return -1; // always decline Watchtower
        }

        @Override public void showRoundStarted(int round, List<PlayerSnapshot> order) {}
        @Override public void showTotemPlaced(PlayerSnapshot p, char letter) {}
        @Override public void showCardSlotReady(PlayerSnapshot p, boolean top,
                                                List<CardSnapshot> cards) {}
        @Override public void showCardAcquired(PlayerSnapshot p, CardSnapshot c) {}
        @Override public void showCardAcquisitionFailed(PlayerSnapshot p, CardSnapshot c,
                                                        int missing) {}
        @Override public void showTotemReturnedToTurnOrder(PlayerSnapshot p, int pos) {}
        @Override public void showPostRoundStarted(List<PlayerSnapshot> wt,
                                                   List<CardSnapshot> cards) {}
        @Override public void showEventResolved(int eventId, Era era, EventType type) {}
        @Override public void showEraChanged(Era era) {}
        @Override public void showRoundEnded(int round) {}
        @Override public void showInvalidAction(String player, String reason) {}
        @Override public void showGameEnded(List<PlayerSnapshot> winners,
                                            List<PlayerSnapshot> scores) {}
    }
}
