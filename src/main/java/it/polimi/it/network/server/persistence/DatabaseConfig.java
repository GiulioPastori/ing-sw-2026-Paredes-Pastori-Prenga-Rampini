package it.polimi.it.network.server.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Loads and exposes the MySQL configuration for the Mesos leaderboard.
 *
 * <p>The lookup order is:
 * <ol>
 *   <li>JVM system properties ({@code -Ddb.url=...}, {@code -Ddb.user=...},
 *       {@code -Ddb.password=...}, {@code -Ddb.name=...}) — highest priority,
 *       useful for tests and CI;</li>
 *   <li>The file referenced by {@link #DEFAULT_EXTERNAL_PATH} on the
 *       filesystem, if present (typical deployment layout: a sibling
 *       {@code db.properties} next to the server jar);</li>
 *   <li>The {@code /db.properties} resource on the classpath, shipped with
 *       the application;</li>
 *   <li>Hard-coded fallbacks ({@code root}/{@code root} on
 *       {@code jdbc:mysql://localhost:3306/}, database
 *       {@value #DEFAULT_DB_NAME}).</li>
 * </ol>
 *
 * <p>The database name embeds the group identifier (GC45) to avoid
 * collisions with other projects sharing the same DBMS, as required by the
 * additional specification of the assignment.
 */
public final class DatabaseConfig {

    private static final Logger LOG = Logger.getLogger(DatabaseConfig.class.getName());

    /** Default location of an external configuration file (next to the jar). */
    public static final String DEFAULT_EXTERNAL_PATH = "db.properties";

    /** Default database name — embeds the group identifier as required. */
    public static final String DEFAULT_DB_NAME = "GC45_Mesos_DB";

    /** Default base URL of the DBMS (NO database name appended). */
    public static final String DEFAULT_BASE_URL = "jdbc:mysql://localhost:3306/";

    private static final String DEFAULT_USER     = "root";
    private static final String DEFAULT_PASSWORD = "root";

    private final String baseUrl;
    private final String user;
    private final String password;
    private final String databaseName;

    private DatabaseConfig(String baseUrl, String user, String password, String databaseName) {
        this.baseUrl      = baseUrl;
        this.user         = user;
        this.password     = password;
        this.databaseName = databaseName;
    }

    /**
     * Loads the configuration using the lookup order documented in the class
     * Javadoc.
     *
     * @return a fully populated {@code DatabaseConfig}; never {@code null}
     */
    public static DatabaseConfig load() {
        Properties props = new Properties();

        // 1. Load classpath default (lowest priority among files)
        try (InputStream in = DatabaseConfig.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                props.load(in);
                LOG.fine("Loaded /db.properties from classpath");
            }
        } catch (IOException e) {
            LOG.warning("Failed reading classpath db.properties: " + e.getMessage());
        }

        // 2. Override with external file if present
        Path external = Path.of(DEFAULT_EXTERNAL_PATH);
        if (Files.isReadable(external)) {
            try (InputStream in = Files.newInputStream(external)) {
                props.load(in);
                LOG.info("Loaded external configuration: " + external.toAbsolutePath());
            } catch (IOException e) {
                LOG.warning("Failed reading external db.properties: " + e.getMessage());
            }
        }

        // 3. Override with JVM system properties (highest priority)
        overrideFromSystem(props, "db.url");
        overrideFromSystem(props, "db.user");
        overrideFromSystem(props, "db.password");
        overrideFromSystem(props, "db.name");

        String baseUrl  = props.getProperty("db.url",     DEFAULT_BASE_URL);
        String user     = props.getProperty("db.user",    DEFAULT_USER);
        String password = props.getProperty("db.password", DEFAULT_PASSWORD);
        String dbName   = props.getProperty("db.name",    DEFAULT_DB_NAME);

        if (!baseUrl.endsWith("/")) baseUrl = baseUrl + "/";

        return new DatabaseConfig(baseUrl, user, password, dbName);
    }

    private static void overrideFromSystem(Properties props, String key) {
        String value = System.getProperty(key);
        if (value != null && !value.isBlank()) props.setProperty(key, value);
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** @return JDBC URL of the DBMS without any database appended (used to CREATE DATABASE). */
    public String baseUrl() { return baseUrl; }

    /** @return JDBC URL targeting the application database. */
    public String databaseUrl() {
        // useSSL=false and serverTimezone=UTC keep the driver quiet across versions
        return baseUrl + databaseName + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    }

    public String user()         { return user; }
    public String password()     { return password; }
    public String databaseName() { return databaseName; }

    @Override
    public String toString() {
        return "DatabaseConfig{baseUrl='" + baseUrl + "', database='" + databaseName
                + "', user='" + user + "'}";
    }
}
