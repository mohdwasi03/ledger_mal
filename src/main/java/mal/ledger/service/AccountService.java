package mal.ledger.service;

import mal.ledger.enums.Currency;
import mal.ledger.events.AccountEvent;
import mal.ledger.events.Event;
import mal.ledger.store.EventStore;
import mal.ledger.util.CommandResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountService {
    private final EventStore eventStore;
    private int currentDay = 1;
    private final Map<String, Currency> accountCurrencies = new HashMap<>();
    private final Map<String, String> accountUserMap = new HashMap<>();

    public AccountService(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    public int getCurrentDay() { return currentDay; }
    public void advanceDay() { currentDay++; }

    public void validateSystemDay() {
        if (currentDay < 1) {
            throw new IllegalStateException("System day cannot be less than 1. Current day: " + currentDay);
        }
    }

    public Map<String, Currency> getAccountCurrencies() { return accountCurrencies; }
    public Map<String, String> getAccountUserMap() { return accountUserMap; }

    public CommandResult createAccount(String userId, String accountId, Currency currency, String bootstrapEventId) {
        validateSystemDay();
        List<Event> history = eventStore.getStream(accountId);
        if (!history.isEmpty()) {
            return CommandResult.failure("Account " + accountId + " already exists.");
        }
        var event = new AccountEvent(bootstrapEventId, accountId, userId, currency, currentDay);
        eventStore.append(accountId, 0, List.of(event));
        accountCurrencies.put(accountId, currency);
        accountUserMap.put(accountId, userId);
        return CommandResult.success("Created account " + accountId + " for User " + userId + " [" + currency + "]");
    }
}