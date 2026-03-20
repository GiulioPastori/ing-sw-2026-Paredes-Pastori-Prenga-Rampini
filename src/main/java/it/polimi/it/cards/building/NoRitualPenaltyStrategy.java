package it.polimi.it.cards.building;


import it.polimi.it.Player.Player;
import it.polimi.it.cards.building.BuildingStrategy;
import it.polimi.it.cards.events.ShamanicRitual;

/**
 * Strategia per l'Edificio II-3: "Immunità penale rituale".
 * Se il giocatore è tra i perdenti del rituale (ha meno icone stella),
 * la penalità in PP viene annullata.
 */
public class NoRitualPenaltyStrategy implements BuildingStrategy {

    @Override
    public void onRitual(Player player, ShamanicRitual event) {
        // Verifichiamo se il giocatore è finito tra i perdenti (minimo di icone)
        if (event.isLoser(player)) {
            int penaltyAmount = event.getPenaltyPoints();

            // Restituiamo i punti persi (annullando di fatto la penalità)
            player.addPrestigePoint(penaltyAmount);

        }
    }

    @Override
    public String toString() {
        return "No Ritual Penalty Strategy (Immunity)";
    }
}
