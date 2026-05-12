package it.polimi.it.network.server.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bootstrap component for the leaderboard storage.
 *
 * <p>Performs the steps required by the additional specification at server
 * startup:
 * <ol>
 *   <li>Connects to the MySQL DBMS using the credentials from
 *       {@link DatabaseConfig};</li>
 *   <li>Checks whether the application database (e.g. {@code GC45_Mesos_DB})
 *       exists, creating it empty otherwise;</li>
 *   <li>Ensures the {@code game_results} table is present, creating it on
 *       first run.</li>
 * </ol>
 *
 * <p>The operations rely exclusively on JDBC and on the MySQL connector that
 * is added as a project dependency. The component is idempotent: it is safe
 * to invoke it on every server start.
 */
public final class DatabaseInitializer {

    private static final Logger LOG = Logger.getLogger(DatabaseInitializer.class.getName());

    /** Table that stores one row per (player, game) result. */
    public static final String TABLE_NAME = "game_results";

    /**
     * Schema of the leaderboard table.
     *
     * <p>Stores the data required by the specification (nickname, final
     * score, date of the game, number of players in that game), plus a
     * synthetic primary key and an indexed {@code num_players} column to
     * speed up the per-size leaderboard queries.
     */
    private static final String CREATE_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                    + "  id           BIGINT       NOT NULL AUTO_INCREMENT,"
                    + "  game_uuid    VARCHAR(64)  NOT NULL,"
                    + "  nickname     VARCHAR(64)  NOT NULL,"
                    + "  final_score  INT          NOT NULL,"
                    + "  num_players  INT          NOT NULL,"
                    + "  played_at    DATETIME     NOT NULL,"
                    + "  PRIMARY KEY (id),"
                    + "  KEY idx_num_players_score (num_players, final_score)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    private final DatabaseConfig config;

    public DatabaseInitializer(DatabaseConfig config) {
        this.config = config;
    }

    /**
     * Runs the full initialisation routine.
     *
     * @throws SQLException if the DBMS cannot be reached or the schema cannot
     *                      be created
     */
    public void initialise() throws SQLException {
        ensureDriverLoaded();
        createDatabaseIfMissing();
        createTableIfMissing();
        LOG.info("Leaderboard storage ready: " + config);
    }

    // ── Steps ─────────────────────────────────────────────────────────────────

    /**
     * Loads the MySQL JDBC driver explicitly.
     *
     * <p>Modern drivers register themselves via the {@code ServiceLoader}, but
     * doing the lookup once eagerly produces a much clearer error message if
     * the connector is missing from the classpath.
     */
    private void ensureDriverLoaded() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "MySQL JDBC driver not found on the classpath. "
                            + "Add the 'mysql-connector-j' dependency.", e);
        }
    }

    /**
     * Connects to the DBMS (without selecting any schema) and issues a
     * {@code CREATE DATABASE IF NOT EXISTS} statement.
     */
    private void createDatabaseIfMissing() throws SQLException {
        // Connect to the DBMS itself: the URL has no database appended.
        try (Connection conn = DriverManager.getConnection(
                config.baseUrl(), config.user(), config.password());
             Statement stmt = conn.createStatement()) {

            String sql = "CREATE DATABASE IF NOT EXISTS `" + config.databaseName()
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
            stmt.executeUpdate(sql);
            LOG.info("Verified database '" + config.databaseName() + "' (created if missing).");
        }
    }

    /**
     * Connects to the application database and creates the
     * {@value #TABLE_NAME} table if needed.
     */
    private void createTableIfMissing() throws SQLException {
        try (Connection conn = DriverManager.getConnection(
                config.databaseUrl(), config.user(), config.password());
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(CREATE_TABLE_SQL);
            LOG.info("Verified table '" + TABLE_NAME + "' (created if missing).");
        }
    }

    /**
     * Pings the DBMS without performing any DDL.
     *
     * <p>Useful from tests or diagnostic command-line tools that just need to
     * make sure the configuration is correct.
     *
     * @return {@code true} if a connection could be opened
     */
    public boolean ping() {
        try (Connection conn = DriverManager.getConnection(
                config.baseUrl(), config.user(), config.password())) {
            return conn.isValid(2);
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Database ping failed: " + e.getMessage(), e);
            return false;
        }
    }
}
