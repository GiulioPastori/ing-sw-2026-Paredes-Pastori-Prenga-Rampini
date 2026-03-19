package it.polimi.it.cards.events;

import it.polimi.it.Player.Player;
import it.polimi.it.cards.EventStrategy;
import it.polimi.it.enums.CharacterType;
import java.util.List;

public class SustenanceStrategy implements EventStrategy {

    private final int penaltyPerMissingFood;

    /**
     * Costruttore: riceve la penalità indicata sulla carta Evento (es. 2 PP).
     */
    public SustenanceStrategy(int penaltyPerMissingFood) {
        this.penaltyPerMissingFood = penaltyPerMissingFood;
    }

    @Override
    public void apply(List<Player> players) {
        for (Player p : players) {
            applyEffect(p);
        }
    }

    @Override
    public void applyEffect(Player player) {
        // 1. "Pagate 1 Cibo per ogni carta Personaggio nella vostra tribù"
        int totalCharacters = player.getTribe().getCharacters().size();

        // 2. "Ogni Raccoglitore fornisce uno sconto di 3 Cibo"
        int gatherers = player.getTribe().countType(CharacterType.GATHERER);
        int totalDiscount = gatherers * 3;

        // 3. Calcolo del cibo effettivamente richiesto
        int foodRequired = Math.max(0, totalCharacters - totalDiscount);
        int currentFood = player.getFood();

        // 4. "Non si può scegliere di perdere PP per non pagare Cibo"
        // Il giocatore paga tutto quello che può, fino a esaurimento scorte
        int foodPaid = Math.min(foodRequired, currentFood);
        player.spendFood(foodPaid);

        // 5. Calcolo dei personaggi rimasti affamati
        int unfedCharacters = foodRequired - foodPaid;

        // 6. Applicazione della penalità
        if (unfedCharacters > 0) {
            int totalPenalty = unfedCharacters * penaltyPerMissingFood;
            player.addPrestigePoint(-totalPenalty);

            System.out.println("[EVENTO SOSTENTAMENTO] " + player.getName() +
                    " non ha sfamato " + unfedCharacters +
                    " personaggi. Penalità: -" + totalPenalty + " PP.");
        }
    }
}