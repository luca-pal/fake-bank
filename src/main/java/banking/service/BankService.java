package banking.service;

import banking.domain.BankAccount;
import banking.persistence.DatabaseManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Provides banking operations and business logic for the system.
 *
 * This service layer handles account creation, login validation,
 * deposits, withdrawals, balance retrieval, transfers, and account closure.
 * It communicates with the {@code DatabaseManager} for persistence operations.
 */
public class BankService {
    private Random ran;
    private DatabaseManager dbManager;
    private static final Logger logger = Logger.getLogger(BankService.class.getName());
    private static final String BIN_PREFIX = "400000";
    private static final int PIN_LENGTH = 4;
    private static final int ACCOUNT_IDENTIFIER_LENGTH = 9;

    /**
     * Creates a new {@code BankService} with the given database manager.
     *
     * @param dbManager the database manager used for persistence operations
     */
    public BankService(DatabaseManager dbManager) {
        this.ran = new Random();
        this.dbManager = dbManager;
    }

    /**
     * Creates a new account with a unique number and PIN.
     * Stores the account in the database and returns the created object.
     *
     * @param holderName the full name of the account holder
     * @return the newly created {@code BankAccount} object
     */
    public BankAccount createAccount(String holderName) {
        String newAccountNumber = generateUniqueAccountNumber();
        String newPin = createPin();
        LocalDateTime createdAt = LocalDateTime.now();

        dbManager.insertAccount(newAccountNumber, newPin, holderName, createdAt);

        logger.info("Created new account for " + holderName + " (accountNumber=" + newAccountNumber + ")");

        return new BankAccount(newAccountNumber, newPin, holderName, createdAt);
    }

    /**
     * Generates a new, valid, and unique 16-digit account number.
     * Uses a fixed BIN prefix and appends a Luhn checksum digit at the end.
     *
     * @return a valid, unused account number
     */
    public String generateUniqueAccountNumber() {
        StringBuilder accountBuilder = new StringBuilder();

        // Account number should be unique and have a 16-digit length
        do {

            // Clear StringBuilder at each iteration
            accountBuilder.setLength(0);

            // Our bank BIN must be 400000
            accountBuilder.append(BIN_PREFIX);

            // Generate random 9-digit account identifier
            for (int i = 0; i < ACCOUNT_IDENTIFIER_LENGTH; i++) {
                accountBuilder.append(ran.nextInt(10));
            }

            // Calculate and add checksum with Luhn algorithm
            accountBuilder.append(generateLuhnChecksum(accountBuilder.toString()));

        } while (dbManager.accountNumberExists(accountBuilder.toString())); //avoid duplicated account numbers

        return accountBuilder.toString();
    }

    /**
     * Generates a random 4-digit PIN.
     *
     * @return a numeric PIN as a {@code String}
     */
    public String createPin() {
        StringBuilder pinNumber = new StringBuilder();

        // 4-digit pin
        for (int i = 0; i < PIN_LENGTH; i++) {
            pinNumber.append(ran.nextInt(10));
        }

        return pinNumber.toString();
    }

    /**
     * Validates whether the given account number and PIN match an existing account.
     *
     * @param accountNumber the account number entered by the user
     * @param pin the PIN entered by the user
     * @return true if the credentials are correct, false otherwise
     */
    public boolean validateLogin(String accountNumber, String pin) {
        if (!dbManager.validateLogin(accountNumber, pin)) {
            logger.warning("Incorrect login attempt for account " + accountNumber);
            return false;
        }

        return true;
    }

    /**
     * Retrieves the current balance of the specified account.
     *
     * @param accountNumber the account to check
     * @return the account balance as a {@code BigDecimal}
     */
    public BigDecimal getBalance(String accountNumber) {
        return dbManager.getBalance(accountNumber);
    }

