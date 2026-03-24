package it.polimi.it;

import it.polimi.it.cards.building.Building;
import it.polimi.it.cards.Card;
import it.polimi.it.cards.character.*;
import it.polimi.it.cards.event.*;
import it.polimi.it.cards.events.Event;
import it.polimi.it.cards.events.FinalEvent;
import it.polimi.it.cards.events.Hunt;
import it.polimi.it.cards.events.Sustenance;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GameBuilder.
 *
 * Covers input validation, determinism (same seed → same game), and
 * basic setup correctness (player count, starting food, totem colours,
 * turn order).
 */
@DisplayName("GameBuilder")
class GameBuilderTest {

    /** Test-only IDs above all real game ranges. */
    private static final AtomicInteger TEST_ID = new AtomicInteger(8000);
    private static int nextId() { return TEST_ID.incrementAndGet(); }

    private List<Card> minimalCards;

    @BeforeEach
    void setUp() {
        minimalCards = buildMinimalCards();
    }

    // ── Input validation ──────────────────────────────────────────────────────

    @Test
    @DisplayName("duplicate player names throw IllegalArgumentException")
    void duplicateNamesThrow() {
        assertThrows(IllegalArgumentException.class, () ->
                new GameBuilder()
                        .withPlayers(List.of("Alice", "Alice"),
                                List.of(TotemColour.RED, TotemColour.BLUE))
                        .withCards(minimalCards).build());
    }

    @Test
    @DisplayName("duplicate totem colours throw IllegalArgumentException")
    void duplicateColoursThrow() {
        assertThrows(IllegalArgumentException.class, () ->
                new GameBuilder()
                        .withPlayers(List.of("Alice", "Bob"),
                                List.of(TotemColour.RED, TotemColour.RED))
                        .withCards(minimalCards).build());
    }

    @Test
    @DisplayName("fewer than 2 players throw IllegalArgumentException")
    void tooFewPlayersThrow() {
        assertThrows(IllegalArgumentException.class, () ->
                new GameBuilder()
                        .withPlayers(List.of("Alice"), List.of(TotemColour.RED)));
    }

    @Test
    @DisplayName("more than 5 players throw IllegalArgumentException")
    void tooManyPlayersThrow() {
        assertThrows(IllegalArgumentException.class, () ->
                new GameBuilder().withPlayers(
                        List.of("A","B","C","D","E","F"),
                        List.of(TotemColour.RED, TotemColour.BLUE, TotemColour.YELLOW,
                                TotemColour.WHITE, TotemColour.BLACK, TotemColour.RED)));
    }

    @Test
    @DisplayName("build() without withCards() throws IllegalStateException")
    void buildWithoutCardsThrows() {
        assertThrows(IllegalStateException.class, () ->
                new GameBuilder()
                        .withPlayers(List.of("Alice","Bob"),
                                List.of(TotemColour.RED, TotemColour.BLUE))
                        .build());
    }

    @Test
    @DisplayName("build() without withPlayers() throws IllegalStateException")
    void buildWithoutPlayersThrows() {
        assertThrows(IllegalStateException.class, () ->
                new GameBuilder().withCards(minimalCards).build());
    }

    // ── Setup correctness ─────────────────────────────────────────────────────

    @Test
    @DisplayName("built game has the correct number of players")
    void correctPlayerCount() {
        Game game = builder2P().build();
        assertEquals(2, game.getPlayers().size());
    }

    @Test
    @DisplayName("all players have starting food greater than zero")
    void allPlayersHaveStartingFood() {
        Game game = builder2P().build();
        for (Player p : game.getPlayers()) {
            assertTrue(p.getFood() > 0,
                    p.getName() + " should have starting food > 0");
        }
    }

    @Test
    @DisplayName("all players have distinct totem colours")
    void distinctTotemColours() {
        Game game = builder2P().build();
        long distinct = game.getPlayers().stream()
                .map(Player::getTotemColour).distinct().count();
        assertEquals(2, distinct);
    }

