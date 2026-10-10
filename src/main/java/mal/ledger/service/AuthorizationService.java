package mal.ledger.service;

import mal.ledger.events.AuthEvent;
import mal.ledger.events.Event;
import mal.ledger.events.SettlementEvent;
import mal.ledger.store.EventStore;
import mal.ledger.util.CommandResult;

import java.math.BigDecimal;
import java.util.List;

public class AuthorizationService {
    private final EventStore eventStore;

    public AuthorizationService(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    public CommandResult authorizeHold(String accountId, String authId, BigDecimal amount, int currentDay, int valueDay, BigDecimal availableBalance, String eventId) {
        List<Event> history = eventStore.getStream(accountId);
        if (history.isEmpty()) return CommandResult.failure("Account not found: " + accountId);

        if (availableBalance.compareTo(amount) < 0) {
            return CommandResult.failure("Authorization rejected: Insufficient available balance (" + availableBalance + ")"+ " [EventID: " + eventId + "]");
        }

        var event = new AuthEvent(eventId, accountId, authId, amount, currentDay, valueDay);
        eventStore.append(accountId, history.size(), List.of(event));
        return CommandResult.success("Hold " + authId + " authorized for " + amount + " [EventID: " + eventId + "]");
    }

    public CommandResult settleHold(String accountId, String authId, BigDecimal amount, int currentDay, int valueDay, String eventId) {
        List<Event> history = eventStore.getStream(accountId);
        if (history.isEmpty()) return CommandResult.failure("Account not found: " + accountId);

        boolean authExists = history.stream()
                .filter(e -> e instanceof AuthEvent)
                .map(e -> (AuthEvent) e)
                .anyMatch(a -> a.authId().equals(authId));

        if (!authExists) {
            return CommandResult.failure("Settlement rejected: No preceding authorization found for hold ID '" + authId + "'"+ " [EventID: " + eventId + "]");
        }

        var event = new SettlementEvent(eventId, accountId, authId, amount, currentDay, valueDay);
        eventStore.append(accountId, history.size(), List.of(event));
        return CommandResult.success("Settled hold " + authId + " for " + amount + " [EventID: " + eventId + "]");
    }
}