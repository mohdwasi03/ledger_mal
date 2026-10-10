package mal.ledger.service;

import mal.ledger.enums.Currency;
import mal.ledger.events.*;
import mal.ledger.store.EventStore;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class LedgerQueryService {
    private final EventStore eventStore;

    public LedgerQueryService(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    /**
     * Computes the settled balance for an account up to a specific value day (bitemporal projection).
     */
    public BigDecimal computeSettledBalance(List<Event> history, int upToValueDay) {
        BigDecimal balance = BigDecimal.ZERO;
        for (Event e : history) {
            if (e.valueDay() > upToValueDay) continue;
            if (e instanceof CreditEvent c) balance = balance.add(c.amount());
            else if (e instanceof DebitEvent d) balance = balance.subtract(d.amount());
            else if (e instanceof SettlementEvent s) balance = balance.subtract(s.amount());
            else if (e instanceof FeeEvent f) balance = balance.subtract(f.amount());
            else if (e instanceof InterestEvent i) balance = balance.add(i.amount());
            else if (e instanceof ReversalEvent r) {
                BigDecimal originalAmt = getEventAmount(history, r.targetEventId());
                if (isPositiveEffect(history, r.targetEventId())) {
                    balance = balance.subtract(originalAmt);
                } else {
                    balance = balance.add(originalAmt);
                }
            }
        }
        return balance;
    }

    /**
     * Computes the available balance by subtracting active (unsettled) holds from the settled balance.
     */
    public BigDecimal computeAvailableBalance(List<Event> history, int upToDay) {
        BigDecimal settled = computeSettledBalance(history, upToDay);
        BigDecimal activeHolds = BigDecimal.ZERO;
        for (Event e : history) {
            if (e.day() > upToDay) continue;
            if (e instanceof AuthEvent a) {
                boolean settledMatch = history.stream().anyMatch(s -> s instanceof SettlementEvent se && se.day() <= upToDay && se.authId().equals(a.authId()));
                if (!settledMatch) activeHolds = activeHolds.add(a.amount());
            }
        }
        return settled.subtract(activeHolds);
    }

    /**
     * Retrieves total fees assessed for a specific day.
     */
    public BigDecimal getFeesForDay(List<Event> history, int day) {
        BigDecimal total = BigDecimal.ZERO;
        for (Event e : history) {
            if (e instanceof FeeEvent f && f.day() == day) total = total.add(f.amount());
        }
        return total;
    }

    /**
     * Prints the complete event stream replay for all accounts owned by a given user.
     */
    public void printUserEventReplay(String userId, Map<String, String> accountUserMap, Map<String, Currency> accountCurrencies) {
        System.out.println("\n=== Event Stream Replay for User: " + userId + " ===");
        boolean found = false;
        for (Map.Entry<String, String> entry : accountUserMap.entrySet()) {
            if (entry.getValue().equals(userId)) {
                found = true;
                String accId = entry.getKey();
                List<Event> history = eventStore.getStream(accId);
                System.out.println("  -> Account: " + accId + " [" + accountCurrencies.get(accId) + "] (" + history.size() + " events)");
                for (int i = 0; i < history.size(); i++) {
                    Event e = history.get(i);
                    System.out.println(String.format("     [%d] ID: %s | Type: %s | SystemDay: %d | ValueDay: %d",
                            i + 1, e.eventId(), e.getClass().getSimpleName(), e.day(), e.valueDay()));
                }
            }
        }
        if (!found) {
            System.out.println("  No accounts found for user: " + userId);
        }
    }

    /**
     * Prints the daily summary (ledger balance, available balance, fees) for an account.
     */
    public void printDaySummary(String accountId, int day, Map<String, String> accountUserMap) {
        List<Event> history = eventStore.getStream(accountId);
        BigDecimal ledgerBal = computeSettledBalance(history, day);
        BigDecimal availBal = computeAvailableBalance(history, day);
        BigDecimal fee = getFeesForDay(history, day);
        System.out.println(String.format("  [%s (User: %s) - Day %d] Ledger Bal: %s | Avail Bal: %s | Fee: %s",
                accountId, accountUserMap.getOrDefault(accountId, "UNKNOWN"), day, ledgerBal, availBal, fee));
    }

    private BigDecimal getEventAmount(List<Event> history, String eventId) {
        return history.stream().filter(e -> e.eventId().equals(eventId)).map(e -> {
            if (e instanceof CreditEvent c) return c.amount();
            if (e instanceof DebitEvent d) return d.amount();
            if (e instanceof SettlementEvent s) return s.amount();
            if (e instanceof FeeEvent f) return f.amount();
            if (e instanceof InterestEvent i) return i.amount();
            return BigDecimal.ZERO;
        }).findFirst().orElse(BigDecimal.ZERO);
    }

    private boolean isPositiveEffect(List<Event> history, String eventId) {
        return history.stream().filter(e -> e.eventId().equals(eventId))
                .anyMatch(e -> e instanceof CreditEvent || e instanceof InterestEvent);
    }
}