package it.polimi.it.cards.events;

import it.polimi.it.cards.character.Gatherer;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.player.Player;

import java.util.List;

/**  Strategy for the Sustenance event.
  Each player pays 1 food for each character in their tribe.
  Discounts applied in order:
  1. Gatherers: -3 food each
  2. Buildings with SustenanceModifier: additional discount declared by the building

 If characters remain unfed, the player loses PP for each one.
 Sustenance is ALWAYS resolved last among events in the same round. */
public class Sustenance implements EventStrategy {

    private final int penaltyPerUnfedCharacter;

    public Sustenance(int penaltyPerUnfedCharacter) {
        this.penaltyPerUnfedCharacter = penaltyPerUnfedCharacter;
    }

    @Override
    public void apply(List<Player> players) {
        for (Player player : players) {
            resolveForPlayer(player);
        }
    }

    private void resolveForPlayer(Player player) {
        int characterCount   = player.getTribe().getCharacterCount();
        int gathererDiscount = player.getTribe().countByType(CharacterType.GATHERER)
                * Gatherer.FOOD_DISCOUNT_PER_GATHERER;

        // Additional discount from buildings with SustenanceModifier (e.g. 102, 307)
        int buildingDiscount = player.getTribe().getBuildings().stream()
                .filter(b -> b.getEffect().hasSustenanceModifier())
                .mapToInt(b -> b.getEffect().getSustenanceModifier().extraFoodDiscount(player))
                .sum();

        int foodRequired = Math.max(0, characterCount - gathererDiscount - buildingDiscount);
        int foodPaid     = Math.min(foodRequired, player.getFood());
        player.spendFood(foodPaid);

        int unfed = foodRequired - foodPaid;
        if (unfed > 0) {
            player.addPrestigePoints(-unfed * penaltyPerUnfedCharacter);
        }
    }

    public int getPenaltyPerUnfedCharacter() { return penaltyPerUnfedCharacter; }
}