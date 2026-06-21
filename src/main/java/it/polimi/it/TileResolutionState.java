package it.polimi.it;

import it.polimi.it.player.Player;
import it.polimi.it.tiles.OfferTile;

/**
 * Resolution state for a single offer tile during Phase 2 of a round.
 *
 * Encapsulates the remaining slot counters (top and bottom row) and the
 * active player, keeping this temporary state out of Game's instance fields.
 *
 * Game creates a TileResolutionState when it starts processing a tile and
 * discards it when all slots have been consumed. While it exists, it
 * represents the current player's "turn" on that tile.
 */
public final class TileResolutionState {

    private final Player    actingPlayer;
    private final OfferTile tile;
    private int remainingTopCards;
    private int remainingBottomCards;

    public TileResolutionState(Player actingPlayer, OfferTile tile) {
        this.actingPlayer         = actingPlayer;
        this.tile                 = tile;
        this.remainingTopCards    = tile.getTopRowCards();
        this.remainingBottomCards = tile.getBottomRowCards();
    }

    /** Returns true if the given player is the active player on this tile. */
    public boolean isActingPlayer(Player player) {
        return actingPlayer.equals(player);
    }

    /**
     * Consumes one top-row slot.
     *
     * @throws IllegalStateException if no top-row slots remain
     */
    public void consumeTopSlot() {
        if (remainingTopCards <= 0) throw new IllegalStateException(
                "No top-row slots left for tile " + tile.getLetter());
        remainingTopCards--;
    }

    /**
     * Consumes one bottom-row slot.
     *
     * @throws IllegalStateException if no bottom-row slots remain
     */
    public void consumeBottomSlot() {
        if (remainingBottomCards <= 0) throw new IllegalStateException(
                "No bottom-row slots left for tile " + tile.getLetter());
        remainingBottomCards--;
    }

    /** Returns true if all slots (top and bottom) have been consumed. */
    public boolean isExhausted() {
        return remainingTopCards == 0 && remainingBottomCards == 0;
    }

    /**
     * Returns true if the <em>current</em> row has no cards left.
     *
     * <p>The "current" row is the top row while it still has slots; once
     * the top is exhausted the bottom row becomes current. This mirrors
     * the same rule used in {@code Game.handlePass()} and is used by
     * {@code ActionValidator} to decide whether a pass is legal.
     */
    public boolean isCurrentRowEmpty() {
        if (remainingTopCards > 0) return false;   // fila superiore: ancora carte
        return remainingBottomCards == 0;           // fila inferiore: nessuna carta
    }

    /** Returns the total number of slots still remaining (top + bottom). */
    public int totalSlotsRemaining() {
        return remainingTopCards + remainingBottomCards;
    }

    public Player    getActingPlayer()      { return actingPlayer; }
    public OfferTile getTile()              { return tile; }
    public int       remainingTopCards()    { return remainingTopCards; }
    public int       remainingBottomCards() { return remainingBottomCards; }
}