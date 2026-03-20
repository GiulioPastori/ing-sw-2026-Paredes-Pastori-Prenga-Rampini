package it.polimi.it.cards.building;


import it.polimi.it.Player.Player;
import it.polimi.it.cards.building.BuildingStrategy;
import it.polimi.it.cards.character.Character;

/**
 * Strategia per l'Edificio II-5: "Bonus Cibo Totem".
 * Ogni volta che aggiungi un personaggio con l'icona Totem, ricevi 1 Cibo.
 */
public class TotemFoodBonusStrategy implements BuildingStrategy {

    @Override
    public void onNewCharacter(Player player, Character c) {
        // Verifichiamo se il personaggio appena preso ha l'icona Totem
        // Nota: Assicurati che la classe Character abbia il metodo hasTotemIcon()
        if (c != null && c.hasTotemIcon()) {
            player.addFood(1);
            System.out.println("[STRATEGY] Icona Totem rilevata! +1 Cibo per " + player.getName());
        }
    }

    @Override
    public String toString() {
        return "Totem Food Bonus Strategy (+1 Food on Totem Icon)";
    }
}
