package it.polimi.it.cards.events;

import it.polimi.it.Board;
import it.polimi.it.Player.Player;
import it.polimi.it.cards.EventStrategy;
import java.util.List; // Import corretto

public class ShamanicRitual implements EventStrategy {

    private int reward;
    private int penalty;

    // Il nome del costruttore ora coincide con la classe
    public ShamanicRitual(int reward, int penalty) {
        this.reward = reward;
        this.penalty = penalty;
    }

    @Override
    public void apply(List<Player> players) {
        // 1. Calcoliamo il massimo e il minimo di icone nella valle
        int max = players.stream()
                .mapToInt(p -> p.getTribe().getTotalShamanIcons())
                .max().orElse(0);

        int min = players.stream()
                .mapToInt(p -> p.getTribe().getTotalShamanIcons())
                .min().orElse(0);

        // Se il massimo è uguale al minimo
        if (max == min) {
            System.out.println("Pareggio");
            return;
        }

        // 2. Applichiamo premi e penalità
        for (Player p : players) {
            int icons = p.getTribe().getTotalShamanIcons();

            if (icons == max) {
                p.addPrestigePoint(reward);
                System.out.println(p.getName() + " vince il rituale! +" + reward);
            }
            else if (icons == min) { // "else if" evita che chi è sia max che min (caso unico) prenda entrambi
                p.addPrestigePoint(-penalty);
                System.out.println(p.getName() + " fallisce il rituale... -" + penalty);
            }
        }
    }

    @Override
    public void applyEffect(Player player) {
        // Questo rimane vuoto o con una logica di default per il singolo
    }
}