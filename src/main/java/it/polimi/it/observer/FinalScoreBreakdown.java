package it.polimi.it.observer;

import java.io.Serial;
import java.io.Serializable;

/**
 * Immutable explanation of one player's final score calculation.
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
