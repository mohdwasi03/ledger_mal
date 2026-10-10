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
        Scanner scanner = new Scanner(System.in);

        System.out.println("===  In-Memory Account Ledger  ===");
//        System.out.println("Select execution mode:");
//        System.out.println("  1. Run Automated 6-Day Simulation");
//        System.out.println("  2. Enter Interactive Command Mode");
//        System.out.print("Enter choice (1 or 2): ");
//
//        String choice = scanner.nextLine().trim();
//
//        if (choice.equals("1")) {
//            System.out.println("\n=== Running Automated Bitemporal 6-Day Simulation ===");
//            runAutomatedSimulation(ledger);
//
//        } else if (choice.equals("2")) {
//            System.out.println("\n=== Entering Interactive Command Mode ===");
//            runInteractiveMode(ledger);
//        } else {
//            System.out.println("Invalid choice. Exiting application.");
//        }

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

//    private static void runInteractiveMode(LedgerService ledger) {
//        Scanner scanner = new Scanner(System.in);
////        System.out.println("\nInteractive Commands:");
////        System.out.println("  create <userId> <accountId> <AED|BHD> <bootstrapEventId>");
////        System.out.println("  credit <accountId> <amount> <valueDay>");
////        System.out.println("  debit <accountId> <amount> <valueDay>");
////        System.out.println("  auth <accountId> <authId> <amount> <valueDay>");
////        System.out.println("  settle <accountId> <authId> <amount> <valueDay>");
////        System.out.println("  reverse <accountId> <targetEventId> <valueDay> <systemDay> <reason>");
////        System.out.println("  advance");
////        System.out.println("  user-replay <userId>");
////        System.out.println("  exit");
//
//        while (true) {
//            System.out.print("\n[Day " + ledger.getCurrentDay() + "] ledger> ");
//            String line = scanner.nextLine().trim();
//            if (line.equalsIgnoreCase("exit")) break;
//            String[] parts = line.split("\\s+");
//            if (parts.length == 0) continue;
//
//            try {
//                switch (parts[0].toLowerCase()) {
//                    case "advance" -> {
//                        ledger.advanceDay();
//                        System.out.println("Advanced system day to: " + ledger.getCurrentDay());
//                    }
//                    case "create" -> printRes(ledger.createAccount(parts[1], parts[2], Currency.valueOf(parts[3]), parts[4]));
//                    case "user-replay" -> ledger.printUserEventReplay(parts[1]);
//                    case "credit" -> printRes(ledger.credit(parts[1], new BigDecimal(parts[2]), Integer.parseInt(parts[3])));
//                    case "debit" -> printRes(ledger.debit(parts[1], new BigDecimal(parts[2]), Integer.parseInt(parts[3])));
//                    case "auth" -> printRes(ledger.authorizeHold(parts[1], parts[2], new BigDecimal(parts[3]), Integer.parseInt(parts[4])));
//                    case "settle" -> printRes(ledger.settleHold(parts[1], parts[2], new BigDecimal(parts[3]), Integer.parseInt(parts[4])));
//                    case "reverse" -> printRes(ledger.reverseTransaction(parts[1], parts[2], Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), parts[5]));
//                    case "balance" -> {
//                        // parts[1] = accountId, parts[2] = targetDay (optional, defaults to current day)
//                        String accId = parts[1];
//                        int day = parts.length > 2 ? Integer.parseInt(parts[2]) : ledger.getCurrentDay();
//                        ledger.printDaySummary(accId, day);
//                    }
//                    default -> System.out.println("Unknown command. Try: create, credit, debit, auth, settle, reverse, advance, balance, user-replay, exit");
//                }
//            } catch (Exception e) {
//                System.out.println("Error executing command: " + e.getMessage());
//            }
//        }
//    }

    private static void printRes(CommandResult res) {
        System.out.println((res.success() ? "  [SUCCESS] " : "  [ERROR] ") + res.message());
    }
}