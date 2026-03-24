package it.polimi.it.validation;

import it.polimi.it.TileResolutionState;
import it.polimi.it.actions.*;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.player.Player;
import it.polimi.it.tiles.OfferTile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// ── ValidationResult ──────────────────────────────────────────────────────────

/**
 * Tests ValidationResult sealed subtypes and factory methods.
 */
@DisplayName("ValidationResult")
class ValidationResultTest {

    @Test
    @DisplayName("ok() returns Ok")
    void okIsOk() { assertTrue(ValidationResult.ok().isOk()); }

    @Test
    @DisplayName("fail() returns Fail")
    void failIsNotOk() { assertFalse(ValidationResult.fail("reason").isOk()); }

    @Test
    @DisplayName("Fail carries its reason")
    void failCarriesReason() {
        assertEquals("bad input",
                ((ValidationResult.Fail) ValidationResult.fail("bad input")).reason());
    }

    @Test
    @DisplayName("Ok instances are equal")
    void okInstancesAreEqual() { assertEquals(ValidationResult.ok(), ValidationResult.ok()); }
}

// ── ActionValidator ───────────────────────────────────────────────────────────

/**
 * Tests every validation branch for each action type.
 * Actions now carry string identifiers (player name, card name, tile letter),
 * matching the network-message format.
 */
@DisplayName("ActionValidator")
class ActionValidatorTest {

    private Player alice;
    private Player bob;

    @BeforeEach
    void setUp() {
        alice = new Player("Alice", TotemColour.RED);
        bob   = new Player("Bob", TotemColour.BLUE);
    }

    @Nested
    @DisplayName("PlaceTotemAction")
    class PlaceTotemValidation {

        private OfferTile freeB;
        private OfferTile freeC;

        @BeforeEach
        void setUpTiles() {
            freeB = new OfferTile('B', 2, 1, 0, false);
            freeC = new OfferTile('C', 2, 0, 1, false);
        }

        @Test
        @DisplayName("tile letter present in free list returns Ok")
        void validTileReturnsOk() {
            assertTrue(ActionValidator.validate(
                    new PlaceTotemAction("Alice", 'B'), alice, List.of(freeB, freeC)).isOk());
        }

        @Test
        @DisplayName("tile letter not in free list returns Fail")
        void tileNotInFreeListReturnsFail() {
            assertFalse(ActionValidator.validate(
                    new PlaceTotemAction("Alice", 'D'), alice, List.of(freeB, freeC)).isOk());
        }
    }

    @Nested
    @DisplayName("TakeCardAction")
    class TakeCardValidation {

        private TileResolutionState stateForAlice;

        @BeforeEach
        void setUpState() {
            OfferTile tile = new OfferTile('D', 3, 1, 1, false);
            stateForAlice = new TileResolutionState(alice, tile);
        }

        @Test
        @DisplayName("acting player in top row returns Ok")
        void validTopRowReturnsOk() {
            assertTrue(ActionValidator.validate(
                    new TakeCardAction("Alice", "Hunter1", true), alice, stateForAlice).isOk());
        }

        @Test
        @DisplayName("wrong player returns Fail")
        void wrongPlayerReturnsFail() {
            assertFalse(ActionValidator.validate(
                    new TakeCardAction("Alice", "Hunter1", true), bob, stateForAlice).isOk());
        }

        @Test
        @DisplayName("top slot exhausted returns Fail")
        void topSlotExhaustedReturnsFail() {
            stateForAlice.consumeTopSlot();
            assertFalse(ActionValidator.validate(
                    new TakeCardAction("Alice", "Hunter1", true), alice, stateForAlice).isOk());
        }

        @Test
        @DisplayName("bottom slot exhausted returns Fail")
        void bottomSlotExhaustedReturnsFail() {
            stateForAlice.consumeBottomSlot();
            assertFalse(ActionValidator.validate(
                    new TakeCardAction("Alice", "Hunter1", false), alice, stateForAlice).isOk());
        }
    }

    @Nested
    @DisplayName("PassAction")
    class PassValidation {

        @Test
        @DisplayName("acting player passing returns Ok")
        void actingPlayerPassReturnsOk() {
            var state = new TileResolutionState(alice, new OfferTile('B', 2, 1, 0, false));
            assertTrue(ActionValidator.validate(new PassAction("Alice"), alice, state).isOk());
        }

        @Test
        @DisplayName("wrong player passing returns Fail")
        void wrongPlayerPassReturnsFail() {
            var state = new TileResolutionState(alice, new OfferTile('B', 2, 1, 0, false));
            assertFalse(ActionValidator.validate(new PassAction("Alice"), bob, state).isOk());
        }
    }

    @Nested
    @DisplayName("PostRoundAction")
    class PostRoundValidation {

        @Test
        @DisplayName("player in pending list returns Ok")
        void validPostRoundReturnsOk() {
            assertTrue(ActionValidator.validate(
                    new PostRoundAction("Alice", "Building1"), alice, List.of(alice)).isOk());
        }

        @Test
        @DisplayName("player not in pending list returns Fail")
        void notInPendingListReturnsFail() {
            assertFalse(ActionValidator.validate(
                    new PostRoundAction("Alice", "Building1"), alice, List.of(bob)).isOk());
        }

        @Test
        @DisplayName("null card name (decline) returns Ok")
        void nullCardDeclineReturnsOk() {
            assertTrue(ActionValidator.validate(
                    new PostRoundAction("Alice", null), alice, List.of(alice)).isOk());
        }
    }
}