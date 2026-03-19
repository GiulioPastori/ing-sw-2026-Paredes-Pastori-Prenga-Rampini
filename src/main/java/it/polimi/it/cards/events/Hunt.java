package it.polimi.it.cards.events;

import it.polimi.it.Player.Player;
import it.polimi.it.cards.EventStrategy;
import it.polimi.it.enums.CharacterType;
import java.util.List;

public class Hunt implements EventStrategy {

    private final int ppPerHunter;

    /**
     * @param ppPerHunter Il valore di Punti Prestigio indicato sulla carta Evento (es. 2 PP).
     */
    public Hunt(int ppPerHunter) {
        this.ppPerHunter = ppPerHunter;
    }

    @Override
    public void apply(List<Player> players) {
        for (Player p : players) {
            applyEffect(p);
        }
    }

    @Override
    public void applyEffect(Player player) {
        // 1. Contiamo quanti cacciatori ci sono nella tribù
        int hunterCount = player.getTribe().countType(CharacterType.HUNTER);

        // 2. REGOLA: "Un cibo per ogni cacciatore che hai nella tua tribù"
        player.addFood(hunterCount);

        // 3. REGOLA: "Guadagnate i PP indicati sulla carta per ogni cacciatore"
        int totalPP = hunterCount * ppPerHunter;
        player.addPrestigePoint(totalPP);

        // Log di controllo
        System.out.println("[EVENTO CACCIA] " + player.getName() +
                ": Cacciatori: " + hunterCount +
                " -> +" + hunterCount + " Cibo, +" + totalPP + " PP.");
    }
}

