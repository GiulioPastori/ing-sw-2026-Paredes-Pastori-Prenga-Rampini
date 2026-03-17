package it.polimi.it.tiles;


import it.polimi.it.enums.TotemColour;

import java.util.ArrayList;
import java.util.List;

public class TurnOrderTile {
    private final List<TurnPosition> positions;
    private final int numPlayers;

    public TurnOrderTile(int numPlayers) {
        this.numPlayers = numPlayers;
        this.positions  = new ArrayList<>();
        buildPositions(numPlayers);
    }

    /**
    Bonus e malus del tabellone come da regolamento
     */
    private void buildPositions(int n) {
        for (int i = 1; i <= n; i++) {
            int food    = (i == 1) ? 3 : (i == 2) ? 1 : 0;
            int penalty = (i == n) ? -2 : 0;
            positions.add(new TurnPosition(i, food, penalty));
        }
    }

    /**
     * Places the player's totem in the first free space from the top,
     * applies food bonus or last-place penalty immediately.
     */
    public TurnPosition placeTotem(Player player) {
        for (TurnPosition pos : positions) {
            if (pos.freeisFree()) {
                pos.place(player.getTotem());
                player.setTurnOrder(pos.getPosition());
                if (pos.getFoodTokens() > 0)
                    player.addFood(pos.getFoodTokens());
                if (pos.getPenalties() < 0) {
                    if (player.getFood() > 0) player.addFood(-1);
                    else player.addPrestigePoint(pos.getPenalties());
                }
                return new SpaceTurn(pos);
            }
        }
        throw new IllegalStateException("No free space on Turn Order tile.");
    }

    /** Returns the ordered list of totems for next round's placement order. */
    public List<TotemColour> getOrder() {
        List<TotemColour> order = new ArrayList<>();
        for (TurnPosition pos : positions)
            if (!pos.isFree()) order.add(pos.getPlacedTotem());
        return order;
    }

    /** Clears all spaces at start of each round. */
    public void reset() {
        for (TurnPosition pos : positions) pos.free();
    }

    public List<TurnPosition> getPositions() { return positions; }
    public int                getNumPlayers() { return numPlayers; }
}
