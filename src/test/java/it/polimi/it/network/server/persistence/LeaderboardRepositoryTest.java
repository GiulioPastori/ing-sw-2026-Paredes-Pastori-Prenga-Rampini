package it.polimi.it.network.server.persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link LeaderboardRepository}.
 *
 * <p>These tests require a running MySQL instance reachable on
 * {@code localhost:3306}. To avoid breaking CI builds on machines without a
 * DBMS, they are gated by the {@code mesos.it.db} system property:
 * <pre>
 *     mvn test -Dmesos.it.db=true
 * </pre>
 *
 * <p>A dedicated test database (with a name distinct from the production
 * GC45_Mesos_DB) is created at {@link #beforeAll()} and dropped at
 * {@link #afterAll()}. Each test method starts from an empty table.
 *
 * <p>The covered behaviours are:
 * <ul>
 *   <li>Saving a game's results inside a single transaction;</li>
 *   <li>Reading the leaderboard ordered by score (desc) then date (asc);</li>
 *   <li>Computing the rank of a single result with the tie-breaking rule
 *       used by the production query;</li>
 *   <li>Counting the entries for a given game size;</li>
 *   <li>Idempotency of {@link DatabaseInitializer#initialise()}.</li>
 * </ul>
 */
@EnabledIfSystemProperty(named = "mesos.it.db", matches = "true")
@DisplayName("LeaderboardRepository (integration)")
class LeaderboardRepositoryTest {

    /** Dedicated test database — distinct from production. */
    private static final String TEST_DB = "GC45_Mesos_DB_TEST";
    private static DatabaseConfig        config;
    private static LeaderboardRepository repo;

    @BeforeAll
    static void beforeAll() throws SQLException {
        // Build a configuration that targets the dedicated test database
        // (the rest of the connection parameters comes from the standard
        // lookup chain so that CI can override them via -Ddb.user, etc.).
        System.setProperty("db.name", TEST_DB);
        config = DatabaseConfig.load();
        new DatabaseInitializer(config).initialise();
        repo = new LeaderboardRepository(config);
    }

    @BeforeEach
    void truncate() throws SQLException {
        try (Connection conn = DriverManager.getConnection(
                config.databaseUrl(), config.user(), config.password());
             Statement s = conn.createStatement()) {
            s.executeUpdate("TRUNCATE TABLE " + DatabaseInitializer.TABLE_NAME);
        }
    }

    @AfterAll
    static void afterAll() throws SQLException {
        try (Connection conn = DriverManager.getConnection(
                config.baseUrl(), config.user(), config.password());
             Statement s = conn.createStatement()) {
            s.executeUpdate("DROP DATABASE IF EXISTS `" + TEST_DB + "`");
        }
    }

    @Test
    @DisplayName("saveGameResults: persists every entry of a single game")
    void savesAllRows() throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime storedAt = repo.saveGameResults(List.of(
                LeaderboardEntry.of("Alice", 42, 3, now),
                LeaderboardEntry.of("Bob",   30, 3, now),
                LeaderboardEntry.of("Carol", 51, 3, now)
        ), 3);

        assertNotNull(storedAt, "Saving a non-empty game should return the stored timestamp");
        assertEquals(3, repo.countEntries(3),
                "All three rows of the game should be in the table");
    }

    @Test
    @DisplayName("getLeaderboard: orders by score desc then date asc")
    void leaderboardOrdered() throws SQLException {
        LocalDateTime t1 = LocalDateTime.of(2026, 1, 1, 12, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 1, 2, 12, 0);

        repo.saveGameResults(List.of(LeaderboardEntry.of("Alice", 42, 2, t1)), 2);
        repo.saveGameResults(List.of(LeaderboardEntry.of("Bob",   42, 2, t2)), 2);
        repo.saveGameResults(List.of(
                LeaderboardEntry.of("Eve",   99, 2, t2),
                LeaderboardEntry.of("Mallory", 1, 2, t2)
        ), 2);

        List<LeaderboardEntry> board = repo.getLeaderboard(2);
        // Highest score first; for equal scores the earlier game wins
        assertEquals("Eve",   board.get(0).nickname());
        // Alice (42 @ t1) before Bob (42 @ t2)
        assertEquals("Alice", board.get(1).nickname());
        assertEquals("Bob",   board.get(2).nickname());
        assertEquals(1, board.get(0).rank());
        assertEquals(2, board.get(1).rank());
        assertEquals(3, board.get(2).rank());
    }

    @Test
    @DisplayName("getLeaderboard: filters by num_players")
    void leaderboardFilteredBySize() throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        repo.saveGameResults(List.of(LeaderboardEntry.of("A", 10, 2, now)), 2);
        repo.saveGameResults(List.of(LeaderboardEntry.of("B", 20, 3, now)), 3);

        assertEquals(1, repo.getLeaderboard(2).size());
        assertEquals(1, repo.getLeaderboard(3).size());
        assertEquals(0, repo.getLeaderboard(4).size());
    }

    @Test
    @DisplayName("getPlayerRank: matches the index reported by getLeaderboard")
    void rankConsistentWithLeaderboard() throws SQLException {
        LocalDateTime t = LocalDateTime.now();
        LocalDateTime storedAt = repo.saveGameResults(List.of(
                LeaderboardEntry.of("Alice", 30, 2, t),
                LeaderboardEntry.of("Bob",   50, 2, t)
        ), 2);

        int aliceRank = repo.getPlayerRank("Alice", 30, 2, storedAt);
        int bobRank   = repo.getPlayerRank("Bob",   50, 2, storedAt);
        assertEquals(1, bobRank);
        assertEquals(2, aliceRank);
    }

    @Test
    @DisplayName("DatabaseInitializer.initialise() is idempotent")
    void initialiseIdempotent() throws SQLException {
        // Already called once in beforeAll: calling again must not throw and
        // must not lose data.
        repo.saveGameResults(List.of(LeaderboardEntry.of("A", 1, 2,
                LocalDateTime.now())), 2);
        int before = repo.countEntries(2);
        new DatabaseInitializer(config).initialise();
        int after = repo.countEntries(2);
        assertEquals(before, after, "Re-initialising must not truncate data");
    }
}
