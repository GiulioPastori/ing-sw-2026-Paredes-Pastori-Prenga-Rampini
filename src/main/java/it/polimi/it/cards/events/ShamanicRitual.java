package it.polimi.it.cards.events;

import it.polimi.it.player.Player;

import java.util.List;

/**
 * Strategy for the Shamanic Ritual event.
 *
 * The player(s) with the most star icons gain the prestige points
 * indicated on the event card.
 *
 * The player(s) with the fewest star icons lose the prestige points
 * indicated on the event card.
 *
 * In case of a tie, all tied players gain or lose the indicated points.
 *
 * Building effects:
 * - 103: immunity to the minimum penalty.
 * - 201: doubles the reward gained from the ritual.
 * - 207: grants 3 virtual star icons.
 */
public class ShamanicRitual implements EventStrategy {

    private final int rewardForMost;
    private final int penaltyForFewest;

    public ShamanicRitual(int rewardForMost, int penaltyForFewest) {
        this.rewardForMost = rewardForMost;
        this.penaltyForFewest = penaltyForFewest;
    }

    @Override
    public void apply(List<Player> players) {

        if (players.isEmpty()) {
            return;
        }

        int[] effectiveIcons = new int[players.size()];

        for (int i = 0; i < players.size(); i++) {
            effectiveIcons[i] = computeEffectiveIcons(players.get(i));
        }

        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;

        for (int icons : effectiveIcons) {
            max = Math.max(max, icons);
            min = Math.min(min, icons);
        }

        for (int i = 0; i < players.size(); i++) {

            Player player = players.get(i);
            int icons = effectiveIcons[i];

            // Players with the most icons gain prestige points
            if (icons == max) {

                boolean doubleReward =
                        player.getTribe().getBuildings().stream()
                                .filter(b -> b.getEffect().hasShamanicModifier())
                                .anyMatch(b ->
                                        b.getEffect()
                                                .getShamanicModifier()
                                                .doublesMaxReward(player));

                int reward = doubleReward
                        ? rewardForMost * 2
                        : rewardForMost;

                player.addPrestigePoints(reward);
            }

            // Players with the fewest icons lose prestige points
            if (max != min && icons == min) {

                boolean immune =
                        player.getTribe().getBuildings().stream()
                                .filter(b -> b.getEffect().hasShamanicModifier())
                                .anyMatch(b ->
                                        b.getEffect()
                                                .getShamanicModifier()
                                                .isImmuneToMinPenalty(player));

                if (!immune) {
                    player.addPrestigePoints(-penaltyForFewest);
                }
            }
        }
    }

    /**
     * Computes the effective number of star icons for the ritual,
     * including building modifiers.
     */
    private int computeEffectiveIcons(Player player) {

        int baseIcons = player.getTribe().totalShamanIcons();

        int bonusIcons =
                player.getTribe().getBuildings().stream()
                        .filter(b -> b.getEffect().hasShamanicModifier())
                        .mapToInt(b ->
                                b.getEffect()
                                        .getShamanicModifier()
                                        .extraStarIcons(player))
                        .sum();

        return baseIcons + bonusIcons;
    }
}