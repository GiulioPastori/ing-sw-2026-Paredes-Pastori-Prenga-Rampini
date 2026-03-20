package it.polimi.it.cards.building;


import it.polimi.it.Player.Player;
import it.polimi.it.cards.events.ShamanicRitual;

/**
 * Strategia per l'Edificio II-4: "Raddoppio premio rituale".
 * Se il giocatore vince il rituale (ha più icone stella), il premio in PP è raddoppiato.
 */
public class DoubleRitualRewardStrategy implements BuildingStrategy {

    @Override
    public void onRitual(Player player, ShamanicRitual event) {
        // Verifichiamo se il giocatore è il vincitore del rituale
        // Nota: Assumiamo che la classe ShamanicRitual abbia un metodo isWinner(Player p)
        if (event.isWinner(player)) {
            int originalReward = event.getRewardPoints();
            // Aggiungiamo una seconda volta il premio per ottenere l'effetto raddoppio
            player.addPrestigePoint(originalReward);

            System.out.println("[STRATEGY] Bonus DoubleRitual applicato a: " + player.getName());
        }
    }

    @Override
    public String toString() {
        return "Double Ritual Reward Strategy";
    }
}