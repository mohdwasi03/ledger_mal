package mal.ledger;

import mal.ledger.enums.Currency;
import mal.ledger.util.CommandResult;
import mal.ledger.service.LedgerService;
import mal.ledger.store.EventStore;

import java.math.BigDecimal;
import java.util.Scanner;

public class LedgerRunner {
    public static void main(String[] args) {
        EventStore eventStore = new EventStore();
        LedgerService ledger = new LedgerService(eventStore);

        System.out.println("===  In-Memory Account Ledger(testcase with failed event)  ===");

        runAutomatedSimulation(ledger);

    }

    private static void runAutomatedSimulation(LedgerService ledger) {
        // Accounts tied to User "USER-1" with bootstrap event IDs EA and EB
        printRes(ledger.createAccount("USER-1", "ACC-001", Currency.AED, "EA"));
        printRes(ledger.createAccount("USER-1", "ACC-002", Currency.BHD, "EB"));

        // Day 1
        System.out.println("\n--- DAY 1 ---");
        printRes(ledger.credit("ACC-001", new BigDecimal("1200.00"), 1));
        printRes(ledger.debit("ACC-001", new BigDecimal("950.00"), 1));
        ledger.endOfDayProcessing(1);
        ledger.printDaySummary("ACC-001", 1);
        ledger.printDaySummary("ACC-002", 1);

        // Day 2
        System.out.println("\n--- DAY 2 ---");
        ledger.advanceDay();
        printRes(ledger.authorizeHold("ACC-001", "Auth-A", new BigDecimal("200.00"), 2));
        ledger.endOfDayProcessing(2);
        ledger.printDaySummary("ACC-001", 2);
        ledger.printDaySummary("ACC-002", 2);

        // Day 3
        System.out.println("\n--- DAY 3 ---");
        ledger.advanceDay();
        printRes(ledger.credit("ACC-001", new BigDecimal("400.00"), 3));
        ledger.endOfDayProcessing(3);
        ledger.printDaySummary("ACC-001", 3);
        ledger.printDaySummary("ACC-002", 3);

        // Day 4
        System.out.println("\n--- DAY 4 ---");
        ledger.advanceDay();
        printRes(ledger.settleHold("ACC-001", "Auth-A", new BigDecimal("185.00"), 4));
        printRes(ledger.settleHold("ACC-001", "Auth-Z", new BigDecimal("180.00"), 4)); // Orphan rejection
        ledger.endOfDayProcessing(4);
        ledger.printDaySummary("ACC-001", 4);
        ledger.printDaySummary("ACC-002", 4);

        // Day 5
        System.out.println("\n--- DAY 5 ---");
        ledger.advanceDay();
        printRes(ledger.debit("ACC-001", new BigDecimal("620.00"), 2)); // Retroactive value_date Day 2
        printRes(ledger.authorizeHold("ACC-001", "Auth-B", new BigDecimal("90.00"), 5));

        // BHD 10.000 split into 3 equal installments on ACC-002 using sequential event IDs
        ledger.splitAndCreditInstallments("ACC-002", new BigDecimal("10.000"), 3, 5);

        ledger.endOfDayProcessing(5);
        ledger.printDaySummary("ACC-001", 5);
        ledger.printDaySummary("ACC-002", 5);

        // Day 6
        System.out.println("\n--- DAY 6 ---");
        ledger.advanceDay();
        printRes(ledger.reverseTransaction("ACC-001", "E7", 2, 6, "Duplicate debit reversal"));
        ledger.capitalizeInterest(6);
        ledger.endOfDayProcessing(6);
        ledger.printDaySummary("ACC-001", 6);
        ledger.printDaySummary("ACC-002", 6);

        // Replay all account event streams for User-1
        ledger.printUserEventReplay("USER-1");
    }


    private static void printRes(CommandResult res) {
        System.out.println((res.success() ? "  [SUCCESS] " : "  [ERROR] ") + res.message());
    }
}