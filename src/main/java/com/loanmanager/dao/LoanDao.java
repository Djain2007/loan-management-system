package com.loanmanager.dao;

import com.loanmanager.model.Loan;
import com.loanmanager.model.LoanStatus;
import com.loanmanager.model.LoanType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** CRUD for the {@code loans} table. Every read joins {@code users} so rows carry the applicant's name. */
public class LoanDao {

    private static final String SELECT = """
            SELECT l.id, l.user_id, CONCAT(u.first_name, ' ', u.last_name) AS applicant_name, u.email AS applicant_email,
                   l.loan_type, l.amount, l.tenure_months, l.interest_rate, l.annual_income, l.purpose, l.status,
                   l.emi_amount, l.total_interest, l.total_payable, l.rejection_reason, l.applied_at, l.decided_at
            FROM loans l
            JOIN users u ON u.id = l.user_id
            """;

    public Optional<Loan> findById(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SELECT + " WHERE l.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<Loan> findByUser(Connection con, int userId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SELECT + " WHERE l.user_id = ? ORDER BY l.id")) {
            ps.setInt(1, userId);
            return queryList(ps);
        }
    }

    /** All loans, optionally restricted to one status ({@code null} = every status). */
    public List<Loan> findAll(Connection con, LoanStatus status) throws SQLException {
        String sql = SELECT + (status == null ? "" : " WHERE l.status = ?") + " ORDER BY l.id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (status != null) {
                ps.setString(1, status.name());
            }
            return queryList(ps);
        }
    }

    public Map<LoanStatus, Integer> countByStatus(Connection con) throws SQLException {
        Map<LoanStatus, Integer> counts = new EnumMap<>(LoanStatus.class);
        for (LoanStatus s : LoanStatus.values()) {
            counts.put(s, 0);
        }
        try (PreparedStatement ps = con.prepareStatement("SELECT status, COUNT(*) FROM loans GROUP BY status");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                counts.put(LoanStatus.valueOf(rs.getString(1)), rs.getInt(2));
            }
        }
        return counts;
    }

    /** Inserts a new PENDING application and returns its id. */
    public int insert(Connection con, Loan loan) throws SQLException {
        String sql = """
                INSERT INTO loans (user_id, loan_type, amount, tenure_months, interest_rate, annual_income, purpose, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, loan.userId());
            ps.setString(2, loan.type().name());
            ps.setBigDecimal(3, loan.amount());
            ps.setInt(4, loan.tenureMonths());
            ps.setBigDecimal(5, loan.interestRate());
            ps.setBigDecimal(6, loan.annualIncome());
            ps.setString(7, loan.purpose());
            ps.setString(8, LoanStatus.PENDING.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public void approve(Connection con, int id, BigDecimal emi, BigDecimal totalInterest, BigDecimal totalPayable)
            throws SQLException {
        String sql = """
                UPDATE loans
                SET status = ?, emi_amount = ?, total_interest = ?, total_payable = ?, decided_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, LoanStatus.APPROVED.name());
            ps.setBigDecimal(2, emi);
            ps.setBigDecimal(3, totalInterest);
            ps.setBigDecimal(4, totalPayable);
            ps.setInt(5, id);
            ps.executeUpdate();
        }
    }

    public void reject(Connection con, int id, String reason) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE loans SET status = ?, rejection_reason = ?, decided_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            ps.setString(1, LoanStatus.REJECTED.name());
            ps.setString(2, reason);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    // ---- helpers -----------------------------------------------------------------------------

    private List<Loan> queryList(PreparedStatement ps) throws SQLException {
        List<Loan> loans = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                loans.add(mapRow(rs));
            }
        }
        return loans;
    }

    private static Loan mapRow(ResultSet rs) throws SQLException {
        return new Loan(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("applicant_name"),
                rs.getString("applicant_email"),
                LoanType.valueOf(rs.getString("loan_type")),
                rs.getBigDecimal("amount"),
                rs.getInt("tenure_months"),
                rs.getBigDecimal("interest_rate"),
                rs.getBigDecimal("annual_income"),
                rs.getString("purpose"),
                LoanStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("emi_amount"),
                rs.getBigDecimal("total_interest"),
                rs.getBigDecimal("total_payable"),
                rs.getString("rejection_reason"),
                UserDao.toLocalDateTime(rs.getTimestamp("applied_at")),
                UserDao.toLocalDateTime(rs.getTimestamp("decided_at")));
    }
}
