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
                    new TakeCardAction("Alice", 1, true), alice, stateForAlice).isOk());
        }

        @Test
        @DisplayName("wrong player returns Fail")
        void wrongPlayerReturnsFail() {
            assertFalse(ActionValidator.validate(
                    new TakeCardAction("Alice", 1, true), bob, stateForAlice).isOk());
        }

        @Test
        @DisplayName("top slot exhausted returns Fail")
        void topSlotExhaustedReturnsFail() {
            stateForAlice.consumeTopSlot();
            assertFalse(ActionValidator.validate(
                    new TakeCardAction("Alice", 1, true), alice, stateForAlice).isOk());
        }

        @Test
        @DisplayName("bottom slot exhausted returns Fail")
        void bottomSlotExhaustedReturnsFail() {
            stateForAlice.consumeBottomSlot();
            assertFalse(ActionValidator.validate(
                    new TakeCardAction("Alice", 1, false), alice, stateForAlice).isOk());
        }
    }

    @Nested
    @DisplayName("PassAction")
    class PassValidation {

        // ── Fila superiore vuota (top=0, bottom=0) ────────────────────────────

        @Test
        @DisplayName("pass with empty top row (tile B: top=1,bottom=0) after consuming is Ok")
        void passAfterConsumingTopOnlyIsOk() {
            // Tessera B: 1 carta top, 0 bottom.
            // Dopo aver consumato l'unico slot, la fila corrente è vuota → pass lecito.
            OfferTile tile = new OfferTile('B', 2, 1, 0, false);
            TileResolutionState state = new TileResolutionState(alice, tile);
            state.consumeTopSlot(); // svuota la fila top (la sola presente)
            assertTrue(state.isCurrentRowEmpty());
            assertTrue(ActionValidator.validate(new PassAction("Alice"), alice, state).isOk());
        }

        @Test
        @DisplayName("pass with empty bottom row (tile C: top=0,bottom=1) after consuming is Ok")
        void passAfterConsumingBottomOnlyIsOk() {
            // Tessera C: 0 carte top, 1 bottom.
            // La fila corrente è subito quella inferiore; dopo averla consumata → pass lecito.
            OfferTile tile = new OfferTile('C', 2, 0, 1, false);
            TileResolutionState state = new TileResolutionState(alice, tile);
            state.consumeBottomSlot();
            assertTrue(state.isCurrentRowEmpty());
            assertTrue(ActionValidator.validate(new PassAction("Alice"), alice, state).isOk());
        }

        @Test
        @DisplayName("pass when row is empty from the start (tile with 0 top, 0 bottom) is Ok")
        void passOnTotallyEmptyTileIsOk() {
            // Tessera con fila completamente vuota fin dall'inizio (es. row esaurita dal board).
            OfferTile tile = new OfferTile('A', 1, 0, 0, false);
            TileResolutionState state = new TileResolutionState(alice, tile);
            assertTrue(state.isCurrentRowEmpty());
            assertTrue(ActionValidator.validate(new PassAction("Alice"), alice, state).isOk());
        }

        // ── Fila superiore NON vuota → pass vietato ───────────────────────────

        @Test
        @DisplayName("pass with cards still in top row returns Fail")
        void passWithCardsInTopRowReturnsFail() {
            // Tessera B: 1 carta top ancora disponibile → non si può passare.
            OfferTile tile = new OfferTile('B', 2, 1, 0, false);
            TileResolutionState state = new TileResolutionState(alice, tile);
            assertFalse(state.isCurrentRowEmpty());
            assertFalse(ActionValidator.validate(new PassAction("Alice"), alice, state).isOk());
        }

        @Test
        @DisplayName("pass with cards still in bottom row returns Fail")
        void passWithCardsInBottomRowReturnsFail() {
            // Tessera con solo bottom (top=0, bottom=1): carta disponibile → pass vietato.
            OfferTile tile = new OfferTile('C', 2, 0, 1, false);
            TileResolutionState state = new TileResolutionState(alice, tile);
            assertFalse(state.isCurrentRowEmpty());
            assertFalse(ActionValidator.validate(new PassAction("Alice"), alice, state).isOk());
        }

        @Test
        @DisplayName("pass with multiple cards in top row returns Fail")
        void passWithMultipleCardsInTopRowReturnsFail() {
            // Tessera F: 2 top, 1 bottom. Nessuno slot consumato → pass vietato.
            OfferTile tile = new OfferTile('F', 4, 2, 1, false);
            TileResolutionState state = new TileResolutionState(alice, tile);
            assertFalse(ActionValidator.validate(new PassAction("Alice"), alice, state).isOk());
        }

        @Test
        @DisplayName("pass after consuming only one of two top cards returns Fail")
        void passAfterPartialTopConsumptionReturnsFail() {
            // Tessera F: 2 top, 1 bottom. Consumo 1 top → rimane 1 → pass ancora vietato.
            OfferTile tile = new OfferTile('F', 4, 2, 1, false);
            TileResolutionState state = new TileResolutionState(alice, tile);
            state.consumeTopSlot(); // rimane 1 top
            assertFalse(state.isCurrentRowEmpty());
            assertFalse(ActionValidator.validate(new PassAction("Alice"), alice, state).isOk());
        }

        // ── Giocatore errato ──────────────────────────────────────────────────

        @Test
        @DisplayName("wrong player passing on empty row returns Fail")
        void wrongPlayerPassReturnsFail() {
            // Anche se la fila è vuota, il giocatore sbagliato non può agire.
            OfferTile tile = new OfferTile('B', 2, 1, 0, false);
            TileResolutionState state = new TileResolutionState(alice, tile);
            state.consumeTopSlot();
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
                    new PostRoundAction("Alice", 101), alice, List.of(alice)).isOk());
        }

        @Test
        @DisplayName("player not in pending list returns Fail")
        void notInPendingListReturnsFail() {
            assertFalse(ActionValidator.validate(
                    new PostRoundAction("Alice", 101), alice, List.of(bob)).isOk());
        }

        @Test
        @DisplayName("null card name (decline) returns Ok")
        void nullCardDeclineReturnsOk() {
            assertTrue(ActionValidator.validate(
                    new PostRoundAction("Alice", 0), alice, List.of(alice)).isOk());
        }
    }
}