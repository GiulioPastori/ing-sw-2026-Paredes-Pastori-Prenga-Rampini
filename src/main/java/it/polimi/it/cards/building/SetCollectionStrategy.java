package it.polimi.it.cards.building;


import it.polimi.it.Player.Player;
import it.polimi.it.cards.building.BuildingStrategy;
import it.polimi.it.cards.character.Character;

public class SetCollectionStrategy implements BuildingStrategy {
    private final int reward;
    private final boolean isFood; // true per Era I (Cibo), false per Era III (Punti)

    public SetCollectionStrategy(int reward, boolean isFood) {
        this.reward = reward;
        this.isFood = isFood;
    }

    @Override
    public int endGamePoints(Player player) {
        // Se l'edificio dà cibo (Era I), non dà punti a fine partita
        if (isFood) return 0;

        // Calcoliamo quanti set da 6 personaggi unici ha il giocatore
        int uniqueCount = player.getTribe().getUniqueCharactersCount();
        int sets = uniqueCount / 6;
        return sets * reward;
    }

    @Override
    public void onNewCharacter(Player player, Character c) {
        // Se l'edificio dà cibo (Era I), controlliamo se il nuovo personaggio completa un set
        if (isFood) {
            int uniqueCount = player.getTribe().getUniqueCharactersCount();
            // Se abbiamo esattamente 6 personaggi unici (o un multiplo), diamo il cibo
            // Nota: questa logica dipende da come si contano i set durante il gioco
            if (uniqueCount > 0 && uniqueCount % 6 == 0) {
                player.addFood(reward);
            }
        }
    }
}