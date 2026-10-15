package com.loanmanager.dao;

import com.loanmanager.model.AccountStatus;
import com.loanmanager.model.Role;
import com.loanmanager.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** CRUD for the {@code users} table. Methods take the connection so callers decide the transaction scope. */
public class UserDao {

    private static final String COLUMNS = """
            id, first_name, last_name, mobile, email, location, pan_number, cibil_score,
            username, password_hash, role, status, failed_attempts, created_at, updated_at
            """;

    public Optional<User> findById(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT " + COLUMNS + " FROM users WHERE id = ?")) {
            ps.setInt(1, id);
            return queryOne(ps);
        }
    }

    /** Looks a user up by username or e-mail (both are unique). */
    public Optional<User> findByLogin(Connection con, String identifier) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT " + COLUMNS + " FROM users WHERE username = ? OR email = ?")) {
            ps.setString(1, identifier);
            ps.setString(2, identifier);
            return queryOne(ps);
        }
    }

    public boolean existsByEmail(Connection con, String email) throws SQLException {
        return exists(con, "SELECT 1 FROM users WHERE email = ?", email);
    }

    public boolean existsByPan(Connection con, String pan) throws SQLException {
        return exists(con, "SELECT 1 FROM users WHERE pan_number = ?", pan);
    }

    public boolean existsByUsername(Connection con, String username) throws SQLException {
        return exists(con, "SELECT 1 FROM users WHERE username = ?", username);
    }

    public boolean existsAdmin(Connection con) throws SQLException {
        return exists(con, "SELECT 1 FROM users WHERE role = ?", Role.ADMIN.name());
    }

    /** Case-insensitive substring search across name, e-mail, mobile, username and status. */
    public List<User> search(Connection con, String term) throws SQLException {
        String sql = "SELECT " + COLUMNS + """
                FROM users
                WHERE first_name LIKE ? OR last_name LIKE ? OR email LIKE ?
                   OR mobile LIKE ? OR username LIKE ? OR status LIKE ?
                ORDER BY id
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            String like = "%" + term + "%";
            for (int i = 1; i <= 6; i++) {
                ps.setString(i, like);
            }
            return queryList(ps);
        }
    }

    /** Inserts the user and returns the generated id. Timestamps and id on the argument are ignored. */
    public int insert(Connection con, User u) throws SQLException {
        String sql = """
                INSERT INTO users (first_name, last_name, mobile, email, location, pan_number, cibil_score,
                                   username, password_hash, role, status, failed_attempts)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.firstName());
            ps.setString(2, u.lastName());
            ps.setString(3, u.mobile());
            ps.setString(4, u.email());
            ps.setString(5, u.location());
            ps.setString(6, u.panNumber());
            ps.setObject(7, u.cibilScore());
            ps.setString(8, u.username());
            ps.setString(9, u.passwordHash());
            ps.setString(10, u.role().name());
            ps.setString(11, u.status().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public boolean updateContact(Connection con, int id, String email, String mobile, String location)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE users SET email = ?, mobile = ?, location = ? WHERE id = ?")) {
            ps.setString(1, email);
            ps.setString(2, mobile);
            ps.setString(3, location);
            ps.setInt(4, id);
            return ps.executeUpdate() == 1;
        }
    }

    /** Sets the status and clears the failed-attempt counter, so a re-activated user can log in again. */
    public boolean updateStatus(Connection con, int id, AccountStatus status) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE users SET status = ?, failed_attempts = 0 WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    public void updateFailedAttempts(Connection con, int id, int attempts, AccountStatus status) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE users SET failed_attempts = ?, status = ? WHERE id = ?")) {
            ps.setInt(1, attempts);
            ps.setString(2, status.name());
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public void updatePassword(Connection con, int id, String passwordHash) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE users SET password_hash = ?, failed_attempts = 0 WHERE id = ?")) {
            ps.setString(1, passwordHash);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public boolean delete(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM users WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    // ---- helpers -----------------------------------------------------------------------------

    private boolean exists(Connection con, String sql, String param) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Optional<User> queryOne(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
        }
    }

    private List<User> queryList(PreparedStatement ps) throws SQLException {
        List<User> users = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        }
        return users;
    }

    private static User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("mobile"),
                rs.getString("email"),
                rs.getString("location"),
                rs.getString("pan_number"),
                rs.getObject("cibil_score", Integer.class),
                rs.getString("username"),
                rs.getString("password_hash"),
                Role.valueOf(rs.getString("role")),
                AccountStatus.valueOf(rs.getString("status")),
                rs.getInt("failed_attempts"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at")));
    }

    static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}
