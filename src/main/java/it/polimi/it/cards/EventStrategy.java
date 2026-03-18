package it.polimi.it.cards;


import it.polimi.it.Player.Player;
import java.util.List;

public interface EventStrategy {
    void apply(List<Player> players);

    void applyEffect(Player player);


}