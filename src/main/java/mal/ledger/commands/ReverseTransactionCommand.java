package mal.ledger.commands;

import mal.ledger.events.Event;
import mal.ledger.events.ReversalEvent;
import mal.ledger.util.CommandResult;
import mal.ledger.store.EventStore;

import java.util.List;

public class ReverseTransactionCommand implements LedgerCommand {
    private final String accountId;
    private final String targetEventId;
    private final int valueDay;
    private final int systemDay;
    private final String reason;
    private final String eventId;

    public ReverseTransactionCommand(String accountId, String targetEventId, int valueDay, int systemDay, String reason, String eventId) {
        this.accountId = accountId;
        this.targetEventId = targetEventId;
        this.valueDay = valueDay;
        this.systemDay = systemDay;
        this.reason = reason;
        this.eventId = eventId;
    }

    @Override
    public CommandResult execute(EventStore eventStore, int currentDay) {
        List<Event> history = eventStore.getStream(accountId);
        if (history.isEmpty()) {
            return CommandResult.failure("Account not found [EventID: " + eventId + "]");
        }
        boolean targetExists = history.stream().anyMatch(e -> e.eventId().equals(targetEventId));
        if (!targetExists) {
            return CommandResult.failure("Reversal failed: Target event " + targetEventId + " not found. [EventID: " + eventId + "]");
        }

        var event = new ReversalEvent(eventId, accountId, targetEventId, systemDay, valueDay, reason);
        eventStore.append(accountId, history.size(), List.of(event));
        return CommandResult.success("Reversed transaction " + targetEventId + " with " + eventId + " [Reason: " + reason + "]");
    }
}