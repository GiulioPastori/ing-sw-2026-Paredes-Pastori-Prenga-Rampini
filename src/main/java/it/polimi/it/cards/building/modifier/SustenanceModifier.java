package it.polimi.it.cards.building.modifier;

import it.polimi.it.player.Player;

/** Capability interface implemented by buildings that modify the Sustenance event. */
public interface SustenanceModifier {

    /**
     * Returns the additional food discount provided to this player during Sustenance.
     * Stacks on top of the base Gatherer discount.
     * @param owner the player who owns the building
     * @return additional food discounted (>= 0)
     */
    int extraFoodDiscount(Player owner);
}
