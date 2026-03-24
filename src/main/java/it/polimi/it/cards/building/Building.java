package it.polimi.it.cards.building;

import it.polimi.it.player.Tribe;
import it.polimi.it.cards.Card;
import it.polimi.it.cards.character.Character;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;
import it.polimi.it.player.Player;
import it.polimi.it.tiles.TurnPosition;

/**
 * Building card.
 *
 * The building ID serves two purposes:
 * 1. Network key (carried in TakeCardAction / PostRoundAction).
 * 2. Index into BuildingEffectRegistry to retrieve the card's special effect.
 *
 * The effective food cost is the base cost minus the total builder discount
 * accumulated in the player's tribe (never below 0, rulebook p.5).
 */
public class Building extends Card {

    private final int           foodCost;
    private final int           finalPoints;
    private final String        effectDescription;
    private final BuildingEffect effect;

    public Building(int id, Era era, int foodCost, int finalPoints,
                    String effectDescription) {
        super(id, era);
        this.foodCost          = foodCost;
        this.finalPoints       = finalPoints;
        this.effectDescription = effectDescription;
        this.effect            = BuildingEffectRegistry.get(id);
    }

    /** Returns the effective food cost after applying builder discounts. Never negative. */
    public int getEffectiveCost(Tribe tribe) {
        return Math.max(0, foodCost - tribe.totalBuildingDiscount());
    }

    public boolean triggersOnEvent(EventType eventType) {
        return effect.triggersOn(eventType);
    }

    public void applyEventHook(Player player, EventType eventType) {
        if (effect.hasEventHook()) effect.getEventHook().apply(player, eventType);
    }

    public void applyTurnHook(Player player, TurnPosition position) {
        if (effect.hasTurnHook()) effect.getTurnHook().apply(player, position);
    }

    public void applyAcquisitionHook(Player player, Character acquired) {
        if (effect.hasAcquisitionHook()) effect.getAcquisitionHook().apply(player, acquired);
    }

    public int computeEndGameBonus(Player player) {
        return effect.hasEndGameScorer() ? effect.getEndGameScorer().compute(player) : 0;
    }

    public boolean hasPostRoundAction() { return effect.hasPostRound(); }

    public int           getFoodCost()          { return foodCost; }
    public int           getFinalPoints()       { return finalPoints; }
    public String        getEffectDescription() { return effectDescription; }
    public BuildingEffect getEffect()           { return effect; }
}