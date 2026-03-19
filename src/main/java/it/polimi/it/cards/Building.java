package it.polimi.it.cards;

import it.polimi.it.Board;
import it.polimi.it.Player.Player;
import it.polimi.it.Player.Tribe;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;

public class Building extends Card {

    protected int foodCost;
    protected int finalPoints;
    protected String effectDescription;

    public Building(String name, Era era, int foodCost, int finalPoints, String effectDescription) {
        super(name, era);
        this.foodCost = foodCost;
        this.finalPoints = finalPoints;
        this.effectDescription = effectDescription;
    }

    /**
     * Calcola il costo reale sottraendo gli sconti dei Builders della Tribe.
     */
    public int getCost(Tribe tribe) {
        int discount = tribe.getBuildingDiscount();
        int finalCost = this.foodCost - discount;
        return Math.max(0, finalCost); // Il costo non può essere negativo
    }

    /**
     * Effetto immediato all'acquisto (Hook sovrascritto dalla Factory).
     */
    @Override
    public void applyEffect(Player player, Board board) {
        // Di base aggiunge i punti vittoria stampati sulla carta
        player.addPrestigePoint(this.finalPoints);
        // La Factory aggiungerà qui la logica specifica (es. +5 cibo se set completo)
        player.getTribe().addBuilding(this);
    }

    /**
     * Hook per effetti durante gli eventi (es. Bonus Caccia o Sostentamento).
     */
    public void onEvent(Player player, EventType type) {
        // Sovrascritto dinamicamente per gli edifici di Era II
    }

    /**
     * Calcolo dei punti bonus a fine partita (es. Moltiplicatori Era III).
     */
    public int getFinalBonus(Player player) {
        return 0; // Sovrascritto dinamicamente
    }

    // Getters
    public int getFoodCost() { return foodCost; }
    public int getFinalPoints() { return finalPoints; }
    public String getEffectDescription() { return effectDescription; }
}
