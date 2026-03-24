package it.polimi.it.player;

import it.polimi.it.cards.building.Building;
import it.polimi.it.cards.character.Builder;
import it.polimi.it.cards.character.Hunter;
import it.polimi.it.cards.character.Inventor;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.TotemColour;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Player.
 *
 * FSM model:
 *   EMPTY -(addFood)-> HAS_FOOD -(spendFood)-> EMPTY
 *   EMPTY -(takeCharacterCard)-> HAS_CHARACTERS
 *   EMPTY -(takeBuildingCard, cost>0)-> EMPTY (not acquired)
 *   HAS_FOOD -(takeBuildingCard, food>=cost)-> HAS_BUILDING
 *
 * Test cases cover all transitions and relevant boundary conditions.
 */
@DisplayName("Player")
class PlayerTest {

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player("Alice", TotemColour.RED);
    }

    // Food management

    @Test
    @DisplayName("initial food is zero")
    void initialFoodIsZero() {
        assertEquals(0, player.getFood());
    }

    @Test
    @DisplayName("addFood increases food by the given amount")
    void addFoodIncreasesSupply() {
        player.addFood(5);
        assertEquals(5, player.getFood());
        player.addFood(3);
        assertEquals(8, player.getFood());
    }

    @Test
    @DisplayName("addFood with negative amount throws IllegalArgumentException")
    void addFoodNegativeThrows() {
        assertThrows(IllegalArgumentException.class, () -> player.addFood(-1));
    }

    @Test
    @DisplayName("spendFood decreases food by the given amount")
    void spendFoodDecreasesSupply() {
        player.addFood(10);
        player.spendFood(4);
        assertEquals(6, player.getFood());
    }

    @Test
    @DisplayName("spendFood with insufficient food throws IllegalStateException")
    void spendFoodInsufficientThrows() {
        player.addFood(2);
        assertThrows(IllegalStateException.class, () -> player.spendFood(3));
    }

    @Test
    @DisplayName("spendFood with negative amount throws IllegalArgumentException")
    void spendFoodNegativeThrows() {
        assertThrows(IllegalArgumentException.class, () -> player.spendFood(-1));
    }

    @Test
    @DisplayName("spendFood exact amount leaves food at zero")
    void spendFoodExactAmountLeavesZero() {
        player.addFood(5);
        player.spendFood(5);
        assertEquals(0, player.getFood());
    }

    // Prestige points

    @Test
    @DisplayName("initial prestige points are zero")
    void initialPrestigePointsAreZero() {
        assertEquals(0, player.getPrestigePoints());
    }

    @Test
    @DisplayName("addPrestigePoints with positive amount increases PP")
    void addPrestigePointsPositive() {
        player.addPrestigePoints(10);
        assertEquals(10, player.getPrestigePoints());
    }

    @Test
    @DisplayName("addPrestigePoints with negative amount decreases PP below zero")
    void addPrestigePointsNegativeGoesNegative() {
        player.addPrestigePoints(-5);
        assertEquals(-5, player.getPrestigePoints());
    }

    // Character card acquisition

    @Test
    @DisplayName("takeCharacterCard adds character to tribe")
    void takeCharacterCardAddsTribe() {
        player.takeCharacterCard(new Hunter(2001, Era.ERA_I, false));
        assertEquals(1, player.getTribe().getCharacterCount());
    }

    @Test
    @DisplayName("Hunter with trigger icon grants food equal to hunter count on acquisition")
    void hunterTriggerIconGrantsFood() {
        // Add one hunter without trigger
        player.takeCharacterCard(new Hunter(2002, Era.ERA_I, false));
        assertEquals(0, player.getFood()); // no trigger, no food

        // Add a hunter with trigger icon: tribe has 2 hunters -> +2 food
        player.takeCharacterCard(new Hunter(2003, Era.ERA_I, true));
        assertEquals(2, player.getFood());
    }

    @Test
    @DisplayName("Hunter without trigger icon grants no immediate food")
    void hunterWithoutTriggerGrantsNoFood() {
        player.takeCharacterCard(new Hunter(2004, Era.ERA_I, false));
        assertEquals(0, player.getFood());
    }

    // Building card acquisition

    @Test
    @DisplayName("takeBuildingCard returns false when food is insufficient")
    void takeBuildingCardReturnsFalseWhenFoodInsufficient() {
        Building building = new Building(2012, Era.ERA_I, 5, 3, "");
        player.addFood(3); // cost = 5, have = 3
        assertFalse(player.takeBuildingCard(building));
        assertEquals(3, player.getFood()); // food unchanged
        assertEquals(0, player.getTribe().getBuildings().size());
    }

    @Test
    @DisplayName("takeBuildingCard returns true and deducts food when affordable")
    void takeBuildingCardSucceedsWhenAffordable() {
        Building building = new Building(2013, Era.ERA_I, 5, 3, "");
        player.addFood(7);
        assertTrue(player.takeBuildingCard(building));
        assertEquals(2, player.getFood()); // 7 - 5 = 2
        assertEquals(1, player.getTribe().getBuildings().size());
    }

    @Test
    @DisplayName("Builder in tribe reduces building cost")
    void builderReducesBuildingCost() {
        player.takeCharacterCard(new Builder(2005, Era.ERA_I, 2, 3)); // discount = 2
        Building building = new Building(2014, Era.ERA_I, 5, 3, ""); // cost 5 -> 3
        player.addFood(3);
        assertTrue(player.takeBuildingCard(building));
        assertEquals(0, player.getFood()); // 3 - 3 = 0
    }

    @Test
    @DisplayName("building cost cannot go below zero with large discount")
    void buildingCostCannotGoBelowZero() {
        player.takeCharacterCard(new Builder(2006, Era.ERA_I, 10, 3)); // discount 10 > cost 5
        Building building = new Building(2015, Era.ERA_I, 5, 3, "");
        player.addFood(0);
        assertTrue(player.takeBuildingCard(building)); // effective cost = 0
        assertEquals(0, player.getFood());
    }

    // Turn order

    @Test
    @DisplayName("setTurnOrder and getTurnOrder are consistent")
    void turnOrderRoundTrip() {
        player.setTurnOrder(3);
        assertEquals(3, player.getTurnOrder());
    }

    // Final score

    @Test
    @DisplayName("calculateFinalScore with no cards equals prestige points")
    void finalScoreWithNoCardsEqualsPrestigePoints() {
        player.addPrestigePoints(12);
        assertEquals(12, player.calculateFinalScore());
    }

    @Test
    @DisplayName("calculateFinalScore includes builder end-game points")
    void finalScoreIncludesBuilderPoints() {
        player.addPrestigePoints(5);
        player.takeCharacterCard(new Builder(2007, Era.ERA_I, 1, 4)); // 4 PP at end
        assertEquals(9, player.calculateFinalScore()); // 5 + 4
    }

    @Test
    @DisplayName("calculateFinalScore includes inventor points (count x distinct icons)")
    void finalScoreIncludesInventorPoints() {
        player.takeCharacterCard(new Inventor(2008, Era.ERA_I, "fire"));
        player.takeCharacterCard(new Inventor(2009, Era.ERA_I, "arrow"));
        // 2 inventors x 2 distinct icons = 4 PP
        assertEquals(4, player.calculateFinalScore());
    }

    @Test
    @DisplayName("two inventors with same icon yield fewer points than with different icons")
    void inventorSameIconFewerPoints() {
        player.takeCharacterCard(new Inventor(2010, Era.ERA_I, "fire"));
        player.takeCharacterCard(new Inventor(2011, Era.ERA_I, "fire")); // same icon
        // 2 inventors x 1 distinct icon = 2 PP
        assertEquals(2, player.calculateFinalScore());
    }
}