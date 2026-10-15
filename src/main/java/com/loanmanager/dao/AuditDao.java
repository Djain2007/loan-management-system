package com.loanmanager.dao;

import com.loanmanager.model.AuditEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Append-only audit trail of admin actions and loan decisions. */
public class AuditDao {

    public void log(Connection con, int actorId, String action, String target, String details) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO audit_logs (actor_id, action, target, details) VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, actorId);
            ps.setString(2, action);
            ps.setString(3, target);
            ps.setString(4, details);
            ps.executeUpdate();
        }
    }

    /** Most recent entries first. */
    public List<AuditEntry> findRecent(Connection con, int limit) throws SQLException {
        String sql = """
                SELECT a.id, COALESCE(u.username, '(deleted)') AS actor, a.action, a.target, a.details, a.created_at
                FROM audit_logs a
                LEFT JOIN users u ON u.id = a.actor_id
                ORDER BY a.id DESC
                LIMIT ?
                """;
        List<AuditEntry> entries = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new AuditEntry(
                            rs.getInt("id"),
                            rs.getString("actor"),
                            rs.getString("action"),
                            rs.getString("target"),
                            rs.getString("details"),
                            UserDao.toLocalDateTime(rs.getTimestamp("created_at"))));
                }
            }
        }
        return entries;
    }
}
