package it.polimi.it.cards.building.modifier;

import it.polimi.it.player.Player;

/**
 * Capability interface implemented by buildings that modify the Sustenance event.
 *
 * Replaces the numeric building-ID comparison in Sustenance, following the
 * same Visitor-style capability pattern used by ShamanicModifier.
 *
 * Modelled effect:
 * - extraFoodDiscount: additional food reduction during Sustenance
 *   (buildings 102/307: -1 food per Artist, Inventor or Gatherer in the tribe)
 */
public interface SustenanceModifier {

    /**
     * Returns the additional food discount provided to this player during Sustenance.
     * Stacks on top of the base Gatherer discount.
     *
     * @param owner the player who owns the building
     * @return additional food discounted (>= 0)
     */
    int extraFoodDiscount(Player owner);
}
