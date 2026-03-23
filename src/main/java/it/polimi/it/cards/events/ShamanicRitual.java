package it.polimi.it.cards.event;

import it.polimi.it.cards.building.modifier.ShamanicModifier;
import it.polimi.it.player.Player;

import java.util.List;

/**
 * Strategy for the Shamanic Ritual event.
 *
 * The player with the most star icons gains rewardForMost PP;
 * the player with the fewest loses penaltyForFewest PP. Ties are included in both cases.
 *
 * Buildings modify the behaviour via ShamanicModifier:
 * - extraStarIcons(): virtual star icon bonus before the comparison
 * - isImmuneToMinPenalty(): immunity to the minimum penalty
 * - doublesMaxReward(): doubles the majority reward
 *
 * ShamanicRitual does not know building IDs: it queries the capability
 * declared by the interface. Adding a new building with a shamanic effect
 * shamanic effect does not require modifying this class.
 */
public class ShamanicRitual implements EventStrategy {

    private final int rewardForMost;
    private final int penaltyForFewest;

    public ShamanicRitual(int rewardForMost, int penaltyForFewest) {
        this.rewardForMost    = rewardForMost;
        this.penaltyForFewest = penaltyForFewest;
    }

    @Override
    public void apply(List<Player> players) {
        if (players.isEmpty()) return;

        int[] effective = new int[players.size()];
        for (int i = 0; i < players.size(); i++) {
            effective[i] = computeEffectiveIcons(players.get(i));
        }

        int max = 0, min = Integer.MAX_VALUE;
        for (int icons : effective) {
            if (icons > max) max = icons;
            if (icons < min) min = icons;
        }

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            int icons     = effective[i];

            if (icons == max) {
                boolean doubles = player.getTribe().getBuildings().stream()
                        .anyMatch(b -> b.getEffect().hasShamanicModifier()
                                && b.getEffect().getShamanicModifier().doublesMaxReward(player));
                int reward = doubles ? rewardForMost * 2 : rewardForMost;
                player.addPrestigePoints(reward);
            }

            if (icons == min) {
                boolean immune = player.getTribe().getBuildings().stream()
                        .anyMatch(b -> b.getEffect().hasShamanicModifier()
                                && b.getEffect().getShamanicModifier().isImmuneToMinPenalty(player));
                if (!immune) {
                    player.addPrestigePoints(-penaltyForFewest);
                }
            }
        }
    }

    private int computeEffectiveIcons(Player player) {
        int base  = player.getTribe().totalShamanIcons();
        int bonus = player.getTribe().getBuildings().stream()
                .filter(b -> b.getEffect().hasShamanicModifier())
                .mapToInt(b -> b.getEffect().getShamanicModifier().extraStarIcons(player))
                .sum();
        return base + bonus;
    }

    public int getRewardForMost()    { return rewardForMost; }
    public int getPenaltyForFewest() { return penaltyForFewest; }
}