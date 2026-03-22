package it.polimi.it.tiles;

import it.polimi.it.Player.Player;
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
     * Bonus e malus del tabellone come da regolamento.
     */
    private void buildPositions(int n) {
        for (int i = 0; i < n; i++) {
            // i=0 è il 1° posto, i=n-1 è l'ultimo
            int food    = (i == 0) ? 3 : (i == 1) ? 1 : 0;
            int penalty = (i == n - 1) ? -2 : 0;
            positions.add(new TurnPosition(i + 1, food, penalty));
        }
    }

    /**
     * Occupa il primo spazio libero dall'alto e applica bonus/malus.
     */
    public void placeTotem(Player player) {
        for (TurnPosition pos : positions) {
            if (pos.isFree()) { // FIX: era freeisFree()
                pos.place(player.getTotem());

                // Aggiorna l'ordine di turno del giocatore per il prossimo round
                player.setTurnOrder(pos.getPosition());

                // Applica Bonus Cibo
                if (pos.getFoodTokens() > 0) {
                    player.addFood(pos.getFoodTokens());
                }

                // Applica Malus Ultimo Posto
                if (pos.getPenalties() < 0) {
                    // Se ha cibo perde 1 cibo, altrimenti perde punti prestigio
                    if (player.getFood() > 0) {
                        player.addFood(-1);
                    } else {
                        player.addPrestigePoint(pos.getPenalties());
                    }
                }
                return; // Usciamo appena troviamo il primo posto libero
            }
        }
        throw new IllegalStateException("Nessun spazio libero sulla tessera Ordine di Turno.");
    }

    public List<TotemColour> getOrder() {
        List<TotemColour> order = new ArrayList<>();
        for (TurnPosition pos : positions) {
            if (!pos.isFree()) order.add(pos.getPlacedTotem());
        }
        return order;
    }

    public void reset() {
        for (TurnPosition pos : positions) {
            pos.free();
        }
    }

    public List<TurnPosition> getPositions() { return positions; }
}
