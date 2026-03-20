package it.polimi.it.cards.building;

import it.polimi.it.Player.Player;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.cards.events.SustenanceStrategy;

public class SustenanceDiscountStrategy implements BuildingStrategy {
    private final CharacterType type;
    private final int discountPerCard;

    public SustenanceDiscountStrategy(CharacterType type, int discountPerCard) {
        this.type = type;
        this.discountPerCard = discountPerCard;
    }

    public CharacterType getType() {
        return this.type;
    }

    public int getDiscountPerCard() {
        return this.discountPerCard;
    }

    @Override
    public void onNewCharacter(it.polimi.it.Player.Player player, it.polimi.it.cards.character.Character c) {}

    @Override
    public String toString() {
        return "Sustenance Discount: " + type + " costs " + discountPerCard + " less food.";
    }

    @Override
    public void onSustenance(Player player, SustenanceStrategy event) {
        int count = player.getTribe().countType(type);
        // event.applyDiscount(count * discountPerCard);
    }
}