package it.polimi.it.cards.character;

import it.polimi.it.enums.Era;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.player.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// ── Gatherer ──────────────────────────────────────────────────────────────────

@DisplayName("Gatherer")
class GathererTest {

    @Test
    @DisplayName("FOOD_DISCOUNT_PER_GATHERER is 3")
    void discountConstantIsThree() {
        assertEquals(3, Gatherer.FOOD_DISCOUNT_PER_GATHERER);
    }

    @Test
    @DisplayName("applyImmediateEffect does not change player food or PP")
    void applyImmediateEffectIsNoOp() {
        Player player = new Player("Alice", TotemColour.RED);
        player.addFood(5);
        player.addPrestigePoints(3);

        new Gatherer(5001, Era.ERA_I).applyImmediateEffect(player);

        assertEquals(5, player.getFood());
        assertEquals(3, player.getPrestigePoints());
    }

    @Test
    @DisplayName("gatherer is counted as GATHERER type")
    void gathererTypeIsCorrect() {
        Gatherer g = new Gatherer(5001, Era.ERA_I);
        assertEquals(it.polimi.it.enums.CharacterType.GATHERER, g.getType());
    }
}

// ── Artist ────────────────────────────────────────────────────────────────────

@DisplayName("Artist")
class ArtistTest {

    @Test
    @DisplayName("applyImmediateEffect does not change player food or PP")
    void applyImmediateEffectIsNoOp() {
        Player player = new Player("Alice", TotemColour.RED);
        player.addFood(5);
        player.addPrestigePoints(3);

        new Artist(7001, Era.ERA_I).applyImmediateEffect(player);

        assertEquals(5, player.getFood());
        assertEquals(3, player.getPrestigePoints());
    }

    @Test
    @DisplayName("artist is counted as ARTIST type")
    void artistTypeIsCorrect() {
        assertEquals(it.polimi.it.enums.CharacterType.ARTIST,
                new Artist(7001, Era.ERA_I).getType());
    }

    @Test
    @DisplayName("two artists contribute 10 PP to final score")
    void twoArtistsContributeTenPP() {
        Player player = new Player("Alice", TotemColour.RED);
        player.takeCharacterCard(new Artist(7002, Era.ERA_I));
        player.takeCharacterCard(new Artist(7003, Era.ERA_I));
        assertEquals(10, player.calculateFinalScore());
    }

    @Test
    @DisplayName("one artist contributes 0 PP to final score")
    void oneArtistContributesZeroPP() {
        Player player = new Player("Alice", TotemColour.RED);
        player.takeCharacterCard(new Artist(7004, Era.ERA_I));
        assertEquals(0, player.calculateFinalScore());
    }
}

// ── Inventor ──────────────────────────────────────────────────────────────────

@DisplayName("Inventor")
class InventorTest {

    @Test
    @DisplayName("getInventionIcon returns the icon provided at construction")
    void getInventionIconReturnsCorrectValue() {
        assertEquals("fire", new Inventor(2001, Era.ERA_I, "fire").getInventionIcon());
    }

    @Test
    @DisplayName("null invention icon throws IllegalArgumentException")
    void nullIconThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new Inventor(2002, Era.ERA_I, null));
    }

    @Test
    @DisplayName("blank invention icon throws IllegalArgumentException")
    void blankIconThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new Inventor(2003, Era.ERA_I, "  "));
    }

    @Test
    @DisplayName("applyImmediateEffect does not change player food or PP")
    void applyImmediateEffectIsNoOp() {
        Player player = new Player("Alice", TotemColour.RED);
        player.addFood(5);
        player.addPrestigePoints(3);

        new Inventor(2004, Era.ERA_I, "fire").applyImmediateEffect(player);

        assertEquals(5, player.getFood());
        assertEquals(3, player.getPrestigePoints());
    }

    @Test
    @DisplayName("inventor is counted as INVENTOR type")
    void inventorTypeIsCorrect() {
        assertEquals(it.polimi.it.enums.CharacterType.INVENTOR,
                new Inventor(2005, Era.ERA_I, "arrow").getType());
    }
}

// ── Builder ───────────────────────────────────────────────────────────────────

@DisplayName("Builder")
class BuilderCardTest {

    @Test
    @DisplayName("getBuildingDiscount returns the value provided at construction")
    void getBuildingDiscount() {
        assertEquals(2, new Builder(2001, Era.ERA_I, 2, 3).getBuildingDiscount());
    }

    @Test
    @DisplayName("getEndGamePoints returns the value provided at construction")
    void getEndGamePoints() {
        assertEquals(3, new Builder(2001, Era.ERA_I, 2, 3).getEndGamePoints());
    }

    @Test
    @DisplayName("applyImmediateEffect does not change player food or PP")
    void applyImmediateEffectIsNoOp() {
        Player player = new Player("Alice", TotemColour.RED);
        player.addFood(5);
        player.addPrestigePoints(3);

        new Builder(2002, Era.ERA_I, 1, 4).applyImmediateEffect(player);

        assertEquals(5, player.getFood());
        assertEquals(3, player.getPrestigePoints());
    }

    @Test
    @DisplayName("builder is counted as BUILDER type")
    void builderTypeIsCorrect() {
        assertEquals(it.polimi.it.enums.CharacterType.BUILDER,
                new Builder(2003, Era.ERA_I, 1, 3).getType());
    }

    @Test
    @DisplayName("builder end-game points contribute to final score")
    void builderContributesToFinalScore() {
        Player player = new Player("Alice", TotemColour.RED);
        player.takeCharacterCard(new Builder(2004, Era.ERA_I, 1, 5));
        assertEquals(5, player.calculateFinalScore());
    }
}