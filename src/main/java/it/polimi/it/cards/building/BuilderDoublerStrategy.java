package it.polimi.it.cards.building;


import it.polimi.it.Player.Player;

public class BuilderDoublerStrategy implements BuildingStrategy {
    @Override
    public int endGamePoints(Player player) {
        // Restituisce una somma pari ai punti già accumulati dai costruttori (ottenendo l'effetto x2)
        return player.getTribe().ppBuilders();
    }
}