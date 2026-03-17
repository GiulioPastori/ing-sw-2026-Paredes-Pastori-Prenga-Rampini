package it.polimi.it.cards.events;

import com.sun.tools.javac.util.List;
import it.polimi.it.Board;
import it.polimi.it.Player.Player;

public class ShamanicRitual implements EventStrategy {

    private int reward;
    private int penalty;

    public ShamanicRitualStrategy(int reward, int penalty) {
        this.reward = reward;
        this.penalty = penalty;
    }

    @Override
    public void apply(List<Player> players, Board board) {

        int max = players.stream()
                .mapToInt(p -> p.getTribe().shamanIcons())
                .max().orElse(0);

        int min = players.stream()
                .mapToInt(p -> p.getTribe().shamanIcons())
                .min().orElse(0);

        for (Player p : players) {
            int icons = p.getTribe().shamanIcons();

            if (icons == max) {
                p.addPrestige(reward);
            }
            if (icons == min) {
                p.addPrestige(-penalty);
            }
        }
    }
}