package mal.ledger.commands;

import mal.ledger.events.CreditEvent;
import mal.ledger.util.CommandResult;
import mal.ledger.store.EventStore;

import java.math.BigDecimal;
import java.util.List;

public class CreditCommand implements LedgerCommand {
    private final String accountId;
    private final BigDecimal amount;
    private final int valueDay;
    private final String eventId;

    public CreditCommand(String accountId, BigDecimal amount, int valueDay, String eventId) {
        this.accountId = accountId;
        this.amount = amount;
        this.valueDay = valueDay;
        this.eventId = eventId;
    }

    @Override
    public CommandResult execute(EventStore eventStore, int currentDay) {
        List<?> history = eventStore.getStream(accountId);
        if (history.isEmpty()) {
            return CommandResult.failure("Account not found: " + accountId);
        }
        var event = new CreditEvent(eventId, accountId, amount, currentDay, valueDay);
        eventStore.append(accountId, history.size(), List.of(event));
        return CommandResult.success("Credited " + amount + " to " + accountId + " [EventID: " + eventId + "]");
    }
}