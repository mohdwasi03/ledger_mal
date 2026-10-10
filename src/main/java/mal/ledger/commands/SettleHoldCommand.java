package mal.ledger.commands;

import mal.ledger.events.AuthEvent;
import mal.ledger.events.SettlementEvent;
import mal.ledger.util.CommandResult;
import mal.ledger.store.EventStore;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class SettleHoldCommand implements LedgerCommand {
    private final String accountId;
    private final String authId;
    private final BigDecimal amount;
    private final int valueDay;
    private final String eventId;

    public SettleHoldCommand(String accountId, String authId, BigDecimal amount, int valueDay, String eventId) {
        this.accountId = accountId;
        this.authId = authId;
        this.amount = amount;
        this.valueDay = valueDay;
        this.eventId = eventId;
    }

    @Override
    public CommandResult execute(EventStore eventStore, int currentDay) {
        List<Object> history = Collections.singletonList(eventStore.getStream(accountId));
        if (history.isEmpty()) {
            return CommandResult.failure("Account not found [EventID: " + eventId + "]");
        }
        boolean authExists = history.stream()
                .filter(e -> e instanceof AuthEvent)
                .map(e -> (AuthEvent) e)
                .anyMatch(a -> a.authId().equals(authId));

        if (!authExists) {
            return CommandResult.failure("Settlement rejected: No preceding authorization found for hold ID '" + authId + "' [EventID: " + eventId + "]");
        }

        var event = new SettlementEvent(eventId, accountId, authId, amount, currentDay, valueDay);
        eventStore.append(accountId, history.size(), List.of(event));
        return CommandResult.success("Settled hold " + authId + " for " + amount + " [EventID: " + eventId + "]");
    }
}