    /**
     * Calculates the Luhn checksum digit for a given account number prefix.
     *
     * This method is used to ensure account numbers are valid per the Luhn algorithm,
     * which helps catch input errors.
     *
     * @param partialAccountNumber the 15-digit account number prefix (without the checksum digit)
     * @return the final checksum digit as an integer (0–9)
     */
    private int generateLuhnChecksum(String partialAccountNumber) {
        String[] numbers = partialAccountNumber.split("");
        int[] temp = new int[numbers.length];
        int sum = 0;
        int checkSum = 0;

        // Convert String array to int array, could have used stream
        for (int i = 0; i < numbers.length; i++) {
            temp[i] = Integer.parseInt(numbers[i]);
        }

        // Multiply odd digits by 2 and subtract 9 if >9
        for (int i = 0; i < temp.length; i += 2) {
            temp[i] *= 2;
            temp[i] = temp[i] > 9 ? temp[i] - 9 : temp[i];
        }

        // Sum all digits
        for (int digit : temp) {
            sum += digit;
        }

        // Search for x -> (sum + x) % 10 = 0
        for (int i = 0; i < 10; i++) {
            if ((sum + i) % 10 == 0) {
                checkSum = i;
                break;
            }
        }

        // Shorter, less readable alternative:
        // checkSum = (10 - (sum % 10)) % 10;

        return checkSum;
    }

    /**
     * Deposits a specified amount into the given account.
     *
     * @param accountNumber the target account
     * @param amount the amount to deposit (must be positive)
     * @throws IllegalArgumentException if the amount is non-positive
     */
    public void deposit(String accountNumber, BigDecimal amount) throws IllegalArgumentException{
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        dbManager.updateBalance(accountNumber, amount);

        logger.info("Deposited " + amount + " into account " + accountNumber);
    }

    /**
     * Validates whether a transfer from one account to another is allowed.
     * Checks that the receiver exists, is different from sender, and passes the Luhn check.
     *
     * @param accountNumber the sender's account number
     * @param receiverAccount the receiver's account number
     * @return true if the transfer is allowed
     * @throws IllegalArgumentException for invalid conditions (same account, bad checksum, non-existent)
     */
    public boolean isTransferAllowed(String accountNumber, String receiverAccount) throws IllegalArgumentException{
        if (accountNumber.equals(receiverAccount)) {
            throw new IllegalArgumentException("Transfer failed: source and destination accounts must be different.");
        }

        if (!isChecksumValid(receiverAccount)) {
            throw new IllegalArgumentException("Transfer failed: invalid account number.");
        }

        if (!dbManager.accountNumberExists(receiverAccount)) {
            throw new IllegalArgumentException("Transfer failed: the destination account does not exist.");
        }

        return true;
    }

    /**
     * Validates the Luhn checksum of the given account number.
     *
     * @param accountNumber the full account number including checksum
     * @return true if the checksum is valid, false otherwise
     */
    public boolean isChecksumValid(String accountNumber) {
        String partial = accountNumber.substring(0, accountNumber.length() - 1);

        char lastChar = accountNumber.charAt(accountNumber.length() - 1);
        int checkDigit = Character.getNumericValue(lastChar);

        return checkDigit == generateLuhnChecksum(partial);
    }

    /**
     * Transfers money from one account to another, if the sender has enough funds.
     *
     * @param accountNumber the sender account
     * @param receiverAccount the recipient account
     * @param amount the amount to transfer
     * @return true if the transfer was successful, false if insufficient funds
     */
    public boolean transfer(String accountNumber, String receiverAccount, BigDecimal amount) {
        if (dbManager.getBalance(accountNumber).compareTo(amount) < 0) {
            logger.warning("Transfer failed: insufficient funds in " + accountNumber +
                    " to send " + amount + " to " + receiverAccount);

            return false;
        }

        dbManager.transfer(accountNumber, receiverAccount, amount);
        logger.info("Transferred " + amount + " from " + accountNumber +
                " to " + receiverAccount);

        return true;
    }

    /**
     * Closes the specified account by deleting it from the database.
     *
     * @param accountNumber the account to be closed
     */
    public void closeAccount(String accountNumber) {
        dbManager.closeAccount(accountNumber);

        logger.info("Closed account: " + accountNumber);
    }

    /**
     * Withdraws a specified amount from the given account.
     *
     * @param accountNumber the target account
     * @param amount the amount to withdraw (must be positive and less/equal to balance)
     * @throws IllegalArgumentException if the amount is invalid or insufficient funds
     */
    public void withdraw(String accountNumber, BigDecimal amount) throws IllegalArgumentException {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        BigDecimal currentBalance = dbManager.getBalance(accountNumber);

        if (currentBalance.compareTo(amount) < 0) {
            logger.warning("Withdrawal failed: insufficient funds in account " + accountNumber +
                    " (attempted: " + amount + ", available: " + currentBalance + ")");

            throw new IllegalArgumentException("Insufficient funds");
        }

        dbManager.updateBalance(accountNumber, amount.negate()); // Subtract the amount

        logger.info("Withdrew " + amount + " from account " + accountNumber);
    }
}