    @Test
    @DisplayName("all players have a turn order position between 1 and 2")
    void turnOrderAssigned() {
        Game game = builder2P().build();
        for (Player p : game.getPlayers()) {
            int order = p.getTurnOrder();
            assertTrue(order >= 1 && order <= 2,
                    "Turn order should be 1 or 2, got: " + order);
        }
    }

    // ── Determinism ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("same seed produces the same turn order")
    void sameSeedSameTurnOrder() {
        Game g1 = builder2P().withRandom(new Random(42)).build();
        Game g2 = builder2P().withRandom(new Random(42)).build();

        List<Player> p1 = g1.getPlacementOrder();
        List<Player> p2 = g2.getPlacementOrder();

        assertEquals(p1.get(0).getName(), p2.get(0).getName());
        assertEquals(p1.get(1).getName(), p2.get(1).getName());
    }

    @Test
    @DisplayName("different seeds can produce different turn orders")
    void differentSeedsCanDiffer() {
        String firstOrder = null;
        boolean foundDifferent = false;
        for (int seed = 0; seed < 20; seed++) {
            Game g = builder2P().withRandom(new Random(seed)).build();
            String order = g.getPlacementOrder().get(0).getName();
            if (firstOrder == null)          { firstOrder = order; }
            else if (!order.equals(firstOrder)) { foundDifferent = true; break; }
        }
        assertTrue(foundDifferent, "Different seeds should sometimes produce different orders");
    }

    // ─────────────────────────────────────────────────────────────────────────

    private GameBuilder builder2P() {
        return new GameBuilder()
                .withPlayers(List.of("Alice", "Bob"),
                        List.of(TotemColour.RED, TotemColour.BLUE))
                .withCards(buildMinimalCards());
    }

    /**
     * Minimal card set for a 2-player game.
     * Character and event IDs are test-only (>= 8000).
     * Building IDs reuse real effect IDs so BuildingEffectRegistry works.
     */
    private static List<Card> buildMinimalCards() {
        List<Card> cards = new ArrayList<>();

        // Enough characters across eras to fill setup rows (2P: top=6, bottom=3)
        for (int i = 0; i < 10; i++) cards.add(new Hunter(nextId(),  Era.ERA_I,   false));
        for (int i = 0; i < 4;  i++) cards.add(new Gatherer(nextId(),Era.ERA_I));
        for (int i = 0; i < 6;  i++) cards.add(new Hunter(nextId(),  Era.ERA_II,  false));
        for (int i = 0; i < 6;  i++) cards.add(new Hunter(nextId(),  Era.ERA_III, false));

        // Events (at least one per era, plus finals)
        cards.add(new Event(nextId(), Era.ERA_I,   EventType.SUSTENANCE, 1, new Sustenance(1)));
        cards.add(new Event(nextId(), Era.ERA_I,   EventType.HUNT,       1, new Hunt(1)));
        cards.add(new Event(nextId(), Era.ERA_II,  EventType.SUSTENANCE, 2, new Sustenance(2)));
        cards.add(new Event(nextId(), Era.ERA_II,  EventType.HUNT,       2, new Hunt(2)));
        cards.add(new Event(nextId(), Era.ERA_III, EventType.SUSTENANCE, 3, new Sustenance(3)));
        cards.add(new Event(nextId(), Era.ERA_III, EventType.HUNT,       3, new Hunt(3)));
        cards.add(new FinalEvent(nextId(), EventType.HUNT,       4, new Hunt(4)));
        cards.add(new FinalEvent(nextId(), EventType.SUSTENANCE, 4, new Sustenance(4)));

        // Buildings: real IDs so effects work; 2P needs 1 EraI + 2 EraII + 3 EraIII
        for (int i = 0; i < 3; i++) cards.add(new Building(301, Era.ERA_I,   0, 1, ""));
        for (int i = 0; i < 4; i++) cards.add(new Building(205, Era.ERA_II,  0, 1, ""));
        for (int i = 0; i < 5; i++) cards.add(new Building(301, Era.ERA_III, 0, 1, ""));

        return cards;
    }
}