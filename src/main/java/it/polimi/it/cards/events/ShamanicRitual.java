package it.polimi.it.cards.events;

import it.polimi.it.Player.Player;

import java.util.ArrayList;
import java.util.List; // Import corretto

public class ShamanicRitual implements EventStrategy {

    private int reward;
    private int penalty;
    private final List<Player> winners = new ArrayList<>();
    private final List<Player> losers = new ArrayList<>();


    // Il nome del costruttore ora coincide con la classe
    public ShamanicRitual(int reward, int penalty) {
        this.reward = reward;
        this.penalty = penalty;

    }

    @Override
    public void apply(List<Player> players) {winners.clear();
        losers.clear();

        int max = players.stream()
                .mapToInt(p -> p.getTribe().getTotalShamanIcons())
                .max().orElse(0);

        int min = players.stream()
                .mapToInt(p -> p.getTribe().getTotalShamanIcons())
                .min().orElse(0);

        if (max == min) return;

        for (Player p : players) {
            int icons = p.getTribe().getTotalShamanIcons();
            if (icons == max) {
                winners.add(p);
                p.addPrestigePoint(reward);
                // Qui notifichiamo gli edifici del vincitore per eventuali raddoppi
                p.getTribe().getBuildings().forEach(b -> b.onRitual(p, this));
            } else if (icons == min) {
                losers.add(p);
                p.addPrestigePoint(-penalty);
                // Qui notifichiamo gli edifici del perdente per eventuali protezioni
                p.getTribe().getBuildings().forEach(b -> b.onRitual(p, this));
            }
        }
    }

    // Metodi helper per le Strategy degli edifici
    public boolean isWinner(Player p) { return winners.contains(p); }
    public boolean isLoser(Player p) { return losers.contains(p); }
    public int getRewardPoints() { return reward; }
    public int getPenaltyPoints() { return penalty;
    }

    @Override
    public void applyEffect(Player player) {
        // Questo rimane vuoto o con una logica di default per il singolo
    }
}