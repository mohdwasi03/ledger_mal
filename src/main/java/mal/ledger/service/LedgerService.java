package mal.ledger.service;

import mal.ledger.enums.Currency;
import mal.ledger.events.AccountEvent;
import mal.ledger.events.DomainEvent;
import mal.ledger.store.EventStore;

import java.util.List;

public class LedgerService {
    private final EventStore eventStore;
    private int currentDay = 1;

    public LedgerService(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    public void advanceDay() {
        currentDay++;
        System.out.println(">>> System Day Advanced To: Day " + currentDay);
    }

    public CommandResult createAccount(String accountId, Currency currency, String eventId) {
        List<DomainEvent> history = eventStore.getStream(accountId);

        if (!history.isEmpty()) {
            return CommandResult.failure("Account " + accountId + " already exists.");
        }

        try {
            var event = new AccountEvent(eventId, accountId, currency, currentDay);
            eventStore.append(accountId, 0, List.of(event));
            return CommandResult.success("Created Account: " + accountId + " [" + currency + "] on Day " + currentDay);
        } catch (IllegalArgumentException e) {
            return CommandResult.failure("Failed to create account: " + e.getMessage());
        }
    }

}