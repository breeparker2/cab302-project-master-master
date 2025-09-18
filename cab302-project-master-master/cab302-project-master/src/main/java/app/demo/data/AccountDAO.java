package app.demo.data;

import app.demo.LoginDetailsDAO;
import app.demo.passwordEncryption;

import java.sql.*;

public class AccountDAO implements LoginDetailsDAO {

    // Create table if missing
    public static void initialiseDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS accounts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE NOT NULL, " +
                "password TEXT NOT NULL)";
        Connection connection = SqLiteConnection.getInstance();
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    // ---------- Check username (username-only) ----------
    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM accounts WHERE username = ? LIMIT 1";
        Connection connection = SqLiteConnection.getInstance(); // do not close
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ---------- Create using RAW password ----------
    // Returns true if the row was inserted, false otherwise
    public boolean register(String username, String rawPassword) {
        String sql = "INSERT INTO accounts(username, password) VALUES(?, ?)";
        String hashed = passwordEncryption.hashPassword(rawPassword);
        Connection connection = SqLiteConnection.getInstance(); // do not close
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hashed);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) return false;
            e.printStackTrace();
            return false;
        }
    }

    // ---------- Create with Account object ----------
    public boolean tryCreateAccount(Account account) {
        String sql = "INSERT INTO accounts(username, password) VALUES(?, ?)";
        Connection connection = SqLiteConnection.getInstance(); // do not close
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, account.getUsername());
            ps.setString(2, account.getPasswordHash());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) return false;
            e.printStackTrace();
            return false;
        }
    }

    // Keep interface contract; delegate to boolean version
    @Override
    public void CreateAccount(Account account) {
        tryCreateAccount(account);
    }

    // ---------- Confirm login ----------
    @Override
    public boolean ConfrimLogin(String username, String password) {
        String hashedPassword = passwordEncryption.hashPassword(password);
        String sql = "SELECT COUNT(*) FROM accounts WHERE username = ? AND password = ?";
        Connection connection = SqLiteConnection.getInstance(); // do not close
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, hashedPassword);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getInt(1) > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // ---------- Get account ----------
    @Override
    public Account getAccount(String username, String password) {
        String hashedPassword = passwordEncryption.hashPassword(password);
        String sql = "SELECT username, password FROM accounts WHERE username = ? AND password = ?";
        Connection connection = SqLiteConnection.getInstance(); // do not close
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, hashedPassword);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return new Account(
                            result.getString("username"),
                            result.getString("password"),
                            true // already hashed flag
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
