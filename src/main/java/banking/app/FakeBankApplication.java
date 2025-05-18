package banking.app;

import banking.ui.BankInterface;
import banking.service.BankService;
import banking.persistence.DatabaseManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class FakeBankApplication {
    public static void main(String[] args) {

        // Ensure logs directory exists
        new File("logs").mkdirs();

        // Create logger file with logging config
        try {
            LogManager.getLogManager().readConfiguration(new FileInputStream("logging.properties"));
        } catch (IOException e) {
            System.err.println("Could not load logging config: " + e.getMessage());
        }

        // Check command-line arguments to get the database file name after -fileName flag
        String dbFileName = "default.db";
        for (int i = 0; i < args.length - 1; i++) {
            if ("-fileName".equals(args[i])) {
                dbFileName = args[i + 1];
                break;
            }
        }

        Logger logger = Logger.getLogger(FakeBankApplication.class.getName());
        logger.info("Using database file: " + dbFileName);

        DatabaseManager db = new DatabaseManager(dbFileName);
        db.createTableIfNotExists();

        BankService service = new BankService(db);

        BankInterface bankInterface = new BankInterface(service);
        bankInterface.start();
    }
}
