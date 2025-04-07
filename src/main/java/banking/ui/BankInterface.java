package banking.ui;

import banking.domain.BankAccount;
import banking.service.BankService;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/**
 * Handles the text-based user interface for the banking system.
 *
 * Displays menus, receives input, and calls the appropriate service-layer methods.
 * All user interaction is managed through this class.
 */
public class BankInterface {

    private Scanner sc;
    private BankService bankService;
    private boolean exitRequested;

    /**
     * Creates a new {@code BankInterface} instance using the provided banking service.
     *
     * @param bankService the service layer used to perform operations
     */
    public BankInterface(BankService bankService) {
        this.sc = new Scanner(System.in);
        this.bankService = bankService;
        this.exitRequested = false;
    }

    /**
     * Starts the main application loop.
     *
     * Displays the initial menu and routes user choices to the correct handlers.
     */
    public void start(){
        System.out.println("====================================");
        System.out.println("      Welcome to Fake Bank");
        System.out.println("Secure. Simple. Personal Banking.");
        System.out.println("====================================\n");

        do {
            printMainMenu();

            String userInput = sc.nextLine();

            switch (userInput) {
                case "1":
                    System.out.println("Enter your full name:");
                    String holderName = sc.nextLine();
                    createAccountText(bankService.createAccount(holderName));
                    break;
                case "2":
                    logInAttempt();
                    break;
                case "0":
                    exitRequested = true;
                    break;
                default:
                    System.out.println("Error. Invalid input.");
            }

            System.out.println(); // Clean visual spacing

        } while (!exitRequested);

        System.out.println("Goodbye!");
    }

    /**
     * Unsurprisingly displays the main menu options to the user.
     */
    public void printMainMenu() {
        System.out.println("--- Main Menu ---");
        System.out.println("1. Create an account");
        System.out.println("2. Log into account");
        System.out.println("0. Exit\n");
    }

    /**
     * Displays the menu options for a logged-in account.
     */
    public void printAccountMenu() {
        System.out.println("\n--- Account Menu ---");
        System.out.println("1. Balance");
        System.out.println("2. Deposit money");
        System.out.println("3. Withdraw money");
        System.out.println("4. Transfer");
        System.out.println("5. Close account");
        System.out.println("6. Log out");
        System.out.println("0. Exit\n");
    }

    /**
     * Displays account creation details to the user, including account number and PIN.
     *
     * @param account the newly created {@code BankAccount}
     */
    public void createAccountText(BankAccount account) {
        // Readable date formatting
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        String formattedDate = account.getCreatedAt().format(formatter);

        System.out.println("\n--- Account Successfully Created ---");
        System.out.printf("Account Holder:  %s%n", account.getHolderName());
        System.out.printf("Account Number:  %s%n", account.getAccountNumber());
        System.out.printf("PIN:             %s%n", account.getPin());
        System.out.printf("Created At:      %s%n", formattedDate);
        System.out.println("------------------------------------");
        System.out.println("Please store your credentials securely.\n");
    }

    /**
     * Prompts the user for login credentials and validates them.
     *
     * If successful, transitions to the logged-in menu.
     */
    public void logInAttempt(){
        System.out.println("\n--- Log In ---");
        System.out.print("Enter your account number: ");
        String cardNumber = sc.nextLine();

        System.out.print("Enter your PIN: ");
        String pin = sc.nextLine();

        if (!bankService.validateLogin(cardNumber, pin)) {
            System.out.println("\nLogin failed: incorrect account number or PIN.\n");
            return;
        }

        System.out.println("\nLogin successful!");
        System.out.println("Welcome back to Fake Bank.\n");
        loggedIn(cardNumber);
    }

