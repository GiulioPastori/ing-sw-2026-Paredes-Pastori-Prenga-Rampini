package it.polimi.it.observer;

import java.io.Serial;
import java.io.Serializable;

/**
 * Immutable explanation of one player's final score calculation.
 *
 * @param playerName player nickname
 * @param inGamePrestige prestige points earned before end-game bonuses
 * @param builderPoints end-game points from Builders
 * @param inventorPoints end-game points from Inventors
 * @param artistPoints end-game points from Artists
 * @param buildingPoints points printed on acquired buildings
 * @param totalPrestige final prestige total
 * @param food food remaining at game end
 */
public record FinalScoreBreakdown(
        String playerName,
        int inGamePrestige,
        int builderPoints,
        int inventorPoints,
        int artistPoints,
        int buildingPoints,
        int totalPrestige,
        int food
) implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    public int endGameBonus() {
        return builderPoints + inventorPoints + artistPoints + buildingPoints;
    }
}
