package it.polimi.it.cards.character;

import it.polimi.it.enums.Era;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.player.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Shaman.
 *
 * FSM model (single state: SHAMAN_CARD):
 * - Constructor validates star icon range
 * - getStarIcons() is an observer
 * - applyImmediateEffect() is a no-op
 */
@DisplayName("Shaman")
class ShamanTest {

    @ParameterizedTest(name = "valid star count {0}")
    @ValueSource(ints = {1, 2, 3})
    @DisplayName("valid star icon counts 1-3 are accepted")
    void validStarIconsAccepted(int starIcons) {
        assertDoesNotThrow(() -> new Shaman(2001, Era.ERA_I, starIcons));
    }

    @ParameterizedTest(name = "invalid star count {0}")
    @ValueSource(ints = {0, 4, -1, 10})
    @DisplayName("star icon count outside 1-3 throws IllegalArgumentException")
    void invalidStarIconsThrow(int starIcons) {
        assertThrows(IllegalArgumentException.class,
                () -> new Shaman(2002, Era.ERA_I, starIcons));
    }

    @Test
    @DisplayName("getStarIcons returns the value provided at construction")
    void getStarIconsReturnsConstructedValue() {
        assertEquals(2, new Shaman(2003, Era.ERA_I, 2).getStarIcons());
    }

    @Test
    @DisplayName("applyImmediateEffect does not change player food or PP")
    void applyImmediateEffectIsNoOp() {
        Player player = new Player("Alice", TotemColour.RED);
        player.addFood(5);
        player.addPrestigePoints(3);

        new Shaman(2004, Era.ERA_I, 2).applyImmediateEffect(player);

        assertEquals(5, player.getFood());
        assertEquals(3, player.getPrestigePoints());
    }
}