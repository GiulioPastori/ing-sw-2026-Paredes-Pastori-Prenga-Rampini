package it.polimi.it.Player;

import it.polimi.it.cards.Building;

public class Player {
    private final String name;
    private int food;
    private int prestigePoint;
    private int turnOrder;
    private final TotemColour totem;
    private final Tribe tribe;

    public Player(String name, TotemColour totem) {
        this.name = name;
        this.totem = totem;
        this.tribe = new Tribe();
        this.food = 0;
        this.prestigePoint = 0;
        this.turnOrder = 0;
    }

    public void takeCharacterCard(Character c) {
        tribe.addCharacter(c);
        c.applyEffect(this); // Hunter trigger icon fires immediately
    }

    public boolean takeBuildingCard(Building b) {
        int cost = b.getCost(tribe);
        if (food < cost) return false;
        food -= cost;
        tribe.addBuilding(b);
        b.applyEffect(this);
        return true;
    }

    public void addFood(int amount)          {
        food += amount;
    }
    public void addPrestigePoint(int pp)     {
        prestigePoint += pp;
    }


    public int calculateFinalScore() {
        return prestigePoint
                + tribe.ppBuilders()
                + tribe.ppArtists()
                + tribe.ppInventors()
                + tribe.ppBuildings();
    }

    public String getName()              {
        return name;
    }
    public int    getFood()              { return food; }
    public int    getPrestigePoint()     { return prestigePoint; }
    public int    getTurnOrder()         { return turnOrder; }
    public void   setTurnOrder(int o)    { this.turnOrder = o; }
    public Totem  getTotem()             { return totem; }
    public Tribe  getTribe()             { return tribe; }

    public void spendFood(int toPay) {
    }
}