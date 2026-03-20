package it.polimi.it.cards.building;

import it.polimi.it.Board;
import it.polimi.it.Player.Player;
import it.polimi.it.Player.Tribe;
import it.polimi.it.cards.Card;
import it.polimi.it.cards.events.ShamanicRitual;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;

import java.util.ArrayList;
import java.util.List;

public class Building extends Card {

    protected int foodCost;
    protected int finalPoints;
    protected String effectDescription;
    private List<BuildingStrategy> strategies;

    public Building(String name, Era era, int foodCost, int finalPoints, String effectDescription, List<BuildingStrategy> strategies) {
        super(name, era);
        this.foodCost = foodCost;
        this.finalPoints = finalPoints;
        this.effectDescription = effectDescription;
        this.strategies = (strategies != null) ? strategies : new ArrayList<>();
    }

    /**
     * Calcola il costo reale sottraendo gli sconti dei Builders della Tribe.
     */
    public int getCost(Tribe tribe) {
        int discount = tribe.getBuildingDiscount();
        int finalCost = this.foodCost - discount;
        return Math.max(0, finalCost); // Il costo non può essere negativo
    }

    public void onNewCharacter(Player player, it.polimi.it.cards.character.Character c) {
        for (BuildingStrategy s : strategies) {
            s.onNewCharacter(player,c );
        }
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

    public void onRitual(Player player, ShamanicRitual event) {
        for (BuildingStrategy s : strategies) {
            s.onRitual(player, event);
        }
    }

    public List<BuildingStrategy> getStrategies() {
        return strategies;
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

    public int endGamePoints(Player player) {
        int totalBonus = 0;
        // Cicla su ogni strategia contenuta nella lista dell'edificio
        for (BuildingStrategy s : strategies) {
            totalBonus += s.endGamePoints(player);
        }
        return totalBonus;
    }

    // Getters
    public int getFoodCost() { return foodCost; }
    public int getFinalPoints() { return finalPoints; }
    public String getEffectDescription() { return effectDescription; }
}
