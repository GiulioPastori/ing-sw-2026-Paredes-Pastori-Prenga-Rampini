package it.polimi.it.cards.character;

import it.polimi.it.enums.Era;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Hunter.
 *
 * FSM model:
 *   Transition T1: acquire hunter WITHOUT trigger icon -> no food change
 *   Transition T2: acquire hunter WITH trigger icon -> food += current hunter count
 *
 * Test cases cover:
 * - TC-1: no-trigger hunter, isBound = false, food unchanged
 * - TC-2: trigger hunter added first, food = 1 (only itself)
 * - TC-3: trigger hunter added after a no-trigger one, food = 2 (both hunters)
 * - TC-4: two trigger hunters, second grants food = 2 (cumulative)
 */
@DisplayName("Hunter")
class HunterTest {

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player("Alice", TotemColour.RED);
    }

    @Test
    @DisplayName("hunter without trigger icon grants no immediate food")
    void noTriggerGrantsNoFood() {
        player.takeCharacterCard(new Hunter(2001, Era.ERA_I, false));
        assertEquals(0, player.getFood());
    }

    @Test
    @DisplayName("trigger hunter added first counts only itself")
    void firstTriggerHunterGrantsOneFood() {
        player.takeCharacterCard(new Hunter(2002, Era.ERA_I, true));
        assertEquals(1, player.getFood()); // 1 hunter in tribe
    }

    @Test
    @DisplayName("trigger hunter added after a plain hunter counts both")
    void triggerHunterCountsAllHunters() {
        player.takeCharacterCard(new Hunter(2003, Era.ERA_I, false)); // no food
        player.takeCharacterCard(new Hunter(2004, Era.ERA_I, true));  // 2 hunters -> +2
        assertEquals(2, player.getFood());
    }

    @Test
    @DisplayName("two trigger hunters accumulate correctly")
    void twoTriggerHuntersAccumulate() {
        player.takeCharacterCard(new Hunter(2005, Era.ERA_I, true)); // 1 hunter -> +1
        int afterFirst = player.getFood();
        assertEquals(1, afterFirst);

        player.takeCharacterCard(new Hunter(2006, Era.ERA_I, true)); // 2 hunters -> +2
        assertEquals(3, player.getFood()); // 1 + 2
    }

    @Test
    @DisplayName("hasTriggerIcon reflects the constructor argument")
    void hasTriggerIconReflectsConstructor() {
        assertTrue(new Hunter(2007, Era.ERA_I, true).hasTriggerIcon());
        assertFalse(new Hunter(2008, Era.ERA_I, false).hasTriggerIcon());
    }
}