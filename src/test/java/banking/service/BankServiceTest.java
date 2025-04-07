package banking.service;

import banking.persistence.DatabaseManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BankServiceTest {

    private BankService service;
    private DatabaseManager mockDb;

    @BeforeEach
    public void setup() {
        mockDb = mock(DatabaseManager.class);
        service = new BankService(mockDb);
    }

    @Test
    public void deposit_shouldRejectNegativeAmounts() {
        String accountNumber = "4000001234567890";
        BigDecimal negative = new BigDecimal("-100.00");

        // Expect an exception when trying to deposit a negative amount
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.deposit(accountNumber, negative)
        );

        // Check that the exception message is accurate
        assertEquals("Amount must be positive", ex.getMessage());

        // Verify that the DB was never called
        verify(mockDb, never()).updateBalance(any(), any());
    }

    @Test
    public void deposit_shouldAcceptValidAmount() {
        String accountNumber = "4000001234567890";
        BigDecimal amount = new BigDecimal("100.00");

        // No exception
        assertDoesNotThrow(() -> service.deposit(accountNumber, amount));

        // Verify that the DB was called once
        verify(mockDb, times(1)).updateBalance(accountNumber, amount);
    }

    @Test
    public void withdraw_shouldRejectIfBalanceTooLow() {
        String accountNumber = "4000001234567890";
        BigDecimal balance = new BigDecimal("50.00");
        BigDecimal attempt = new BigDecimal("100.00");

        // Mock the balance information
        when(mockDb.getBalance(accountNumber)).thenReturn(balance);

        // Expect an exception when trying to withdraw more than balance
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.withdraw(accountNumber, attempt)
        );

        // Check exception message
        assertEquals("Insufficient funds", ex.getMessage());

        // Verify that the DB was never called
        verify(mockDb, never()).updateBalance(any(), any());
    }

    @Test
    public void withdraw_shouldRejectNegativeAmount() {
        String accountNumber = "4000001234567890";
        BigDecimal negativeAmount = new BigDecimal("-100.00");

        // Expect an exception when trying to withdraw negative amount
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.withdraw(accountNumber, negativeAmount)
        );

        // Check exception message
        assertEquals("Amount must be positive", ex.getMessage());

        // Verify that the DB was never called
        verify(mockDb, never()).updateBalance(any(), any());
    }

    @Test
    public void withdraw_shouldSucceedWithSufficientBalance() {
        String accountNumber = "4000001234567890";
        BigDecimal balance = new BigDecimal("150.00");
        BigDecimal withdrawal = new BigDecimal("100.00");

        // Mock the balance information
        when(mockDb.getBalance(accountNumber)).thenReturn(balance);

        // No exception
        assertDoesNotThrow(() -> service.withdraw(accountNumber, withdrawal));

        // Verify that the DB was called once
        verify(mockDb, times(1)).updateBalance(accountNumber, withdrawal.negate());
    }

    @Test
    public void isTransferAllowed_shouldRejectIfReceiverIsSameAccount() {
        String accountNumber = "4000001234567890";

        // Expect an exception when trying to transfer to the same account number
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.isTransferAllowed(accountNumber, accountNumber)
        );

        // Check exception message
        assertEquals("Transfer failed: source and destination accounts must be different.", ex.getMessage());

        // Verify that the DB was never called
        verify(mockDb, never()).accountNumberExists(any());
    }

    @Test
    public void isTransferAllowed_shouldRejectIfChecksumIsInvalid() {
        String senderAccount = "4000001234567890";
        String invalidChecksumAccount = "4000001234567891"; // Different last digit to make it invalid

        // Expect an exception when last digit is not a correct checksum
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.isTransferAllowed(senderAccount, invalidChecksumAccount)
        );

        // Check exception message
        assertEquals("Transfer failed: invalid account number.", ex.getMessage());

        // Verify that the DB was never called
        verify(mockDb, never()).accountNumberExists(any());
    }

    @Test
    public void isTransferAllowed_shouldRejectIfReceiverDoesNotExist() {
        String senderAccount = "4000001234567890";
        String receiverAccount = "4000001234567899"; // Valid number

        when(mockDb.accountNumberExists(receiverAccount)).thenReturn(false);

        // Expect an exception when account number does not exist in DB
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.isTransferAllowed(senderAccount, receiverAccount)
        );

        // Check exception message
        assertEquals("Transfer failed: the destination account does not exist.", ex.getMessage());

        // Verify that the DB was called once
        verify(mockDb, times(1)).accountNumberExists(receiverAccount);
    }

    @Test
    public void isTransferAllowed_shouldSucceedForValidReceiver() {
        String senderAccount = "4000001234567890";
        String receiverAccount = "4000001234567899"; // Valid number

        when(mockDb.accountNumberExists(receiverAccount)).thenReturn(true);

        // expect no exception
        boolean result = assertDoesNotThrow(() ->
                service.isTransferAllowed(senderAccount, receiverAccount)
        );

        // expect true output and verify that the DB was called once
        assertTrue(result);
        verify(mockDb, times(1)).accountNumberExists(receiverAccount);
    }

    @Test
    public void transfer_shouldRejectWhenInsufficientFunds() {
        String sender = "4000001234567890";
        String receiver = "4000001234567899";
        BigDecimal balance = new BigDecimal("50.00");
        BigDecimal amount = new BigDecimal("100.00");

        // Mock the balance information
        when(mockDb.getBalance(sender)).thenReturn(balance);

        boolean result = service.transfer(sender, receiver, amount);

        // Expect false output and verify that there was no transfer in the DB
        assertFalse(result);
        verify(mockDb, never()).transfer(any(), any(), any());
    }

    @Test
    public void transfer_shouldSucceedWhenBalanceIsSufficient() {
        String sender = "4000001234567890";
        String receiver = "4000001234567899";
        BigDecimal balance = new BigDecimal("200.00");
        BigDecimal amount = new BigDecimal("100.00");

        // Mock the balance information
        when(mockDb.getBalance(sender)).thenReturn(balance);

        boolean result = service.transfer(sender, receiver, amount);

        // Expect true output and verify transfer in DB was called
        assertTrue(result);
        verify(mockDb, times(1)).transfer(sender, receiver, amount);
    }
}
