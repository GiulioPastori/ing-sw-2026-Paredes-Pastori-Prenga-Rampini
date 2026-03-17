package it.polimi.it.cards.events;

import com.sun.tools.javac.util.List;
import it.polimi.it.Board;
import it.polimi.it.Player.Player;

public interface EventStrategy {
    void apply(List<Player> players, Board board);
}