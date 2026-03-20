package it.polimi.it.cards.building;

import it.polimi.it.Player.Player;
import it.polimi.it.cards.building.BuildingStrategy;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.cards.events.Hunt;

public class PerTypeStrategy implements BuildingStrategy {
    private final CharacterType type;
    private final int huntFood;
    private final int huntPoints;
    private final int endGamePoints;

    public PerTypeStrategy(CharacterType type, int huntFood, int huntPoints, int endGamePoints) {
        this.type = type;
        this.huntFood = huntFood;
        this.huntPoints = huntPoints;
        this.endGamePoints = endGamePoints;
    }

    @Override
    public void onHunt(Player player, Hunt event) {
        int count = player.getTribe().countType(type); // [cite: 71]
        // Supponendo che Hunt abbia questi metodi per accumulare i bonus dell'evento
        // event.addExtraFood(count * huntFood);
        // event.addExtraPoints(count * huntPoints);
        for(int i=0; i<count; i++) {
            player.addFood(huntFood);
            player.addPrestigePoint(huntPoints);
        }
    }

    @Override
    public int endGamePoints(Player player) {
        return player.getTribe().countType(type) * endGamePoints;
    }
}