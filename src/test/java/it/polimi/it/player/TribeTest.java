package it.polimi.it.player;

import it.polimi.it.cards.character.*;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Tribe.
 *
 * FSM model:
 *   EMPTY -(addCharacter)-> HAS_CHARACTERS -(addCharacter)-> HAS_CHARACTERS
 *   All scoring methods are observers: tested after a sequence of addCharacter calls.
 *
 * Test cases cover:
 * - countByType on empty tribe and after adding various types
 * - totalShamanIcons accumulation
 * - countUniqueInventorIcons deduplication
 * - totalBuildingDiscount stacking
 * - computeBuilderPoints, computeInventorPoints, computeArtistPoints
 * - unmodifiable view invariant
 */
@DisplayName("Tribe")
class TribeTest {

    private Tribe tribe;

    @BeforeEach
    void setUp() {
        tribe = new Tribe();
    }

    // Empty state

    @Test
    @DisplayName("empty tribe has zero characters")
    void emptyTribeHasZeroCharacters() {
        assertEquals(0, tribe.getCharacterCount());
    }

    @Test
    @DisplayName("countByType returns zero for all types on empty tribe")
    void countByTypeEmptyTribe() {
        for (CharacterType type : CharacterType.values()) {
            assertEquals(0, tribe.countByType(type));
        }
    }

    @Test
    @DisplayName("totalShamanIcons returns zero on empty tribe")
    void totalShamanIconsEmpty() {
        assertEquals(0, tribe.totalShamanIcons());
    }

    @Test
    @DisplayName("computeInventorPoints returns zero on empty tribe")
    void inventorPointsEmpty() {
        assertEquals(0, tribe.computeInventorPoints());
    }

    @Test
    @DisplayName("computeArtistPoints returns zero on empty tribe")
    void artistPointsEmpty() {
        assertEquals(0, tribe.computeArtistPoints());
    }

    // countByType

    @Test
    @DisplayName("countByType returns correct count after adding characters")
    void countByTypeAfterAdding() {
        tribe.addCharacter(new Hunter(2001, Era.ERA_I, false));
        tribe.addCharacter(new Hunter(2002, Era.ERA_I, false));
        tribe.addCharacter(new Gatherer(5001, Era.ERA_I));

        assertEquals(2, tribe.countByType(CharacterType.HUNTER));
        assertEquals(1, tribe.countByType(CharacterType.GATHERER));
        assertEquals(0, tribe.countByType(CharacterType.SHAMAN));
    }

    // Shaman icons

    @Test
    @DisplayName("totalShamanIcons sums all star icons")
    void totalShamanIconsSumsAll() {
        tribe.addCharacter(new Shaman(2003, Era.ERA_I, 1));
        tribe.addCharacter(new Shaman(2004, Era.ERA_I, 3));
        tribe.addCharacter(new Shaman(2005, Era.ERA_II, 2));
        assertEquals(6, tribe.totalShamanIcons());
    }

    @Test
    @DisplayName("non-shaman characters do not contribute to shaman icon count")
    void nonShamanCharactersDoNotContribute() {
        tribe.addCharacter(new Hunter(2006, Era.ERA_I, false));
        tribe.addCharacter(new Gatherer(5002, Era.ERA_I));
        assertEquals(0, tribe.totalShamanIcons());
    }

    // Unique inventor icons

    @Test
    @DisplayName("countUniqueInventorIcons counts distinct icons")
    void uniqueInventorIconsDistinct() {
        tribe.addCharacter(new Inventor(2007, Era.ERA_I, "fire"));
        tribe.addCharacter(new Inventor(2008, Era.ERA_I, "arrow"));
        tribe.addCharacter(new Inventor(2009, Era.ERA_I, "fire")); // duplicate
        assertEquals(2, tribe.countUniqueInventorIcons());
    }

    @Test
    @DisplayName("countUniqueInventorIcons returns zero when no inventors")
    void uniqueInventorIconsNoInventors() {
        tribe.addCharacter(new Hunter(2010, Era.ERA_I, false));
        assertEquals(0, tribe.countUniqueInventorIcons());
    }

    // Building discount

    @Test
    @DisplayName("totalBuildingDiscount sums all builder discounts")
    void totalBuildingDiscountSums() {
        tribe.addCharacter(new Builder(2011, Era.ERA_I, 1, 3));
        tribe.addCharacter(new Builder(2012, Era.ERA_I, 2, 3));
        assertEquals(3, tribe.totalBuildingDiscount());
    }

    @Test
    @DisplayName("totalBuildingDiscount returns zero when no builders")
    void totalBuildingDiscountNoBuilders() {
        tribe.addCharacter(new Hunter(2013, Era.ERA_I, false));
        assertEquals(0, tribe.totalBuildingDiscount());
    }

    // End-game scoring

    @Test
    @DisplayName("computeBuilderPoints sums all builder end-game points")
    void computeBuilderPointsSums() {
        tribe.addCharacter(new Builder(2014, Era.ERA_I, 1, 3));
        tribe.addCharacter(new Builder(2015, Era.ERA_I, 1, 5));
        assertEquals(8, tribe.computeBuilderPoints());
    }

    @Test
    @DisplayName("computeInventorPoints = inventor count x distinct icon count")
    void computeInventorPointsFormula() {
        tribe.addCharacter(new Inventor(2016, Era.ERA_I, "fire"));
        tribe.addCharacter(new Inventor(2017, Era.ERA_I, "arrow"));
        tribe.addCharacter(new Inventor(2018, Era.ERA_I, "wheel"));
        // 3 inventors x 3 distinct icons = 9
        assertEquals(9, tribe.computeInventorPoints());
    }

    @Test
    @DisplayName("computeInventorPoints with duplicate icons reduces distinct count")
    void computeInventorPointsDuplicateIcons() {
        tribe.addCharacter(new Inventor(2019, Era.ERA_I, "fire"));
        tribe.addCharacter(new Inventor(2020, Era.ERA_I, "fire"));
        // 2 inventors x 1 distinct icon = 2
        assertEquals(2, tribe.computeInventorPoints());
    }

    @Test
    @DisplayName("computeArtistPoints is 10 per pair (integer division)")
    void computeArtistPointsPerPair() {
        tribe.addCharacter(new Artist(7005, Era.ERA_I));
        tribe.addCharacter(new Artist(7006, Era.ERA_I));
        tribe.addCharacter(new Artist(7007, Era.ERA_I)); // odd one out
        // 3 / 2 = 1 pair -> 10 PP
        assertEquals(10, tribe.computeArtistPoints());
    }

    @Test
    @DisplayName("computeArtistPoints with one artist returns zero")
    void computeArtistPointsOneArtist() {
        tribe.addCharacter(new Artist(7008, Era.ERA_I));
        assertEquals(0, tribe.computeArtistPoints());
    }

    @Test
    @DisplayName("computeArtistPoints with four artists returns 20")
    void computeArtistPointsFourArtists() {
        for (int i = 0; i < 4; i++) tribe.addCharacter(new Artist(7100 + i, Era.ERA_I));
        assertEquals(20, tribe.computeArtistPoints());
    }

    // Unmodifiable views

    @Test
    @DisplayName("getCharacters returns unmodifiable view")
    void getCharactersIsUnmodifiable() {
        tribe.addCharacter(new Hunter(2021, Era.ERA_I, false));
        assertThrows(UnsupportedOperationException.class,
                () -> tribe.getCharacters().add(new Hunter(2022, Era.ERA_I, false)));
    }
}