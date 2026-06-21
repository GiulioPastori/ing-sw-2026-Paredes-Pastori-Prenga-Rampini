package it.polimi.it.tiles;

import it.polimi.it.enums.TotemColour;
import it.polimi.it.player.Player;
import it.polimi.it.TileResolutionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TileResolutionState.
 *
 * FSM model:
 *   ACTIVE -(consumeTopSlot)-> ACTIVE | EXHAUSTED
 *   ACTIVE -(consumeBottomSlot)-> ACTIVE | EXHAUSTED
 *   EXHAUSTED = both counters at zero
 *
 * Test cases cover counter initialisation, slot consumption, exhaustion
 * detection, guard against over-consumption, and the new isCurrentRowEmpty()
 * method used by ActionValidator to enforce the mandatory-draw rule.
 */
@DisplayName("TileResolutionState")
class TileResolutionStateTest {

    private Player alice;
    private OfferTile tileWithBoth;  // 2 top, 1 bottom
    private OfferTile tileTopOnly;   // 1 top, 0 bottom

    @BeforeEach
    void setUp() {
        alice        = new Player("Alice", TotemColour.RED);
        tileWithBoth = new OfferTile('F', 4, 2, 1, false);
        tileTopOnly  = new OfferTile('B', 2, 1, 0, false);
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    @Test
    @DisplayName("initial slot counts match tile configuration")
    void initialCountsMatchTile() {
        TileResolutionState state = new TileResolutionState(alice, tileWithBoth);
        assertEquals(2, state.remainingTopCards());
        assertEquals(1, state.remainingBottomCards());
        assertEquals(3, state.totalSlotsRemaining());
    }

    @Test
    @DisplayName("isActingPlayer returns true for the correct player")
    void isActingPlayerTrue() {
        TileResolutionState state = new TileResolutionState(alice, tileTopOnly);
        assertTrue(state.isActingPlayer(alice));
    }

    @Test
    @DisplayName("isActingPlayer returns false for a different player")
    void isActingPlayerFalse() {
        Player bob = new Player("Bob", TotemColour.BLUE);
        TileResolutionState state = new TileResolutionState(alice, tileTopOnly);
        assertFalse(state.isActingPlayer(bob));
    }

    // ── Consumption ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("consumeTopSlot decrements top counter")
    void consumeTopSlotDecrements() {
        TileResolutionState state = new TileResolutionState(alice, tileWithBoth);
        state.consumeTopSlot();
        assertEquals(1, state.remainingTopCards());
        assertFalse(state.isExhausted());
    }

    @Test
    @DisplayName("consumeBottomSlot decrements bottom counter")
    void consumeBottomSlotDecrements() {
        TileResolutionState state = new TileResolutionState(alice, tileWithBoth);
        state.consumeBottomSlot();
        assertEquals(0, state.remainingBottomCards());
        assertFalse(state.isExhausted());
    }

    @Test
    @DisplayName("state is exhausted when all slots consumed")
    void exhaustedWhenAllConsumed() {
        TileResolutionState state = new TileResolutionState(alice, tileWithBoth);
        state.consumeTopSlot();
        state.consumeTopSlot();
        state.consumeBottomSlot();
        assertTrue(state.isExhausted());
    }

    @Test
    @DisplayName("state with top-only tile exhausted after one top consumption")
    void topOnlyExhaustedAfterOneConsumption() {
        TileResolutionState state = new TileResolutionState(alice, tileTopOnly);
        state.consumeTopSlot();
        assertTrue(state.isExhausted());
    }

    @Test
    @DisplayName("consumeTopSlot on exhausted state throws")
    void consumeTopOnExhaustedThrows() {
        TileResolutionState state = new TileResolutionState(alice, tileTopOnly);
        state.consumeTopSlot();
        assertThrows(IllegalStateException.class, state::consumeTopSlot);
    }

    @Test
    @DisplayName("consumeBottomSlot when no bottom slots throws")
    void consumeBottomWithZeroSlotsThrows() {
        TileResolutionState state = new TileResolutionState(alice, tileTopOnly); // 0 bottom
        assertThrows(IllegalStateException.class, state::consumeBottomSlot);
    }

    @Test
    @DisplayName("totalSlotsRemaining decreases with each consumption")
    void totalSlotsDecreases() {
        TileResolutionState state = new TileResolutionState(alice, tileWithBoth);
        assertEquals(3, state.totalSlotsRemaining());
        state.consumeTopSlot();
        assertEquals(2, state.totalSlotsRemaining());
        state.consumeBottomSlot();
        assertEquals(1, state.totalSlotsRemaining());
    }

    // ── isCurrentRowEmpty ─────────────────────────────────────────────────────
    // La "fila corrente" è quella superiore finché ha slot; poi quella inferiore.
    // Questo metodo è usato da ActionValidator per decidere se il pass è lecito.

    @Test
    @DisplayName("isCurrentRowEmpty false when top row has cards")
    void currentRowNotEmptyWhenTopHasCards() {
        // tileWithBoth: top=2, bottom=1 → fila corrente = top → non vuota
        TileResolutionState state = new TileResolutionState(alice, tileWithBoth);
        assertFalse(state.isCurrentRowEmpty());
    }

    @Test
    @DisplayName("isCurrentRowEmpty false after consuming one of two top cards")
    void currentRowNotEmptyAfterPartialTopConsumption() {
        TileResolutionState state = new TileResolutionState(alice, tileWithBoth);
        state.consumeTopSlot(); // rimane 1 top
        assertFalse(state.isCurrentRowEmpty());
    }

    @Test
    @DisplayName("isCurrentRowEmpty false when top exhausted but bottom has cards")
    void currentRowNotEmptyWhenBottomHasCards() {
        // tileWithBoth: top=2, bottom=1.
        // Dopo aver consumato entrambi i top, la fila corrente diventa quella inferiore
        // che ha ancora 1 carta → non vuota.
        TileResolutionState state = new TileResolutionState(alice, tileWithBoth);
        state.consumeTopSlot();
        state.consumeTopSlot();
        assertFalse(state.isCurrentRowEmpty());
    }

    @Test
    @DisplayName("isCurrentRowEmpty true after consuming the only top card (no bottom)")
    void currentRowEmptyAfterOnlyTopConsumed() {
        // tileTopOnly: top=1, bottom=0. Dopo aver consumato l'unico top → vuota.
        TileResolutionState state = new TileResolutionState(alice, tileTopOnly);
        state.consumeTopSlot();
        assertTrue(state.isCurrentRowEmpty());
    }

    @Test
    @DisplayName("isCurrentRowEmpty true when tile starts with 0 top and 0 bottom")
    void currentRowEmptyFromStart() {
        // Tessera totalmente esaurita (es. tutte le carte già prese da altri giocatori).
        OfferTile emptyTile = new OfferTile('A', 1, 0, 0, false);
        TileResolutionState state = new TileResolutionState(alice, emptyTile);
        assertTrue(state.isCurrentRowEmpty());
    }

    @Test
    @DisplayName("isCurrentRowEmpty true after consuming all top and bottom cards")
    void currentRowEmptyAfterAllConsumed() {
        TileResolutionState state = new TileResolutionState(alice, tileWithBoth);
        state.consumeTopSlot();
        state.consumeTopSlot();
        state.consumeBottomSlot();
        assertTrue(state.isCurrentRowEmpty());
        assertTrue(state.isExhausted()); // isCurrentRowEmpty e isExhausted coincidono qui
    }

    @Test
    @DisplayName("isCurrentRowEmpty false when tile has only bottom cards (top=0, bottom=1)")
    void currentRowNotEmptyWhenOnlyBottomHasCards() {
        // Tessera C: top=0, bottom=1. La fila corrente è subito quella inferiore → non vuota.
        OfferTile bottomOnly = new OfferTile('C', 2, 0, 1, false);
        TileResolutionState state = new TileResolutionState(alice, bottomOnly);
        assertFalse(state.isCurrentRowEmpty());
    }

    @Test
    @DisplayName("isCurrentRowEmpty true after consuming the only bottom card (no top)")
    void currentRowEmptyAfterOnlyBottomConsumed() {
        OfferTile bottomOnly = new OfferTile('C', 2, 0, 1, false);
        TileResolutionState state = new TileResolutionState(alice, bottomOnly);
        state.consumeBottomSlot();
        assertTrue(state.isCurrentRowEmpty());
    }
}