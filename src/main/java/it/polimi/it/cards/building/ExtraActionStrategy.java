package it.polimi.it.cards.building;

import it.polimi.it.Player.Player;
import it.polimi.it.cards.building.BuildingStrategy;

/**
 * Strategia per l'Edificio II-6: "Azione Extra".
 * Permette al giocatore di acquistare una carta dalla fila superiore a fine round.
 */
public class ExtraActionStrategy implements BuildingStrategy {

    // Nota: onExtraAction è un "hook" personalizzato che chiamerai nel GameController
    public void triggerExtraAction(Player player) {
        System.out.println("[STRATEGY] Attivazione Azione Extra per: " + player.getName());

        /*
           Il GameController dovrebbe verificare se il giocatore possiede un edificio
           con questa strategia e, in caso positivo, aprire una finestra di scelta
           per fargli acquistare una carta dalla fila superiore del tabellone.
        */
    }

    @Override
    public String toString() {
        return "Extra Action Strategy (Top Row Purchase)";
    }
}