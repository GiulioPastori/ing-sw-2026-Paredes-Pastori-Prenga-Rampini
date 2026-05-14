package it.polimi.it.cards.character;

import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;
import it.polimi.it.player.Player;

/**
 * Character card: Gatherer.
 *
 * Sustenance event: each gatherer provides a discount of
 * {@value #FOOD_DISCOUNT_PER_GATHERER} food on the total amount to pay.
 * No immediate effect; no direct contribution to the final score.
 */
public class Gatherer extends Character {

    public static final int FOOD_DISCOUNT_PER_GATHERER = 3;

    public Gatherer(int id, Era era) {
        super(id, era, CharacterType.GATHERER, 0);
    }

    @Override
    public void applyImmediateEffect(Player player) { /* no immediate effect */ }
}
