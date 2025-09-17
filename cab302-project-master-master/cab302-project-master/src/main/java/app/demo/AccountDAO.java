package app.demo;

import java.sql.*;

public class    AccountDAO implements LoginDetailsDAO {

    public static void initialiseDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS accounts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE NOT NULL, " +
                "password TEXT NOT NULL)";
        Connection connection = SqLiteConnection.getInstance();
        try (
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

        @Override
    public void CreateAccount(Account account) {
        try{
        String sql = "INSERT INTO accounts(username, password) VALUES(?, ?)";
        Connection connection = SqLiteConnection.getInstance();
        PreparedStatement statement = connection.prepareStatement(sql);

        statement.setString(1, account.getUsername());
        statement.setString(2, account.getPasswordHash());
        statement.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean ConfrimLogin(String username, String password) {
        String hashedPassword = passwordEncryption.hashPassword(password);
        String sql = "SELECT COUNT(*) FROM accounts WHERE username = ? AND password = ?";
        Connection connection = SqLiteConnection.getInstance();
        try(
            PreparedStatement statement = connection.prepareStatement(sql)){
            statement.setString(1, username);
            statement.setString(2, hashedPassword);
            ResultSet result  = statement.executeQuery();
            return result.next() && result.getInt(1) > 0;

            } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Account getAccount(String username, String password) {
        String hashedPassword = passwordEncryption.hashPassword(password);
        String sql = "SELECT username, password FROM accounts WHERE username = ? AND password =?";
        Connection connection = SqLiteConnection.getInstance();
        try(
            PreparedStatement statement = connection.prepareStatement(sql)){
            statement.setString(1, username);
            statement.setString(2, hashedPassword);

            ResultSet result = statement.executeQuery();

            if (result.next()){
                return new Account(result.getString("username"), result.getString("password"), true);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;

        }
    }
