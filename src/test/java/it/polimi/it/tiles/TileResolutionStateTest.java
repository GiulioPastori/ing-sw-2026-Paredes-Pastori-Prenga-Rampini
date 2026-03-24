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
 * detection, and guard against over-consumption.
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
}