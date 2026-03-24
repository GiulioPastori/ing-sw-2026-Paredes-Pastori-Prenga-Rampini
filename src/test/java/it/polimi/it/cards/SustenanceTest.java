package it.polimi.it.cards;

import it.polimi.it.cards.character.Gatherer;
import it.polimi.it.cards.character.Hunter;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.player.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Sustenance event strategy.
 *
 * Transition coverage:
 * - T1: no characters -> no food spent, no PP lost
 * - T2: characters, full food -> exact food spent
 * - T3: characters, partial food -> food fully spent, PP penalty for unfed
 * - T4: gatherer discount reduces food required
 * - T5: penalty scales linearly with unfed character count
 */
@DisplayName("Sustenance")
class SustenanceTest {

    private static final int PENALTY = 2;
    private final Sustenance sustenance = new Sustenance(PENALTY);

    private Player player(int food, int characters) {
        Player p = new Player("P", TotemColour.RED);
        p.addFood(food);
        for (int i = 0; i < characters; i++) {
            p.takeCharacterCard(new Hunter(7400 + i, Era.ERA_I, false));
        }
        return p;
    }

    @Test
    @DisplayName("T1: no characters means no food spent and no PP lost")
    void noCharactersNoEffect() {
        Player p = new Player("P", TotemColour.RED);
        p.addFood(5);
        sustenance.apply(List.of(p));
        assertEquals(5, p.getFood());
        assertEquals(0, p.getPrestigePoints());
    }

    @Test
    @DisplayName("T2: exact food available pays exactly the required amount")
    void exactFoodPaidFully() {
        Player p = player(3, 3); // 3 characters, 3 food -> 0 food left, 0 PP lost
        sustenance.apply(List.of(p));
        assertEquals(0, p.getFood());
        assertEquals(0, p.getPrestigePoints());
    }

    @Test
    @DisplayName("T3: insufficient food causes PP penalty for unfed characters")
    void insufficientFoodCausesPenalty() {
        Player p = player(1, 3); // 3 characters, 1 food -> 2 unfed -> -4 PP
        sustenance.apply(List.of(p));
        assertEquals(0, p.getFood());
        assertEquals(-2 * PENALTY, p.getPrestigePoints());
    }

    @Test
    @DisplayName("T4: gatherer discount reduces food required")
    void gathererReducesFoodRequired() {
        Player p = new Player("P", TotemColour.RED);
        p.addFood(2);
        // 1 gatherer (discount 3) + 3 hunters = 4 characters, foodRequired = max(0, 4-3) = 1
        p.takeCharacterCard(new Gatherer(5001, Era.ERA_I));
        p.takeCharacterCard(new Hunter(2001, Era.ERA_I, false));
        p.takeCharacterCard(new Hunter(2002, Era.ERA_I, false));
        p.takeCharacterCard(new Hunter(2003, Era.ERA_I, false));

        sustenance.apply(List.of(p));
        assertEquals(1, p.getFood()); // 2 - 1 = 1
        assertEquals(0, p.getPrestigePoints()); // fully fed
    }

    @Test
    @DisplayName("T5: PP penalty scales linearly with number of unfed characters")
    void penaltyScalesWithUnfedCount() {
        Player p = player(0, 5); // 5 characters, 0 food -> -10 PP
        sustenance.apply(List.of(p));
        assertEquals(-5 * PENALTY, p.getPrestigePoints());
    }

    @Test
    @DisplayName("gatherer discount cannot make food required negative")
    void gathererDiscountCannotMakeFoodNegative() {
        Player p = new Player("P", TotemColour.RED);
        p.addFood(10);
        // 1 character, 1 gatherer (discount 3): foodRequired = max(0, 1-3) = 0
        p.takeCharacterCard(new Hunter(2004, Era.ERA_I, false));
        p.takeCharacterCard(new Gatherer(5002, Era.ERA_I));

        sustenance.apply(List.of(p));
        assertEquals(10, p.getFood()); // nothing spent
        assertEquals(0, p.getPrestigePoints());
    }

    @Test
    @DisplayName("multiple players are resolved independently")
    void multiplePlayers() {
        Player p1 = player(3, 3); // fully fed
        Player p2 = player(0, 2); // 2 unfed -> -4 PP

        sustenance.apply(List.of(p1, p2));

        assertEquals(0, p1.getFood());
        assertEquals(0, p1.getPrestigePoints());
        assertEquals(0, p2.getFood());
        assertEquals(-2 * PENALTY, p2.getPrestigePoints());
    }
}