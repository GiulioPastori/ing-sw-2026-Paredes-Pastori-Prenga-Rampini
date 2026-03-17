package it.polimi.it.cards.events;

import com.sun.tools.javac.util.List;
import it.polimi.it.Board;
import it.polimi.it.Player.Player;
import it.polimi.it.enums.CharacterType;

public class Hunt implements EventStrategy {

    private int prestigePerHunter;

    public HuntStrategy(int prestigePerHunter) {
        this.prestigePerHunter = prestigePerHunter;
    }

    @Override
    public void apply(List<Player> players, Board board) {
        for (Player p : players) {
            int hunters = p.getTribe().countByType(CharacterType.HUNTERS);

            p.addFood(hunters);
            p.addPrestige(hunters * prestigePerHunter);
        }
    }
}

