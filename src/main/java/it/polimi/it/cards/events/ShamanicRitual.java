package it.polimi.it.cards.events;

import it.polimi.it.player.Player;

import java.util.List;

public class ShamanicRitual implements EventStrategy {

    private final int rewardForMost;
    private final int penaltyForFewest;

    public ShamanicRitual(int rewardForMost, int penaltyForFewest) {
        this.rewardForMost = rewardForMost;
        this.penaltyForFewest = penaltyForFewest;
    }

    @Override
    public void apply(List<Player> players) {
        if (players.isEmpty()) return;

        int[] effective = new int[players.size()];

        for (int i = 0; i < players.size(); i++) {
            effective[i] = computeEffectiveIcons(players.get(i));
        }

        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;

        for (int icons : effective) {
            max = Math.max(max, icons);
            min = Math.min(min, icons);
        }

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            int icons = effective[i];

            // Reward for all players tied for maximum
            if (icons == max) {

                boolean doubles =
                        player.getTribe().getBuildings().stream()
                                .anyMatch(b ->
                                        b.getEffect().hasShamanicModifier()
                                                && b.getEffect()
                                                .getShamanicModifier()
                                                .doublesMaxReward(player));

                int reward = doubles
                        ? rewardForMost * 2
                        : rewardForMost;

                player.addPrestigePoints(reward);
            }

            // Penalty for all players tied for minimum
            if (icons == min) {

                boolean immune =
                        player.getTribe().getBuildings().stream()
                                .anyMatch(b ->
                                        b.getEffect().hasShamanicModifier()
                                                && b.getEffect()
                                                .getShamanicModifier()
                                                .isImmuneToMinPenalty(player));

                if (!immune) {
                    player.addPrestigePoints(-penaltyForFewest);
                }
            }
        }
    }

    private int computeEffectiveIcons(Player player) {
        int base = player.getTribe().totalShamanIcons();

        int bonus =
                player.getTribe().getBuildings().stream()
                        .filter(b -> b.getEffect().hasShamanicModifier())
                        .mapToInt(b ->
                                b.getEffect()
                                        .getShamanicModifier()
                                        .extraStarIcons(player))
                        .sum();

        return base + bonus;
    }
}