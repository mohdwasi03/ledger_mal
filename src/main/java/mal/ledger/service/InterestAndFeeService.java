package mal.ledger.service;

import mal.ledger.enums.Currency;
import mal.ledger.events.Event;
import mal.ledger.events.FeeEvent;
import mal.ledger.events.InterestEvent;
import mal.ledger.store.EventStore;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

public class InterestAndFeeService {
    private final EventStore eventStore;
    private final Map<String, BigDecimal> accumulatedInterest;

    public InterestAndFeeService(EventStore eventStore, Map<String, BigDecimal> accumulatedInterest) {
        this.eventStore = eventStore;
        this.accumulatedInterest = accumulatedInterest;
    }

    public void processEndOfDay(int day, Map<String, Currency> accountCurrencies, java.util.function.Function<List<Event>, BigDecimal> balanceComputer, java.util.function.Supplier<String> idGenerator) {
        for (String accountId : accountCurrencies.keySet()) {
            List<Event> history = eventStore.getStream(accountId);
            BigDecimal closingBalance = balanceComputer.apply(history);
            Currency currency = accountCurrencies.get(accountId);
            int scale = currency.getScale();

            // 1. Overdraft fee if closing balance is negative
            if (closingBalance.compareTo(BigDecimal.ZERO) < 0) {
                String eventId = idGenerator.get();
                BigDecimal feeAmt = new BigDecimal("25.00").setScale(scale, RoundingMode.UNNECESSARY);
                var feeEvent = new FeeEvent(eventId, accountId, feeAmt, day);
                eventStore.append(accountId, history.size(), List.of(feeEvent));
            }

            // 2. Daily interest accrual (positive balances, days 1 to 5)
            if (closingBalance.compareTo(BigDecimal.ZERO) > 0 && day < 6) {
                BigDecimal dailyInterest = closingBalance.multiply(new BigDecimal("0.0004"))
                        .setScale(scale, RoundingMode.HALF_UP);
                accumulatedInterest.put(accountId, accumulatedInterest.getOrDefault(accountId, BigDecimal.ZERO).add(dailyInterest));
            }
        }
    }

    public void capitalizeInterest(int day, Map<String, Currency> accountCurrencies, java.util.function.Supplier<String> idGenerator) {
        if (day != 6) return;
        for (String accountId : accountCurrencies.keySet()) {
            BigDecimal totalInterest = accumulatedInterest.getOrDefault(accountId, BigDecimal.ZERO);
            if (totalInterest.compareTo(BigDecimal.ZERO) > 0) {
                List<Event> history = eventStore.getStream(accountId);
                String eventId = idGenerator.get();
                var interestEvent = new InterestEvent(eventId, accountId, totalInterest, 6);
                eventStore.append(accountId, history.size(), List.of(interestEvent));
            }
        }
    }
}