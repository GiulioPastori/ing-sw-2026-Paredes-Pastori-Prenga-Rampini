package it.polimi.it.cards;


import it.polimi.mesos.Game;
import it.polimi.mesos.enums.Era;

public class Building extends Card {

    private int costFood;

    private int finalPoints;

    public Building(String name, Era era, int costFood, int finalPoints) {

        super(name, era);

        this.costFood = costFood;
        this.finalPoints = finalPoints;

    }

    public int getCostFood() {
        return costFood;
    }

    public int getFinalPoints() {
        return finalPoints;
    }

    @Override
    public void applyEffect(Game game) {

    }
}
