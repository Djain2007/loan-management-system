package com.loanmanager.db;

import com.loanmanager.exception.DataAccessException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

/**
 * Owns the JDBC connection details and offers two ways to run work against the database:
 * {@link #execute} for single statements (auto-commit) and {@link #transaction} for
 * multi-step changes that must succeed or fail together.
 */
public final class Database {

    /** A unit of work that receives an open connection. */
    @FunctionalInterface
    public interface SqlWork<T> {
        T run(Connection con) throws SQLException;
    }

    private static final String SCHEMA_FILE = "schema.sql";

    private final String url;
    private final String user;
    private final String password;

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public Connection connect() throws SQLException {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("No suitable driver")) {
                throw new SQLException("MySQL JDBC driver not found on the classpath. "
                        + "Build with Maven, or put mysql-connector-j-*.jar in the lib/ folder.", e);
            }
            throw e;
        }
    }

    /** Creates all tables if they do not exist yet (see {@code schema.sql}). */
    public void initSchema() {
        execute(con -> {
            try (Statement st = con.createStatement()) {
                for (String sql : readSchemaStatements()) {
                    st.executeUpdate(sql);
                }
            }
            return null;
        });
    }

    /** Runs {@code work} on a fresh auto-commit connection. */
    public <T> T execute(SqlWork<T> work) {
        try (Connection con = connect()) {
            return work.run(con);
        } catch (SQLException e) {
            throw new DataAccessException("Database error: " + e.getMessage(), e);
        }
    }

    /** Runs {@code work} inside a transaction; rolls back if it throws anything. */
    public <T> T transaction(SqlWork<T> work) {
        try (Connection con = connect()) {
            con.setAutoCommit(false);
            try {
                T result = work.run(con);
                con.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database error: " + e.getMessage(), e);
        }
    }

    private static String[] readSchemaStatements() throws SQLException {
        try (InputStream in = Database.class.getClassLoader().getResourceAsStream(SCHEMA_FILE)) {
            if (in == null) {
                throw new SQLException(SCHEMA_FILE + " not found on classpath");
            }
            String script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            String withoutComments = script.replaceAll("(?m)^\\s*--.*$", "");
            return Arrays.stream(withoutComments.split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
        } catch (IOException e) {
            throw new SQLException("Could not read " + SCHEMA_FILE, e);
        }
    }
}
