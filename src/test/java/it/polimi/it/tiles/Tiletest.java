package it.polimi.it.tiles;

import it.polimi.it.enums.TotemColour;
import it.polimi.it.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// ── OfferTile ─────────────────────────────────────────────────────────────────

@DisplayName("OfferTile")
class OfferTileTest {

    private OfferTile tile;

    @BeforeEach
    void setUp() {
        tile = new OfferTile('B', 2, 1, 0, false);
    }

    @Test
    @DisplayName("new tile is free")
    void newTileIsFree() {
        assertTrue(tile.isFree());
    }

    @Test
    @DisplayName("placeTotem makes tile occupied")
    void placeTotemMakesOccupied() {
        tile.placeTotem(TotemColour.RED);
        assertFalse(tile.isFree());
        assertEquals(TotemColour.RED, tile.getPlacedTotem());
    }

    @Test
    @DisplayName("placeTotem on occupied tile throws IllegalStateException")
    void placeTotemOnOccupiedThrows() {
        tile.placeTotem(TotemColour.RED);
        assertThrows(IllegalStateException.class,
                () -> tile.placeTotem(TotemColour.BLUE));
    }

    @Test
    @DisplayName("removeTotem frees the tile and returns the colour")
    void removeTotemFreesTile() {
        tile.placeTotem(TotemColour.RED);
        TotemColour removed = tile.removeTotem();
        assertEquals(TotemColour.RED, removed);
        assertTrue(tile.isFree());
    }

    @Test
    @DisplayName("removeTotem on free tile throws IllegalStateException")
    void removeTotemOnFreeThrows() {
        assertThrows(IllegalStateException.class, tile::removeTotem);
    }

    @Test
    @DisplayName("getters return correct values")
    void gettersReturnCorrectValues() {
        assertEquals('B', tile.getLetter());
        assertEquals(2,   tile.getMinPlayers());
        assertEquals(1,   tile.getTopRowCards());
        assertEquals(0,   tile.getBottomRowCards());
        assertFalse(tile.isFoodOnly());
    }

    @Test
    @DisplayName("foodOnly tile returns true for isFoodOnly")
    void foodOnlyTile() {
        OfferTile foodTile = new OfferTile('A', 5, 0, 0, true);
        assertTrue(foodTile.isFoodOnly());
    }
}

// ── OfferTiles ────────────────────────────────────────────────────────────────

@DisplayName("OfferTiles")
class OfferTilesTest {

    private Player     alice;
    private Player     bob;
    private OfferTile  tileB;
    private OfferTile  tileC;
    private OfferTiles offerTiles;

    @BeforeEach
    void setUp() {
        alice      = new Player("Alice", TotemColour.RED);
        bob        = new Player("Bob",   TotemColour.BLUE);
        tileB      = new OfferTile('B', 2, 1, 0, false);
        tileC      = new OfferTile('C', 2, 0, 1, false);
        offerTiles = new OfferTiles(List.of(tileB, tileC));
    }

    @Test
    @DisplayName("all tiles are free initially")
    void allTilesFreeInitially() {
        assertEquals(2, offerTiles.getFreeTiles().size());
        assertTrue(offerTiles.allFree());
    }

    @Test
    @DisplayName("placeTotem removes tile from free list")
    void placeTotemRemovesFromFree() {
        offerTiles.placeTotem(alice, tileB);
        List<OfferTile> free = offerTiles.getFreeTiles();
        assertEquals(1, free.size());
        assertFalse(free.contains(tileB));
    }

    @Test
    @DisplayName("getTileByTotem returns correct tile")
    void getTileByTotemReturnsCorrectTile() {
        offerTiles.placeTotem(alice, tileB);
        assertEquals(tileB, offerTiles.getTileByTotem(TotemColour.RED));
    }

    @Test
    @DisplayName("getTileByTotem throws when totem not placed")
    void getTileByTotemThrowsWhenNotPlaced() {
        assertThrows(IllegalStateException.class,
                () -> offerTiles.getTileByTotem(TotemColour.RED));
    }

    @Test
    @DisplayName("getAllTiles returns all tiles in order")
    void getAllTilesReturnsAll() {
        List<OfferTile> all = offerTiles.getAllTiles();
        assertEquals(2, all.size());
        assertEquals('B', all.get(0).getLetter());
        assertEquals('C', all.get(1).getLetter());
    }

    @Test
    @DisplayName("placeTotem on tile not in track throws IllegalArgumentException")
    void placeTotemOnForeignTileThrows() {
        OfferTile foreign = new OfferTile('G', 5, 2, 3, false);
        assertThrows(IllegalArgumentException.class,
                () -> offerTiles.placeTotem(alice, foreign));
    }

    @Test
    @DisplayName("allFree returns false when any tile is occupied")
    void allFreeReturnsFalseWhenOccupied() {
        offerTiles.placeTotem(alice, tileB);
        assertFalse(offerTiles.allFree());
    }
}

// ── TurnPosition ──────────────────────────────────────────────────────────────

