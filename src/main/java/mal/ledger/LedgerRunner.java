package mal.ledger;

import mal.ledger.enums.Currency;
import mal.ledger.service.CommandResult;
import mal.ledger.service.LedgerService;
import mal.ledger.store.EventStore;

import java.math.BigDecimal;

public class LedgerRunner {
    public static void main(String[] args) {

        EventStore eventStore = new EventStore();
        LedgerService ledgerService = new LedgerService(eventStore);

        System.out.println("=== Starting Ledger Engine Simulation ===");


        CommandResult res1 = ledgerService.createAccount("ACC-001", Currency.AED, "evt-001");
        System.out.println(res1.message());

        ledgerService.advanceDay();

        CommandResult res2 = ledgerService.createAccount("ACC-002", Currency.BHD, "evt-002");
        System.out.println(res2.message());







    }
}