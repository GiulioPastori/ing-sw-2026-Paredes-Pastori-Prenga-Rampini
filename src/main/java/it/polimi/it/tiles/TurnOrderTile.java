package it.polimi.it.tiles;

import it.polimi.it.enums.TotemColour;
import it.polimi.it.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Turn Order Tile.
 *
 * Contains one space per player. After resolving their offer tile action,
 * each player places their totem in the first free space from the top and
 * immediately receives the space's bonus (or pays the penalty).
 *
 * Space bonuses and penalties (rulebook p.4):
 * - 1st space: +3 food
 * - 2nd space: +1 food
 * - middle spaces: no bonus
 * - last space: pay 1 food; if none available, lose 2 PP
 *
 * The order of totems at the end of a round determines who places first
 * on the Offer Track in the next round.
 *
 * After totem placement, buildings with a TurnHook are notified
 * (e.g. building 104: +1 food when landing on a food-bonus space).
 */
public class TurnOrderTile {

    private static final int LAST_PLACE_PP_PENALTY = -2;

    private final List<TurnPosition> positions;
    private final int numPlayers;

    /**
     * @param numPlayers number of players (= number of spaces, 2–5)
     */
    public TurnOrderTile(int numPlayers) {
        this.numPlayers = numPlayers;
        this.positions  = new ArrayList<>();
        buildPositions(numPlayers);
    }

    private void buildPositions(int n) {
        for (int i = 1; i <= n; i++) {
            int     foodBonus      = (i == 1) ? 3 : (i == 2) ? 1 : 0;
            boolean isLastPosition = (i == n);
            positions.add(new TurnPosition(i, foodBonus, isLastPosition));
        }
    }

    /**
     * Places the player's totem in the first free space from the top, applies
     * the space's bonus or penalty immediately, then notifies buildings with
     * a TurnHook.
     *
     * Last-space penalty: the player pays 1 food if available, otherwise loses 2 PP.
     *
     * @param player the player repositioning their totem
     * @return the space that was occupied
     * @throws IllegalStateException if no free space is available
     */
    public TurnPosition placeTotem(Player player) {
        for (TurnPosition pos : positions) {
            if (pos.isFree()) {
                pos.place(player.getTotemColour());
                player.setTurnOrder(pos.getPosition());

                if (pos.getFoodBonus() > 0) {
                    player.addFood(pos.getFoodBonus());
                }

                if (pos.isLastPosition()) {
                    if (player.getFood() > 0) {
                        player.spendFood(1);
                    } else {
                        player.addPrestigePoints(LAST_PLACE_PP_PENALTY);
                    }
                }

                // Notify buildings with a TurnHook (e.g. building 104)
                player.getTribe().getBuildings().stream()
                        .filter(b -> b.getEffect().hasTurnHook())
                        .forEach(b -> b.applyTurnHook(player, pos));

                return pos;
            }
        }
        throw new IllegalStateException("No free space on the Turn Order Tile.");
    }

    /**
     * Returns the totem colours in order (top to bottom, spaces 1 to n).
     * Free spaces are skipped. Used to determine placement order for the next round.
     */
    public List<TotemColour> getTotemOrder() {
        List<TotemColour> order = new ArrayList<>();
        for (TurnPosition pos : positions) {
            if (!pos.isFree()) order.add(pos.getPlacedTotem());
        }
        return order;
    }

    /** Clears all spaces. Called at the start of each round. */
    public void reset() {
        for (TurnPosition pos : positions) {
            if (!pos.isFree()) pos.free();
        }
    }

    public List<TurnPosition> getPositions() { return List.copyOf(positions); }
    public int                getNumPlayers() { return numPlayers; }
}