@DisplayName("TurnPosition")
class TurnPositionTest {

    @Test
    @DisplayName("new position is free")
    void newPositionIsFree() {
        TurnPosition pos = new TurnPosition(1, 3, false);
        assertTrue(pos.isFree());
    }

    @Test
    @DisplayName("place sets the totem and makes position occupied")
    void placeSetTotem() {
        TurnPosition pos = new TurnPosition(1, 3, false);
        pos.place(TotemColour.RED);
        assertFalse(pos.isFree());
        assertEquals(TotemColour.RED, pos.getPlacedTotem());
    }

    @Test
    @DisplayName("place on occupied position throws IllegalStateException")
    void placeOnOccupiedThrows() {
        TurnPosition pos = new TurnPosition(1, 3, false);
        pos.place(TotemColour.RED);
        assertThrows(IllegalStateException.class, () -> pos.place(TotemColour.BLUE));
    }

    @Test
    @DisplayName("free removes totem and returns it")
    void freeRemovesTotem() {
        TurnPosition pos = new TurnPosition(1, 3, false);
        pos.place(TotemColour.RED);
        TotemColour removed = pos.free();
        assertEquals(TotemColour.RED, removed);
        assertTrue(pos.isFree());
    }

    @Test
    @DisplayName("free on free position throws IllegalStateException")
    void freeOnFreeThrows() {
        TurnPosition pos = new TurnPosition(1, 3, false);
        assertThrows(IllegalStateException.class, pos::free);
    }

    @Test
    @DisplayName("getters return correct values")
    void gettersReturnCorrectValues() {
        TurnPosition pos = new TurnPosition(3, 0, true);
        assertEquals(3, pos.getPosition());
        assertEquals(0, pos.getFoodBonus());
        assertTrue(pos.isLastPosition());
    }
}

// ── TurnOrderTile ─────────────────────────────────────────────────────────────

@DisplayName("TurnOrderTile")
class TurnOrderTileTest {

    private Player alice;
    private Player bob;
    private Player charlie;

    @BeforeEach
    void setUp() {
        alice   = new Player("Alice",   TotemColour.RED);
        bob     = new Player("Bob",     TotemColour.BLUE);
        charlie = new Player("Charlie", TotemColour.YELLOW);
    }

    @Test
    @DisplayName("first player placed gets +3 food")
    void firstPlaceGetsThreeFood() {
        TurnOrderTile tile = new TurnOrderTile(2);
        tile.placeTotem(alice);
        assertEquals(3, alice.getFood());
        assertEquals(1, alice.getTurnOrder());
    }

    @Test
    @DisplayName("second player in a 3-player game gets +1 food")
    void secondPlaceGetsOneFood() {
        TurnOrderTile tile = new TurnOrderTile(3);
        tile.placeTotem(alice); // first:  +3
        tile.placeTotem(bob);   // second: +1
        assertEquals(1, bob.getFood());
        assertEquals(2, bob.getTurnOrder());
    }

    @Test
    @DisplayName("last player (3P) pays 1 food if available")
    void lastPlacePaysOneFood() {
        TurnOrderTile tile = new TurnOrderTile(3);
        charlie.addFood(5);
        tile.placeTotem(alice);   // first:  +3
        tile.placeTotem(bob);     // second: +1
        tile.placeTotem(charlie); // last:   -1 food → 5-1=4
        assertEquals(4, charlie.getFood());
    }

    @Test
    @DisplayName("last player loses 2 PP if no food")
    void lastPlaceLosesTwoPPWhenNoFood() {
        TurnOrderTile tile = new TurnOrderTile(3);
        tile.placeTotem(alice);   // first
        tile.placeTotem(bob);     // second
        tile.placeTotem(charlie); // last, charlie has 0 food → -2 PP
        assertEquals(-2, charlie.getPrestigePoints());
    }

    @Test
    @DisplayName("reset clears all positions")
    void resetClearsAllPositions() {
        TurnOrderTile tile = new TurnOrderTile(2);
        tile.placeTotem(alice);
        tile.placeTotem(bob);
        tile.reset();
        assertDoesNotThrow(() -> tile.placeTotem(alice));
    }

    @Test
    @DisplayName("getTotemOrder returns colours in placement order")
    void getTotemOrderReturnsInOrder() {
        TurnOrderTile tile = new TurnOrderTile(2);
        tile.placeTotem(alice);
        tile.placeTotem(bob);
        List<TotemColour> order = tile.getTotemOrder();
        assertEquals(TotemColour.RED,  order.get(0));
        assertEquals(TotemColour.BLUE, order.get(1));
    }

    @Test
    @DisplayName("placeTotem when all spaces full throws IllegalStateException")
    void placeTotemWhenFullThrows() {
        TurnOrderTile tile = new TurnOrderTile(2);
        tile.placeTotem(alice);
        tile.placeTotem(bob);
        assertThrows(IllegalStateException.class, () -> tile.placeTotem(charlie));
    }
}
