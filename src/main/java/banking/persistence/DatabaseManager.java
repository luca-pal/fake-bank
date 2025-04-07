package banking.persistence;

import org.sqlite.SQLiteDataSource;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.logging.Logger;

/**
 * Manages all database operations for the banking system.
 * This class handles the creation of the account table, and provides methods
 * for inserting, updating, querying, and deleting account data using SQLite.
 */
public class DatabaseManager {
    private SQLiteDataSource dataSource;
    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());

    public DatabaseManager(String dbFileName) {
        String url = "jdbc:sqlite:" + dbFileName;
        dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);
    }

    /**
     * Initializes the database by creating the 'account' table if it doesn't exist.
     * The table includes account number, PIN, balance, holder name, and creation timestamp.
     * Logs an error if table creation fails.
     */
    public void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS account ("
                    + "id INTEGER PRIMARY KEY, "
                    + "number TEXT NOT NULL, "
                    + "pin TEXT NOT NULL, "
                    + "balance NUMERIC DEFAULT 0.00, "
                    + "holder_name TEXT NOT NULL, "
                    + "created_at TEXT"
                    + ")";
        try (
                Connection con = dataSource.getConnection();
                Statement statement = con.createStatement()
        ) {
            statement.executeUpdate(sql);

            logger.info("Database table 'account' checked or created successfully.");
        } catch (SQLException e) {
            e.printStackTrace();

            logger.severe("Failed to create or verify 'account' table: " + e.getMessage());
        }
    }

    /**
     * Inserts a new bank account into the database.
     *
     * @param accountNumber the unique account number
     * @param pin the associated 4-digit PIN
     * @param holderName the full name of the account holder
     * @param createdAt account creation timestamp
     */
    public void insertAccount(String accountNumber, String pin, String holderName, LocalDateTime createdAt) {
        String sql = "INSERT INTO account (number, pin, holder_name, created_at) VALUES (?, ?, ?, ?)";

        try (
                Connection con = dataSource.getConnection();
                PreparedStatement statement = con.prepareStatement(sql)
        ) {
            statement.setString(1, accountNumber);
            statement.setString(2, pin);
            statement.setString(3, holderName);
            statement.setString(4, createdAt.toString());

            logger.info("Inserted new account into database: " + accountNumber);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();

            logger.severe("Failed to insert account: " + e.getMessage());
        }
    }

    /**
     * Checks whether the given account number exists in the database.
     *
     * @param accountNumber the account number to check
     * @return true if the account exists, false otherwise
     */
    public boolean accountNumberExists(String accountNumber) {
        String sql = "SELECT 1 FROM account WHERE number = ?";

        try (
                Connection con = dataSource.getConnection();
                PreparedStatement statement = con.prepareStatement(sql)
        ) {
            statement.setString(1, accountNumber);
            ResultSet rs = statement.executeQuery();

            return rs.next(); // Returns true if card exists
        } catch (SQLException e) {
            e.printStackTrace();

            logger.severe("Failed to check account existence for account " + accountNumber + ": " + e.getMessage());
        }

        return false;
    }

    /**
     * Validates login credentials by checking if the account number and PIN match an existing record.
     *
     * @param accountNumber the account number entered by the user
     * @param pin the PIN entered by the user
     * @return true if credentials are valid, false otherwise
     */
    public boolean validateLogin(String accountNumber, String pin) {
        String sql = "SELECT 1 FROM account WHERE number = ? AND pin = ?";

        try (
                Connection con = dataSource.getConnection();
                PreparedStatement statement = con.prepareStatement(sql)
        ) {
            statement.setString(1, accountNumber);
            statement.setString(2, pin);
            ResultSet rs = statement.executeQuery();

            return rs.next(); // Returns true if a matching card and pin are found
        } catch (SQLException e) {
            e.printStackTrace();

            logger.severe("Login validation failed for account " + accountNumber + ": " + e.getMessage());
        }

        return false;
    }

    /**
     * Retrieves the current balance of the specified account.
     *
     * @param accountNumber the account number to look up
     * @return the account balance as a BigDecimal, or zero if not found (only when loggedIn, so card must exist)
     */
    public BigDecimal getBalance(String accountNumber) {
        String sql = "SELECT balance FROM account WHERE number = ?";

        try (
                Connection con = dataSource.getConnection();
                PreparedStatement statement = con.prepareStatement(sql)
        ) {
            statement.setString(1, accountNumber);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                return rs.getBigDecimal("balance");
            }
        } catch (SQLException e) {
            e.printStackTrace();

            logger.severe("Failed to retrieve balance for " + accountNumber + ": " + e.getMessage());
        }

        return BigDecimal.ZERO; // Fallback, login phase guarantees that the card exists
    }

    /**
     * Updates the balance of an account by the specified amount.
     *
     * @param accountNumber the account to update
     * @param amount the amount to add (positive) or subtract (negative)
     */
    public void updateBalance(String accountNumber, BigDecimal amount) {
        String sql = "UPDATE account SET balance = balance + ? WHERE number = ?";

        try (
                Connection con = dataSource.getConnection();
                PreparedStatement statement = con.prepareStatement(sql)
        ) {
            statement.setBigDecimal(1, amount);
            statement.setString(2, accountNumber);
            statement.executeUpdate();

            logger.info("Updated balance by " + amount + " for account " + accountNumber);
        } catch (SQLException e) {
            e.printStackTrace();

            logger.severe("Failed to update balance for account " + accountNumber + ": " + e.getMessage());
        }
    }

    /**
     * Transfers money between two accounts in a single transaction.
     *
     * @param senderAccount the account sending money
     * @param receiverAccount the account receiving money
     * @param amount the amount to transfer
     */
    public void transfer(String senderAccount, String receiverAccount, BigDecimal amount) {
        String sender = "UPDATE account SET balance = balance - ? WHERE number = ?";
        String receiver = "UPDATE account SET balance = balance + ? WHERE number = ?";

        try (
                Connection con = dataSource.getConnection();
                PreparedStatement senderStatement = con.prepareStatement(sender);
                PreparedStatement receiverStatement = con.prepareStatement(receiver)
        ) {
            con.setAutoCommit(false);

            senderStatement.setBigDecimal(1, amount);
            senderStatement.setString(2, senderAccount);
            senderStatement.executeUpdate();

            receiverStatement.setBigDecimal(1, amount);
            receiverStatement.setString(2, receiverAccount);
            receiverStatement.executeUpdate();

            con.commit();

            logger.info("Transfer committed: " + amount + " from " + senderAccount + " to " + receiverAccount);
        } catch (SQLException e) {
            e.printStackTrace();

            logger.severe("Transfer failed between " + senderAccount + " and " +
                    receiverAccount + ": " + e.getMessage());
        }
    }

    /**
     * Permanently deletes the specified account from the database.
     *
     * @param accountNumber the account to be closed
     */
    public void closeAccount(String accountNumber) {
        String sql = "DELETE FROM account WHERE number = ?";

        try (
                Connection con = dataSource.getConnection();
                PreparedStatement statement = con.prepareStatement(sql)
        ) {
            statement.setString(1, accountNumber);
            statement.executeUpdate();

            logger.info("Account deleted from database: " + accountNumber);
        } catch (SQLException e) {
            e.printStackTrace();

            logger.severe("Failed to delete account " + accountNumber + ": " + e.getMessage());
        }
    }
}
