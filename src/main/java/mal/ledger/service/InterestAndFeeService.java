package mal.ledger.service;

import mal.ledger.enums.Currency;
import mal.ledger.events.Event;
import mal.ledger.events.FeeEvent;
import mal.ledger.events.InterestEvent;
import mal.ledger.store.EventStore;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;

public class InterestAndFeeService {
    private final EventStore eventStore;
    private final Map<String, BigDecimal> accumulatedInterest;

    // Configuration fields loaded from application.properties
    private final BigDecimal dailyInterestRate;
    private final BigDecimal overdraftFeeAmount;

    public InterestAndFeeService(EventStore eventStore, Map<String, BigDecimal> accumulatedInterest) {
        this.eventStore = eventStore;
        this.accumulatedInterest = accumulatedInterest;

        // Load properties
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                props.load(input);
            }
        } catch (Exception e) {
            System.out.println("Warning: Could not load application.properties, using fallback defaults.");
        }

        // Map configuration keys with fallbacks
        this.dailyInterestRate = new BigDecimal(props.getProperty("ledger.interest.daily.rate", "0.0004"));
        this.overdraftFeeAmount = new BigDecimal(props.getProperty("ledger.fee.overdraft", "25.00"));
    }

    public void processEndOfDay(int day, Map<String, Currency> accountCurrencies, Function<List<Event>, BigDecimal> balanceComputer, Supplier<String> idGenerator) {
        for (String accountId : accountCurrencies.keySet()) {
            List<Event> history = eventStore.getStream(accountId);
            BigDecimal closingBalance = balanceComputer.apply(history);
            Currency currency = accountCurrencies.get(accountId);
            int scale = currency.getScale();

            // 1. Overdraft fee if closing balance is negative (using configurable fee)
            if (closingBalance.compareTo(BigDecimal.ZERO) < 0) {
                String eventId = idGenerator.get();
                BigDecimal feeAmt = overdraftFeeAmount.setScale(scale, RoundingMode.UNNECESSARY);
                var feeEvent = new FeeEvent(eventId, accountId, feeAmt, day);
                eventStore.append(accountId, history.size(), List.of(feeEvent));
            }

            // 2. Daily interest accrual runs on EVERY active day for positive balances
            if (closingBalance.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal dailyInterest = closingBalance.multiply(dailyInterestRate)
                        .setScale(scale, RoundingMode.HALF_UP);
                accumulatedInterest.put(accountId, accumulatedInterest.getOrDefault(accountId, BigDecimal.ZERO).add(dailyInterest));
            }
        }
    }

    public void capitalizeInterest( Map<String, Currency> accountCurrencies, Supplier<String> idGenerator) {

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