    /**
     * Handles the logged-in session for a specific account.
     *
     * Displays the account menu and responds to user actions.
     *
     * @param accountNumber the account currently logged in
     */
    public void loggedIn(String accountNumber){
        while (true) {
            printAccountMenu();

            String userInput = sc.nextLine();

            switch (userInput) {
                case "1":
                    showBalance(accountNumber);
                    break;
                case "2":
                    deposit(accountNumber);
                    break;
                case "3":
                    withdraw(accountNumber);
                    break;
                case "4":
                    transfer(accountNumber);
                    break;
                case "5":
                    closeAccount(accountNumber);
                    return;
                case "6":
                    System.out.println("You have successfully logged out!\n");
                    return;
                case "0":
                    exitRequested = true;
                    return;
                default:
                    System.out.println("Wrong input");
            }
        }
    }

    /**
     * Displays the current balance of the logged-in account.
     * Checking more than once doesn't increase the balance, unfortunately.
     *
     * @param accountNumber the account to check
     */
    public void showBalance(String accountNumber) {
        BigDecimal balance = bankService.getBalance(accountNumber);

        System.out.println("\n--- Account Balance ---");
        System.out.printf("Current Balance: €%,.2f%n", balance);
        System.out.println("------------------------\n");
    }

    /**
     * Prompts the user to enter an amount to deposit.
     * Would belong more in an ATM software than a bank app, but was added for the sake of practice.
     * Validates and processes the input.
     *
     * @param accountNumber the target account
     */
    public void deposit(String accountNumber) {
        System.out.println("\n--- Deposit Funds ---");
        System.out.print("Enter amount to deposit: ");
        String input = sc.nextLine();

        try {
            BigDecimal amount = new BigDecimal(input);
            bankService.deposit(accountNumber, amount);
            System.out.printf("Successfully deposited: €%,.2f%n%n", amount);
        } catch (NumberFormatException e) {
            System.out.println("Error: invalid number format.\n");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage() + "\n");
        }
    }

    /**
     * Handles the process of transferring money from one account to another.
     *
     * Includes validation for receiver, amount, and success/failure handling.
     *
     * @param accountNumber the sender's account
     */

    public void transfer(String accountNumber) {
        System.out.println("\n--- Transfer Funds ---");
        System.out.print("Enter receiver's account number: ");
        String receiverAccount = sc.nextLine();

        try {
            if (bankService.isTransferAllowed(accountNumber, receiverAccount)) {
                System.out.print("Enter amount to transfer: ");
                String amountInput = sc.nextLine();

                try {
                    BigDecimal amount = new BigDecimal(amountInput);

                    if (bankService.transfer(accountNumber, receiverAccount, amount)) {
                        System.out.printf("Successfully transferred €%,.2f to account %s%n%n",
                                amount, receiverAccount);
                    } else {
                        System.out.println("Transfer failed: insufficient funds.\n");
                    }

                } catch (NumberFormatException e) {
                    System.out.println("Error: invalid amount format.\n");
                }
            }
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage() + "\n");
        }
    }

    /**
     * Closes the currently logged-in account and notifies the user.
     * Dear customer, please don't leave us.
     *
     * @param accountNumber the account to close
     */
    public void closeAccount(String accountNumber) {
        bankService.closeAccount(accountNumber);
        System.out.println("\nYour account has been closed. Logging out...\n");
    }

    /**
     * Prompts the user to enter an amount to withdraw from the account.
     * Would belong more in an ATM software than a bank app, but was added for the sake of practice.
     * Validates the input format and available balance, and performs the withdrawal.
     *
     * @param accountNumber the account from which to withdraw funds
     */
    public void withdraw(String accountNumber) {
        System.out.println("\n--- Withdraw Funds ---");
        System.out.print("Enter amount to withdraw: ");
        String input = sc.nextLine();

        try {
            BigDecimal amount = new BigDecimal(input);
            bankService.withdraw(accountNumber, amount);
            System.out.printf("Successfully withdrew: €%,.2f%n%n", amount);
        } catch (NumberFormatException e) {
            System.out.println("Error: invalid number format.\n");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage() + "\n");
        }
    }
}