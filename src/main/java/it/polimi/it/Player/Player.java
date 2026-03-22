package it.polimi.it.Player;

import it.polimi.it.Board;
import it.polimi.it.cards.building.Building;
import it.polimi.it.cards.character.Character;
import it.polimi.it.enums.TotemColour;

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
        this.tribe = new Tribe(this);
        this.food = 0;
        this.prestigePoint = 0;
        this.turnOrder = 0;
    }

    //*metodo per acquisisre un personaggio*//
    public void takeCharacterCard(Character c, Board board) {
        if (c != null) {
            tribe.addCharacter(c);
            c.applyEffect(this, board); // Hunter trigger icon fires immediately
        }
    }



    public boolean takeBuildingCard(Building b, Board board) {
        if (b==null) return false;

        int cost = b.getCost(this.tribe);
        if (food < cost) {
            return false;
        }

        this.food -= cost;
        tribe.addBuilding(b);
        b.applyEffect(this,board);
        return true;
    }

    public void addFood(int amount)          {

        this.food += amount;
        if (this.food<0) this.food=0;
    }

    public void addPrestigePoint(int pp)     {

        prestigePoint += pp;
    }


    public void spendFood(int toPay) {
        this.food -= toPay;
        if (this.food < 0) this.food = 0;
    }

    public int calculateFinalScore() {
        // 1. Punti accumulati durante la partita (icone dirette ed eventi)
        int total = this.prestigePoint;

        // 2. Punti base dei personaggi (Builder, Artist, Inventor)
        total += tribe.ppBuilders();
        total += tribe.ppArtists();
        total += tribe.ppInventors();

        // 3. Punti base degli edifici costruiti
        total += tribe.ppBuildings();

        // 4. BONUS DINAMICI (Building Strategy)
        // Ogni edificio cicla sulle sue strategie per aggiungere i bonus finali (Era III)
        for (Building b : tribe.getBuildings()) {
            total += b.endGamePoints(this);
        }

        return total;
    }

    public String getName()              {
        return name;
    }
    public int    getFood()              { return food; }
    public int    getPrestigePoint()     { return prestigePoint; }
    public int    getTurnOrder()         { return turnOrder; }
    public void   setTurnOrder(int o)    { this.turnOrder = o; }

    public TotemColour getTotem() {
        return totem;
    }

    public Tribe  getTribe()             { return tribe; }



}