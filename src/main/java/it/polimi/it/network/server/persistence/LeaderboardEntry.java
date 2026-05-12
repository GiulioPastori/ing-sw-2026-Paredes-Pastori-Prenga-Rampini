package it.polimi.it.network.server.persistence;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Immutable row of the leaderboard.
 *
 * <p>Represents one (player, game) result as stored in the database: it
 * carries all the fields required by the specification — nickname, final
 * score, date of the game and number of players — plus a {@code rank} field
 * computed on the fly when the row is returned as part of a ranked
 * leaderboard query.
 *
 * <p>Implements {@link Serializable} so it can be ferried over both Sockets
 * (Java object serialisation is not used by this project, but the contract
 * is kept consistent with the other DTOs) and inside the JSON payload of
 * {@code Message}.
 *
 * @param rank        1-based position in the leaderboard (0 if the row was
 *                    produced outside a ranked query)
 * @param nickname    nickname of the player
 * @param finalScore  prestige points at the end of the game
 * @param numPlayers  number of players that took part in the game
 * @param playedAt    when the game was completed
 */
public record LeaderboardEntry(
        int           rank,
        String        nickname,
        int           finalScore,
        int           numPlayers,
        LocalDateTime playedAt
) implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    /** Convenience factory for rows that do not carry a rank yet. */
    public static LeaderboardEntry of(String nickname, int finalScore,
                                      int numPlayers, LocalDateTime playedAt) {
        return new LeaderboardEntry(0, nickname, finalScore, numPlayers, playedAt);
    }

    /** Returns a copy of this entry with the supplied rank set. */
    public LeaderboardEntry withRank(int rank) {
        return new LeaderboardEntry(rank, nickname, finalScore, numPlayers, playedAt);
    }
}
