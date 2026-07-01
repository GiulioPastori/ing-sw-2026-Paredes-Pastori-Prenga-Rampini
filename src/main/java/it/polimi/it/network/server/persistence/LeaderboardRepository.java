package it.polimi.it.network.server.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for the leaderboard.
 *
 * <p>Encapsulates every read/write operation against the
 * {@link DatabaseInitializer#TABLE_NAME} table. The class is intentionally
 * stateless apart from the configuration: each public method opens its own
 * connection via {@link DriverManager} and closes it through
 * try-with-resources, so the repository is safe to share across threads.
 *
 * <p>Operations:
 * <ul>
 *   <li>{@link #saveGameResults(List, int)} — persists the rows of one
 *       completed game in a single transaction and returns the timestamp
 *       actually stored;</li>
 *   <li>{@link #getLeaderboard(int)} — returns the full ranked leaderboard
 *       for games of a given size;</li>
 *   <li>{@link #getPlayerRank(String, int, int, LocalDateTime)} — computes
 *       the position of one specific result against all other games with the
 *       same number of players.</li>
 * </ul>
 */
public class LeaderboardRepository {

    private static final Logger LOG = Logger.getLogger(LeaderboardRepository.class.getName());

    private final DatabaseConfig config;

    public LeaderboardRepository(DatabaseConfig config) {
        this.config = config;
    }

    // ── Write ────────────────────────────────────────────────────────────────

    /**
     * Persists the result of a completed game.
     *
     * <p>All rows are inserted inside a single transaction, sharing the same
     * {@code game_uuid} and {@code played_at} timestamp. If the first entry
     * already carries a timestamp, that value is used for the whole game;
     * otherwise the repository generates one. The timestamp is normalised to
     * seconds because the table stores it in a MySQL {@code DATETIME} column
     * without fractional seconds.
     *
     * @param entries    one entry per participant; the {@code rank} field is
     *                   ignored (ranks are recomputed by the queries)
     * @param numPlayers number of players that took part — used to override
     *                   the per-entry value if missing
     * @return the timestamp actually stored for the game, or {@code null} if
     *         {@code entries} is empty
     * @throws SQLException if the database is unreachable or rejects the
     *                      statements
     */
    public LocalDateTime saveGameResults(List<LeaderboardEntry> entries, int numPlayers)
            throws SQLException {
        if (entries == null || entries.isEmpty()) return null;

        String gameUuid  = UUID.randomUUID().toString();
        LocalDateTime playedAt = normaliseTimestamp(entries.getFirst().playedAt());

        String sql = "INSERT INTO " + DatabaseInitializer.TABLE_NAME
                + " (game_uuid, nickname, final_score, num_players, played_at) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = openConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (LeaderboardEntry e : entries) {
                    ps.setString   (1, gameUuid);
                    ps.setString   (2, e.nickname());
                    ps.setInt      (3, e.finalScore());
                    ps.setInt      (4, numPlayers);
                    ps.setTimestamp(5, Timestamp.valueOf(playedAt));
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
                LOG.info("Saved " + entries.size() + " leaderboard entries for game " + gameUuid);
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        return playedAt;
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    /**
     * Returns the full leaderboard for games of the given size, ordered by
     * final score (descending), then by date (ascending — earlier wins break
     * ties), then by nickname for deterministic ties within the same game.
     *
     * @param numPlayers number of players the games of interest had
     * @return one ranked entry per row; never {@code null}
     */
    public List<LeaderboardEntry> getLeaderboard(int numPlayers) throws SQLException {
        String sql = "SELECT nickname, final_score, num_players, played_at "
                + "FROM " + DatabaseInitializer.TABLE_NAME + " "
                + "WHERE num_players = ? "
                + "ORDER BY final_score DESC, played_at ASC, nickname ASC";

        List<LeaderboardEntry> result = new ArrayList<>();
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, numPlayers);
            try (ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    result.add(new LeaderboardEntry(
                            rank++,
                            rs.getString("nickname"),
                            rs.getInt("final_score"),
                            rs.getInt("num_players"),
                            rs.getTimestamp("played_at").toLocalDateTime()));
                }
            }
        }
        return result;
    }

    /**
     * Computes the position (1-based) of a single result among all the games
     * with the same number of players.
     *
     * <p>The query counts how many rows have a strictly higher score, plus
     * how many have the same score but were played strictly earlier; the
     * rank of the result is one more than that count. This avoids loading
     * the full leaderboard just to find a single position.
     *
     * @param nickname    nickname of the player whose result we are placing
     * @param finalScore  score the player obtained in the game of interest
     * @param numPlayers  number of players in the game of interest
     * @param playedAt    timestamp of the game of interest (must match the
     *                    one used when saving)
     * @return the 1-based rank of the result
     */
    public int getPlayerRank(String nickname, int finalScore, int numPlayers,
                             LocalDateTime playedAt) throws SQLException {
        LocalDateTime storedPlayedAt = normaliseTimestamp(playedAt);
        String sql = "SELECT COUNT(*) AS better FROM " + DatabaseInitializer.TABLE_NAME + " "
                + "WHERE num_players = ? AND ("
                + "      final_score > ? "
                + "   OR (final_score = ? AND played_at < ?) "
                + "   OR (final_score = ? AND played_at = ? AND nickname < ?))";

        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt      (1, numPlayers);
            ps.setInt      (2, finalScore);
            ps.setInt      (3, finalScore);
            ps.setTimestamp(4, Timestamp.valueOf(storedPlayedAt));
            ps.setInt      (5, finalScore);
            ps.setTimestamp(6, Timestamp.valueOf(storedPlayedAt));
            ps.setString   (7, nickname);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("better") + 1;
            }
        }
        return -1;
    }

    /**
     * Returns the total number of recorded entries for games of the given
     * size. Useful to show "rank X of Y" on the client.
     */
    public int countEntries(int numPlayers) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM " + DatabaseInitializer.TABLE_NAME
                + " WHERE num_players = ?";
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, numPlayers);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("total");
            }
        }
        return 0;
    }

    /**
     * Counts how many recorded games were won by the given player. Ties count
     * as wins for every player sharing the highest final score in that game.
     */
    public int countWinsForPlayer(String nickname) throws SQLException {
        String sql = "SELECT COUNT(*) AS wins "
                + "FROM " + DatabaseInitializer.TABLE_NAME + " r "
                + "WHERE r.nickname = ? "
                + "AND r.final_score = ("
                + "  SELECT MAX(r2.final_score) "
                + "  FROM " + DatabaseInitializer.TABLE_NAME + " r2 "
                + "  WHERE r2.game_uuid = r.game_uuid"
                + ")";
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nickname);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("wins");
            }
        }
        return 0;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Connection openConnection() throws SQLException {
        try {
            return DriverManager.getConnection(
                    config.databaseUrl(), config.user(), config.password());
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Cannot open DB connection: " + e.getMessage(), e);
            throw e;
        }
    }

    private static LocalDateTime normaliseTimestamp(LocalDateTime timestamp) {
        LocalDateTime value = timestamp == null ? LocalDateTime.now() : timestamp;
        return value.withNano(0);
    }
}
