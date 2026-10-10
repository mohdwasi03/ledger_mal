package mal.ledger.service;

import mal.ledger.events.Event;
import mal.ledger.events.ReversalEvent;
import mal.ledger.store.EventStore;
import mal.ledger.util.CommandResult;

import java.util.List;

public class AdjustmentService {
    private final EventStore eventStore;

    public AdjustmentService(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    public CommandResult reverseTransaction(String accountId, String targetEventId, int valueDay, int systemDay, String reason, String eventId) {
        List<Event> history = eventStore.getStream(accountId);
        if (history.isEmpty()) {
            return CommandResult.failure("Account not found: " + accountId);
        }

        boolean targetExists = history.stream().anyMatch(e -> e.eventId().equals(targetEventId));
        if (!targetExists) {
            return CommandResult.failure("Reversal failed: Target event " + targetEventId + " not found." + " [EventID: " + eventId + "]");
        }

        var event = new ReversalEvent(eventId, accountId, targetEventId, systemDay, valueDay, reason);
        eventStore.append(accountId, history.size(), List.of(event));
        return CommandResult.success("Reversed transaction " + targetEventId + " with [EventID: " + eventId + "]" + " [Reason: " + reason + "]");
    }
}