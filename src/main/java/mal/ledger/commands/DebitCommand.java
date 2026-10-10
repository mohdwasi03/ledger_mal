package mal.ledger.commands;

import mal.ledger.events.DebitEvent;
import mal.ledger.util.CommandResult;
import mal.ledger.store.EventStore;

import java.math.BigDecimal;
import java.util.List;

public class DebitCommand implements LedgerCommand {
    private final String accountId;
    private final BigDecimal amount;
    private final int valueDay;
    private final BigDecimal availableBalance;
    private final String eventId;

    public DebitCommand(String accountId, BigDecimal amount, int valueDay, BigDecimal availableBalance, String eventId) {
        this.accountId = accountId;
        this.amount = amount;
        this.valueDay = valueDay;
        this.availableBalance = availableBalance;
        this.eventId = eventId;
    }

    @Override
    public CommandResult execute(EventStore eventStore, int currentDay) {
        List<?> history = eventStore.getStream(accountId);
        if (history.isEmpty()) {
            return CommandResult.failure("Account not found: " + accountId + " [EventID: " + eventId + "]");
        }
        if (availableBalance.compareTo(amount) < 0) {
            return CommandResult.failure("Insufficient available balance on " + accountId + ". Available: " + availableBalance + " [EventID: " + eventId + "]");
        }
        var event = new DebitEvent(eventId, accountId, amount, currentDay, valueDay);
        eventStore.append(accountId, history.size(), List.of(event));
        return CommandResult.success("Debited " + amount + " from " + accountId + " [EventID: " + eventId + "]");
    }
}