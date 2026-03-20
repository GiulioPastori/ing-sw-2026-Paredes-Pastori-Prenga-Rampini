package it.polimi.it.cards.building;

import it.polimi.it.Player.Player;

public class FixedPointsStrategy implements BuildingStrategy {
    private final int points;

    public FixedPointsStrategy(int points) { this.points = points; }

    @Override
    public int endGamePoints(Player player) { return points; }

}