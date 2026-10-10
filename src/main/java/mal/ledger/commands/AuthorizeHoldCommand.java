package mal.ledger.commands;

import mal.ledger.events.AuthEvent;
import mal.ledger.util.CommandResult;
import mal.ledger.store.EventStore;

import java.math.BigDecimal;
import java.util.List;

public class AuthorizeHoldCommand implements LedgerCommand {
    private final String accountId;
    private final String authId;
    private final BigDecimal amount;
    private final int valueDay;
    private final BigDecimal availableBalance;
    private final String eventId;

    public AuthorizeHoldCommand(String accountId, String authId, BigDecimal amount, int valueDay, BigDecimal availableBalance, String eventId) {
        this.accountId = accountId;
        this.authId = authId;
        this.amount = amount;
        this.valueDay = valueDay;
        this.availableBalance = availableBalance;
        this.eventId = eventId;
    }

    @Override
    public CommandResult execute(EventStore eventStore, int currentDay) {
        List<?> history = eventStore.getStream(accountId);
        if (history.isEmpty()) {
            return CommandResult.failure("Account not found [EventID: " + eventId + "]");
        }
        if (availableBalance.compareTo(amount) < 0) {
            return CommandResult.failure("Authorization rejected: Insufficient available balance (" + availableBalance + ") [EventID: " + eventId + "]");
        }
        var event = new AuthEvent(eventId, accountId, authId, amount, currentDay, valueDay);
        eventStore.append(accountId, history.size(), List.of(event));
        return CommandResult.success("Hold " + authId + " authorized for " + amount + " [EventID: " + eventId + "]");
    }
}