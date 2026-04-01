package it.polimi.it.cards.building;

import it.polimi.it.cards.character.*;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BuildingEffectRegistry")
class BuildingEffectRegistryTest {

    private static Building freeBuilding(int id) {
        return new Building(id, Era.ERA_I, 0, 1, "");
    }

    // ── ID 101: complete set of 6 distinct types -> +5 food ──────────────────

    @Nested
    @DisplayName("ID 101 – complete-set bonus")
    class Id101CompleteSet {

        private Player player;

        @BeforeEach
        void setUp() {
            player = new Player("Alice", TotemColour.RED);
            assertTrue(player.takeBuildingCard(freeBuilding(101)));
        }

        @Test
        @DisplayName("no bonus before all 6 types are present")
        void noBonusBeforeSetComplete() {
            player.takeCharacterCard(new Hunter(2001, Era.ERA_I, false));
            player.takeCharacterCard(new Gatherer(5001, Era.ERA_I));
            player.takeCharacterCard(new Shaman(2002, Era.ERA_I, 1));
            player.takeCharacterCard(new Builder(2003, Era.ERA_I, 1, 2));
            player.takeCharacterCard(new Inventor(2004, Era.ERA_I, "fire"));
            // 5 of 6 types, no Artist yet
            assertEquals(0, player.getFood());
        }

        @Test
        @DisplayName("+5 food when the 6th type completes the set")
        void bonusOnSixthType() {
            player.takeCharacterCard(new Hunter(2005, Era.ERA_I, false));
            player.takeCharacterCard(new Gatherer(5002, Era.ERA_I));
            player.takeCharacterCard(new Shaman(2006, Era.ERA_I, 1));
            player.takeCharacterCard(new Builder(2007, Era.ERA_I, 1, 2));
            player.takeCharacterCard(new Inventor(2008, Era.ERA_I, "fire"));
            player.takeCharacterCard(new Artist(7001, Era.ERA_I));
            assertEquals(5, player.getFood());
        }

        @Test
        @DisplayName("+5 food again when a second complete set is formed")
        void bonusOnSecondSet() {
            player.takeCharacterCard(new Hunter(2009, Era.ERA_I, false));
            player.takeCharacterCard(new Gatherer(5003, Era.ERA_I));
            player.takeCharacterCard(new Shaman(2010, Era.ERA_I, 1));
            player.takeCharacterCard(new Builder(2011, Era.ERA_I, 1, 2));
            player.takeCharacterCard(new Inventor(2012, Era.ERA_I, "fire"));
            player.takeCharacterCard(new Artist(7002, Era.ERA_I)); // set 1
            assertEquals(5, player.getFood());

            player.takeCharacterCard(new Hunter(2013, Era.ERA_I, false));
            player.takeCharacterCard(new Gatherer(5004, Era.ERA_I));
            player.takeCharacterCard(new Shaman(2014, Era.ERA_I, 2));
            player.takeCharacterCard(new Builder(2015, Era.ERA_I, 1, 2));
            player.takeCharacterCard(new Inventor(2016, Era.ERA_I, "arrow"));
            assertEquals(5, player.getFood()); // still 5, second set not complete
            player.takeCharacterCard(new Artist(7003, Era.ERA_I)); // set 2
            assertEquals(10, player.getFood());
        }

        @Test
        @DisplayName("not retroactive: no bonus for sets present before building")
        void notRetroactive() {
            Player p2 = new Player("Bob", TotemColour.BLUE);
            p2.takeCharacterCard(new Hunter(2017, Era.ERA_I, false));
            p2.takeCharacterCard(new Gatherer(5005, Era.ERA_I));
            p2.takeCharacterCard(new Shaman(2018, Era.ERA_I, 1));
            p2.takeCharacterCard(new Builder(2019, Era.ERA_I, 1, 2));
            p2.takeCharacterCard(new Inventor(2020, Era.ERA_I, "fire"));
            p2.takeCharacterCard(new Artist(7004, Era.ERA_I));
            // set already complete; now buy building 101
            assertTrue(p2.takeBuildingCard(freeBuilding(101)));
            assertEquals(0, p2.getFood()); // no retroactive bonus
        }
    }

    // ── ID 105: inventor pair with same icon -> +3 food ───────────────────────

    @Nested
    @DisplayName("ID 105 – inventor pair bonus")
    class Id105InventorPair {

        private Player player;

        @BeforeEach
        void setUp() {
            player = new Player("Alice", TotemColour.RED);
            assertTrue(player.takeBuildingCard(freeBuilding(105)));
        }

