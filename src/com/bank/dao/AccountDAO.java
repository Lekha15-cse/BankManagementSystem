package com.bank.dao;

import com.bank.model.Account;
import java.sql.*;
import java.math.BigDecimal;

public class AccountDAO {

    public boolean createAccount(Account account) {
        String sql = "INSERT INTO accounts (account_number, customer_name, pin, balance) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, account.getAccountNumber());
            pstmt.setString(2, account.getCustomerName());
            pstmt.setString(3, account.getPin());
            pstmt.setBigDecimal(4, account.getBalance());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Account findByAccountNumber(String accountNumber, String pin) {
        String sql = "SELECT * FROM accounts WHERE account_number = ? AND pin = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accountNumber);
            pstmt.setString(2, pin);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Account(
                        rs.getString("account_number"),
                        rs.getString("customer_name"),
                        rs.getString("pin"),
                        rs.getBigDecimal("balance")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Transaction-safe deposit with logging
    public boolean deposit(String accountNumber, BigDecimal amount) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            String updateSql = "UPDATE accounts SET balance = balance + ? WHERE account_number = ?";
            String logSql = "INSERT INTO transactions (account_number, transaction_type, amount, balance_after) VALUES (?, 'DEPOSIT', ?, (SELECT balance FROM accounts WHERE account_number = ?))";

            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                 PreparedStatement logStmt = conn.prepareStatement(logSql)) {

                updateStmt.setBigDecimal(1, amount);
                updateStmt.setString(2, accountNumber);
                int rows = updateStmt.executeUpdate();

                if (rows > 0) {
                    logStmt.setString(1, accountNumber);
                    logStmt.setBigDecimal(2, amount);
                    logStmt.setString(3, accountNumber);
                    logStmt.executeUpdate();
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Transaction-safe withdraw with logging
    public boolean withdraw(String accountNumber, BigDecimal amount) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            String checkSql = "SELECT balance FROM accounts WHERE account_number = ?";
            String updateSql = "UPDATE accounts SET balance = balance - ? WHERE account_number = ? AND balance >= ?";
            String logSql = "INSERT INTO transactions (account_number, transaction_type, amount, balance_after) VALUES (?, 'WITHDRAWAL', ?, (SELECT balance FROM accounts WHERE account_number = ?))";

            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                 PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                 PreparedStatement logStmt = conn.prepareStatement(logSql)) {

                // Check balance
                checkStmt.setString(1, accountNumber);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getBigDecimal("balance").compareTo(amount) < 0) {
                    return false;
                }

                // Update + Log
                updateStmt.setBigDecimal(1, amount);
                updateStmt.setString(2, accountNumber);
                updateStmt.setBigDecimal(3, amount);
                int rows = updateStmt.executeUpdate();

                if (rows > 0) {
                    logStmt.setString(1, accountNumber);
                    logStmt.setBigDecimal(2, amount);
                    logStmt.setString(3, accountNumber);
                    logStmt.executeUpdate();
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ⭐ NEW: FUND TRANSFER (ACID Transaction)
    public boolean transfer(String fromAccount, String toAccount, BigDecimal amount) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Validate both accounts exist + sufficient balance
            String checkFrom = "SELECT balance FROM accounts WHERE account_number = ?";
            String checkTo = "SELECT id FROM accounts WHERE account_number = ?";

            try (PreparedStatement fromStmt = conn.prepareStatement(checkFrom);
                 PreparedStatement toStmt = conn.prepareStatement(checkTo)) {

                fromStmt.setString(1, fromAccount);
                ResultSet fromRs = fromStmt.executeQuery();
                if (!fromRs.next() || fromRs.getBigDecimal("balance").compareTo(amount) < 0) {
                    conn.rollback();
                    return false;
                }

                toStmt.setString(1, toAccount);
                ResultSet toRs = toStmt.executeQuery();
                if (!toRs.next()) {
                    conn.rollback();
                    return false;
                }
            }

            // 2. Transfer funds atomically
            String withdrawSql = "UPDATE accounts SET balance = balance - ? WHERE account_number = ?";
            String depositSql = "UPDATE accounts SET balance = balance + ? WHERE account_number = ?";

            try (PreparedStatement withdrawStmt = conn.prepareStatement(withdrawSql);
                 PreparedStatement depositStmt = conn.prepareStatement(depositSql)) {

                withdrawStmt.setBigDecimal(1, amount);
                withdrawStmt.setString(2, fromAccount);
                depositStmt.setBigDecimal(1, amount);
                depositStmt.setString(2, toAccount);

                int withdrawRows = withdrawStmt.executeUpdate();
                int depositRows = depositStmt.executeUpdate();

                if (withdrawRows > 0 && depositRows > 0) {
                    // 3. Log transactions for BOTH accounts
                    logTransaction(conn, fromAccount, "TRANSFER_OUT", amount);
                    logTransaction(conn, toAccount, "TRANSFER_IN", amount);
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Helper method for transaction logging
    private void logTransaction(Connection conn, String accountNumber, String type, BigDecimal amount) throws SQLException {
        String sql = "INSERT INTO transactions (account_number, transaction_type, amount, balance_after) VALUES (?, ?, ?, (SELECT balance FROM accounts WHERE account_number = ?))";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accountNumber);
            stmt.setString(2, type);
            stmt.setBigDecimal(3, amount);
            stmt.setString(4, accountNumber);
            stmt.executeUpdate();
        }
    }

    public void showTransactionHistory(String accountNumber) {
        String sql = "SELECT * FROM transactions WHERE account_number = ? ORDER BY transaction_date DESC LIMIT 10";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accountNumber);
            ResultSet rs = pstmt.executeQuery();

            System.out.println("\n=== Recent Transactions (Last 10) ===");
            System.out.println("Type\t\tAmount\t\tBalance After\tDate");
            System.out.println("------------------------------------------------");

            while (rs.next()) {
                String type = rs.getString("transaction_type");
                BigDecimal amount = rs.getBigDecimal("amount");
                BigDecimal balanceAfter = rs.getBigDecimal("balance_after");
                Timestamp date = rs.getTimestamp("transaction_date");

                System.out.printf("%-12s ₹%-10s ₹%-12s %s%n",
                        type, amount, balanceAfter, date);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
