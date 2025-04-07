package banking.domain;

import java.time.LocalDateTime;

/**
 * Represents a bank account in the system.
 *
 * This class is primarily used during account creation to encapsulate
 * initial account data. Future versions may use it more broadly to pass
 * account state between layers.
 */
public class BankAccount {
    private final String accountNumber;
    private final String pin;
    private final String holderName;
    private final LocalDateTime createdAt;

    /**
     * Constructs a new {@code BankAccount} instance.
     *
     * @param accountNumber the unique account number
     * @param pin the 4-digit PIN associated with the account
     * @param holderName the name of the account holder
     * @param createdAt the timestamp when the account was created
     */
    public BankAccount(String accountNumber, String pin, String holderName, LocalDateTime createdAt) {
        this.accountNumber = accountNumber;
        this.pin = pin;
        this.holderName = holderName;
        this.createdAt = createdAt;
    }

    /**
     * @return the account number
     */
    public String getAccountNumber() {
        return this.accountNumber;
    }

    /**
     * @return the account's PIN
     */
    public String getPin() {
        return this.pin;
    }

    /**
     * @return the full name of the account holder
     */
    public String getHolderName() {
        return this.holderName;
    }

    /**
     * @return the timestamp when the account was created
     */
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }
}