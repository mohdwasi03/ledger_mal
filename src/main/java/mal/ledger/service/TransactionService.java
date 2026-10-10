package mal.ledger.service;

import mal.ledger.enums.Currency;
import mal.ledger.events.CreditEvent;
import mal.ledger.events.DebitEvent;
import mal.ledger.events.Event;
import mal.ledger.store.EventStore;
import mal.ledger.util.CommandResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class TransactionService {
    private final EventStore eventStore;

    public TransactionService(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    public CommandResult credit(String accountId, BigDecimal amount, int currentDay, int valueDay, String eventId) {
        List<Event> history = eventStore.getStream(accountId);
        if (history.isEmpty()) return CommandResult.failure("Account not found: " + accountId);

        var event = new CreditEvent(eventId, accountId, amount, currentDay, valueDay);
        eventStore.append(accountId, history.size(), List.of(event));
        return CommandResult.success("Credited " + amount + " to " + accountId + " [EventID: " + eventId + "]");
    }

    public CommandResult debit(String accountId, BigDecimal amount, int currentDay, int valueDay, BigDecimal availableBalance, String eventId) {
        List<Event> history = eventStore.getStream(accountId);
        if (history.isEmpty()) return CommandResult.failure("Account not found: " + accountId);

        if (availableBalance.compareTo(amount) < 0) {
            return CommandResult.failure("Insufficient available balance on " + accountId + ". Available: " + availableBalance + " [EventID: " + eventId + "]");
        }

        var event = new DebitEvent(eventId, accountId, amount, currentDay, valueDay);
        eventStore.append(accountId, history.size(), List.of(event));
        return CommandResult.success("Debited " + amount + " from " + accountId + " [EventID: " + eventId + "]");
    }

    public void splitAndCreditInstallments(String accountId, BigDecimal totalAmount, int count, int currentDay, int valueDay, Currency currency, java.util.function.Supplier<String> idGenerator) {
        List<Event> history = eventStore.getStream(accountId);
        int scale = currency.getScale();

        BigDecimal base = totalAmount.divide(BigDecimal.valueOf(count), scale, RoundingMode.DOWN);
        BigDecimal remainder = totalAmount.subtract(base.multiply(BigDecimal.valueOf(count)));

        System.out.print("  [CREDIT] Splitting " + totalAmount + " into " + count + " parts: ");
        for (int i = 1; i <= count; i++) {
            BigDecimal amt = (i == count) ? base.add(remainder) : base;
            String eventId = idGenerator.get();
            var event = new CreditEvent(eventId, accountId, amt, currentDay, valueDay);
            eventStore.append(accountId, history.size(), List.of(event));
            history = eventStore.getStream(accountId);

            System.out.print(amt + (i < count ? ", " : "\n"));
        }
    }
}