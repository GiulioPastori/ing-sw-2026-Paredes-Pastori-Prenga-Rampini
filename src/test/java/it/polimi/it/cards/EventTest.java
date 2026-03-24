package it.polimi.it.cards;

import it.polimi.it.cards.events.Event;
import it.polimi.it.cards.events.EventStrategy;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;
import it.polimi.it.player.Player;
import it.polimi.it.enums.TotemColour;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventTest {

    private it.polimi.it.player.Player player1;
    private Player player2;
    private List<Player> players;

    @BeforeEach
    void setUp() {
        player1 = new Player("Alice", TotemColour.RED);
        player2 = new Player("Bob",   TotemColour.BLUE);
        players = List.of(player1, player2);
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    @Test
    void constructor_nullStrategy_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                new Event(2001, Era.ERA_I, EventType.HUNT, 1, null));
    }

    @Test
    void constructor_validArguments_setsFieldsCorrectly() {
        EventStrategy strategy = ps -> {};
        Event event = new Event(2001, Era.ERA_I, EventType.HUNT, 1, strategy);

        assertEquals(2001,          event.getId());
        assertEquals(Era.ERA_I,     event.getEra());
        assertEquals(EventType.HUNT, event.getType());
        assertEquals(1,             event.getValue());
    }

    // ── resolve() ────────────────────────────────────────────────────────────

    @Test
    void resolve_strategyIsInvoked() {
        // Strategy adds 5 PP to every player
        EventStrategy strategy = ps -> ps.forEach(p -> p.addPrestigePoints(5));
        Event event = new Event(2002, Era.ERA_I, EventType.HUNT, 1, strategy);

        event.resolve(players);

        assertEquals(5, player1.getPrestigePoints());
        assertEquals(5, player2.getPrestigePoints());
    }

    @Test
    void resolve_strategyReceivesAllPlayers() {
        // Strategy counts how many players it receives
        int[] count = {0};
        EventStrategy strategy = ps -> count[0] = ps.size();
        Event event = new Event(2003, Era.ERA_I, EventType.SUSTENANCE, 1, strategy);

        event.resolve(players);

        assertEquals(2, count[0]);
    }

    @Test
    void resolve_negativePrestigePoints_allowed() {
        // Sustenance-like: subtract PP if player can't feed
        EventStrategy strategy = ps -> ps.forEach(p -> p.addPrestigePoints(-3));
        Event event = new Event(2001, Era.ERA_I, EventType.SUSTENANCE, 1, strategy);

        event.resolve(players);

        assertEquals(-3, player1.getPrestigePoints());
        assertEquals(-3, player2.getPrestigePoints());
    }

    @Test
    void resolve_emptyPlayerList_doesNotThrow() {
        EventStrategy strategy = ps -> ps.forEach(p -> p.addPrestigePoints(5));
        Event event = new Event(2004, Era.ERA_I, EventType.CAVE_PAINTINGS, 1, strategy);

        assertDoesNotThrow(() -> event.resolve(List.of()));
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    @Test
    void getType_returnsCorrectEventType() {
        Event event = new Event(2021, Era.ERA_II, EventType.SHAMANIC_RITUAL, 2, ps -> {});
        assertEquals(EventType.SHAMANIC_RITUAL, event.getType());
    }

    @Test
    void getValue_returnsCorrectValue() {
        Event event = new Event(2022, Era.ERA_II, EventType.HUNT, 3, ps -> {});
        assertEquals(3, event.getValue());
    }
}