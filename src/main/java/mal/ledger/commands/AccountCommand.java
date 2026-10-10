package mal.ledger.commands;

import mal.ledger.enums.Currency;
import mal.ledger.events.AccountEvent;
import mal.ledger.util.CommandResult;
import mal.ledger.store.EventStore;

import java.util.List;
import java.util.Map;

public class AccountCommand implements LedgerCommand {
    private final String userId;
    private final String accountId;
    private final Currency currency;
    private final String bootstrapEventId;
    private final Map<String, Currency> currencies;
    private final Map<String, String> userMap;

    public AccountCommand(String userId, String accountId, Currency currency, String bootstrapEventId,
                                Map<String, Currency> currencies, Map<String, String> userMap) {
        this.userId = userId;
        this.accountId = accountId;
        this.currency = currency;
        this.bootstrapEventId = bootstrapEventId;
        this.currencies = currencies;
        this.userMap = userMap;
    }

    @Override
    public CommandResult execute(EventStore eventStore, int currentDay) {
        List<?> history = eventStore.getStream(accountId);
        if (!history.isEmpty()) {
            return CommandResult.failure("Account " + accountId + " already exists.");
        }
        var event = new AccountEvent(bootstrapEventId, accountId, userId, currency, currentDay);
        eventStore.append(accountId, 0, List.of(event));
        currencies.put(accountId, currency);
        userMap.put(accountId, userId);
        return CommandResult.success("Created account " + accountId + " for User " + userId + " [" + currency + "]");
    }
}