package it.polimi.it.cards;


import it.polimi.it.Player.Player;
import java.util.List;

public interface EventStrategy {
    void apply(List<Player> players); //*applica l'evento a tutti i giocatori*//

    void applyEffect(Player player); //*applica l'evento a un singolo giocatore*//


}