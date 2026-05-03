package it.polimi.it.tiles;

import it.polimi.it.enums.TotemColour;
import it.polimi.it.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Bonus per spazio basati sulle tessere fisiche del gioco:
 *   2 giocatori: [+1] [-1/-2PP]
 *   3 giocatori: [+2] [ 0 ] [-1/-2PP]
 *   4 giocatori: [+2] [+1] [ 0 ] [-1/-2PP]
 *   5 giocatori: [+3] [+1] [ 0 ] [ 0 ] [-1/-2PP]
 */
public class TurnOrderTile {

    private static final int LAST_PLACE_PP_PENALTY = -2;

    private final List<TurnPosition> positions;
    private final int numPlayers;

    public TurnOrderTile(int numPlayers) {
        this.numPlayers = numPlayers;
        this.positions  = new ArrayList<>();
        buildPositions(numPlayers);
    }

    private void buildPositions(int n) {
        for (int i = 1; i <= n; i++) {
            boolean isLast = (i == n);
            int foodBonus = switch (n) {
                case 2 -> (i == 1) ? 1 : 0;
                case 3 -> (i == 1) ? 2 : 0;
                case 4 -> (i == 1) ? 2 : (i == 2) ? 1 : 0;
                case 5 -> (i == 1) ? 3 : (i == 2) ? 1 : 0;
                default -> 0;
            };
            positions.add(new TurnPosition(i, foodBonus, isLast));
        }
    }

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

                // Notifica edifici con TurnHook (es. edificio 104)
                player.getTribe().getBuildings().stream()
                        .filter(b -> b.getEffect().hasTurnHook())
                        .forEach(b -> b.applyTurnHook(player, pos));

                return pos;
            }
        }
        throw new IllegalStateException("No free space on the Turn Order Tile.");
    }

    public List<TotemColour> getTotemOrder() {
        List<TotemColour> order = new ArrayList<>();
        for (TurnPosition pos : positions) {
            if (!pos.isFree()) order.add(pos.getPlacedTotem());
        }
        return order;
    }

    public void reset() {
        for (TurnPosition pos : positions) {
            if (!pos.isFree()) pos.free();
        }
    }

    public List<TurnPosition> getPositions() { return List.copyOf(positions); }
    public int                getNumPlayers() { return numPlayers; }
}