        @Test
        @DisplayName("first inventor grants no food")
        void firstInventorNoFood() {
            player.takeCharacterCard(new Inventor(2021, Era.ERA_I, "fire"));
            assertEquals(0, player.getFood());
        }

        @Test
        @DisplayName("second inventor with different icon grants no food")
        void differentIconNoFood() {
            player.takeCharacterCard(new Inventor(2022, Era.ERA_I, "fire"));
            player.takeCharacterCard(new Inventor(2023, Era.ERA_I, "arrow"));
            assertEquals(0, player.getFood());
        }

        @Test
        @DisplayName("second inventor with same icon grants +3 food")
        void sameIconGrantsThreeFood() {
            player.takeCharacterCard(new Inventor(2024, Era.ERA_I, "fire"));
            player.takeCharacterCard(new Inventor(2025, Era.ERA_I, "fire"));
            assertEquals(3, player.getFood());
        }

        @Test
        @DisplayName("third inventor with same icon grants no additional food")
        void thirdSameIconNoFood() {
            player.takeCharacterCard(new Inventor(2026, Era.ERA_I, "fire"));
            player.takeCharacterCard(new Inventor(2027, Era.ERA_I, "fire")); // +3
            player.takeCharacterCard(new Inventor(2028, Era.ERA_I, "fire")); // odd, no bonus
            assertEquals(3, player.getFood());
        }

        @Test
        @DisplayName("fourth inventor with same icon grants another +3 food")
        void fourthSameIconGrantsAgain() {
            player.takeCharacterCard(new Inventor(2029, Era.ERA_I, "fire"));
            player.takeCharacterCard(new Inventor(2030, Era.ERA_I, "fire")); // +3
            player.takeCharacterCard(new Inventor(2031, Era.ERA_I, "fire"));
            player.takeCharacterCard(new Inventor(2032, Era.ERA_I, "fire")); // +3
            assertEquals(6, player.getFood());
        }

        @Test
        @DisplayName("non-inventor acquisition does not trigger the hook")
        void nonInventorNoTrigger() {
            player.takeCharacterCard(new Hunter(2033, Era.ERA_I, false));
            assertEquals(0, player.getFood());
        }
    }

    // ── ID 301: 25 fixed PP at end of game ───────────────────────────────────

    @Nested
    @DisplayName("ID 301 – 25 fixed end-game PP")
    class Id301FixedPP {

        @Test
        @DisplayName("building 301 contributes 25 PP to final score")
        void fixedPPContributed() {
            Player player = new Player("Alice", TotemColour.RED);
            player.addPrestigePoints(10);
            assertTrue(player.takeBuildingCard(freeBuilding(301)));
            // 10 in-game + 25 endGameScorer + 1 finalPoints = 36
            assertEquals(36, player.calculateFinalScore());
        }
    }

    // ── ID 203: double builder PP at end of game ──────────────────────────────

    @Nested
    @DisplayName("ID 203 – double builder end-game PP")
    class Id203DoubleBuilderPP {

        @Test
        @DisplayName("building 203 doubles the builder PP contribution")
        void doubleBuilderPP() {
            Player player = new Player("Alice", TotemColour.RED);
            player.takeCharacterCard(new Builder(2034, Era.ERA_I, 0, 6)); // 6 PP normally
            assertTrue(player.takeBuildingCard(freeBuilding(203)));
            // builder: 6 PP, building 203: endGameScorer = computeBuilderPoints() = 6,
            // finalPoints = 1 -> total = 6 + 6 + 1 = 13
            assertEquals(13, player.calculateFinalScore());
        }
    }

    // ── overrideEndGameScorer ─────────────────────────────────────────────────

    @Test
    @DisplayName("overrideEndGameScorer replaces EndGameScorer correctly")
    void overrideEndGameScorerWorks() {
        BuildingEffectRegistry.overrideEndGameScorer(206, player -> 42);
        BuildingEffect effect = BuildingEffectRegistry.get(206);
        Player player = new Player("Alice", TotemColour.RED);
        assertEquals(42, effect.getEndGameScorer().compute(player));
    }

    @Test
    @DisplayName("overrideEndGameScorer with unknown ID throws")
    void overrideUnknownIdThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                BuildingEffectRegistry.overrideEndGameScorer(9999, p -> 0));
    }

    // ── NONE ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("unknown building ID returns BuildingEffect.NONE")
    void unknownIdReturnsNone() {
        assertSame(BuildingEffect.NONE, BuildingEffectRegistry.get(9999));
    }
}