package it.polimi.it.cards.building;


import it.polimi.it.Player.Player;
// IMPORTANTE: Usa il tuo Character, non java.lang.Character!
import it.polimi.it.cards.character.Character;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.cards.building.BuildingStrategy;

public class InventorPairStrategy implements BuildingStrategy {
    private final int foodReward;

    public InventorPairStrategy(int foodReward) {
        this.foodReward = foodReward;
    }

    @Override
    public void onNewCharacter(Player player, Character c) {
        // Controlliamo che c non sia null e che sia un INVENTOR
        if (c != null && c.getType() == CharacterType.INVENTOR) {
            // Chiamiamo il metodo sulla tribù del giocatore
            if (player.getTribe().hasDuplicateInventorIcon(c)) {
                player.addFood(foodReward);
            }
        }
    }
}