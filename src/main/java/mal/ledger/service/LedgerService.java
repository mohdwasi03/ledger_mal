package mal.ledger.service;

import mal.ledger.enums.Currency;
import mal.ledger.events.Event;
import mal.ledger.store.EventStore;
import mal.ledger.util.CommandResult;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LedgerService {
    private final EventStore eventStore;

    // Decomposed  Services
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final AuthorizationService authService;
    private final AdjustmentService adjustmentService;
    private final InterestAndFeeService interestService;
    private final LedgerQueryService queryService;

    private int eventCounter = 1;
    private final Map<String, BigDecimal> accumulatedInterest = new HashMap<>();

    public LedgerService(EventStore eventStore) {
        this.eventStore = eventStore;
        this.accountService = new AccountService(eventStore);
        this.transactionService = new TransactionService(eventStore);
        this.authService = new AuthorizationService(eventStore);
        this.adjustmentService = new AdjustmentService(eventStore);
        this.queryService = new LedgerQueryService(eventStore);
        this.interestService = new InterestAndFeeService(eventStore, accumulatedInterest);
    }

    public int getCurrentDay() {
        return accountService.getCurrentDay();
    }

    public void advanceDay() {
        accountService.advanceDay();
    }

    private String nextEventId() {
        return "E" + (eventCounter++);
    }

    // --- COMMAND DISPATCH METHODS ---

    public CommandResult createAccount(String userId, String accountId, Currency currency, String bootstrapEventId) {
        return accountService.createAccount(userId, accountId, currency, bootstrapEventId);
    }

    public CommandResult credit(String accountId, BigDecimal amount, int valueDay) {
        accountService.validateSystemDay();
        return transactionService.credit(accountId, amount, getCurrentDay(), valueDay, nextEventId());
    }

    public CommandResult debit(String accountId, BigDecimal amount, int valueDay) {
        accountService.validateSystemDay();
        List<Event> history = eventStore.getStream(accountId);
        BigDecimal available = queryService.computeAvailableBalance(history, getCurrentDay());
        return transactionService.debit(accountId, amount, getCurrentDay(), valueDay, available, nextEventId());
    }

    public CommandResult authorizeHold(String accountId, String authId, BigDecimal amount, int valueDay) {
        accountService.validateSystemDay();
        List<Event> history = eventStore.getStream(accountId);
        BigDecimal available = queryService.computeAvailableBalance(history, getCurrentDay());
        return authService.authorizeHold(accountId, authId, amount, getCurrentDay(), valueDay, available, nextEventId());
    }

    public CommandResult settleHold(String accountId, String authId, BigDecimal amount, int valueDay) {
        accountService.validateSystemDay();
        return authService.settleHold(accountId, authId, amount, getCurrentDay(), valueDay, nextEventId());
    }

    public CommandResult reverseTransaction(String accountId, String targetEventId, int valueDay, int systemDay, String reason) {
        accountService.validateSystemDay();
        return adjustmentService.reverseTransaction(accountId, targetEventId, valueDay, systemDay, reason, nextEventId());
    }

    public void splitAndCreditInstallments(String accountId, BigDecimal totalAmount, int count, int valueDay) {
        accountService.validateSystemDay();
        Currency currency = accountService.getAccountCurrencies().getOrDefault(accountId, Currency.AED);
        transactionService.splitAndCreditInstallments(accountId, totalAmount, count, getCurrentDay(), valueDay, currency, this::nextEventId);
    }

    // --- BATCH & QUERY PROJECTIONS ---

    public void endOfDayProcessing(int day) {
        accountService.validateSystemDay();
        interestService.processEndOfDay(
                day,
                accountService.getAccountCurrencies(),
                history -> queryService.computeSettledBalance(history, day),
                this::nextEventId
        );
    }

    public void capitalizeInterest(int day) {
        accountService.validateSystemDay();
        interestService.capitalizeInterest(day, accountService.getAccountCurrencies(), this::nextEventId);
    }

    public void printUserEventReplay(String userId) {
        queryService.printUserEventReplay(userId, accountService.getAccountUserMap(), accountService.getAccountCurrencies());
    }

    public void printDaySummary(String accountId, int day) {
        queryService.printDaySummary(accountId, day, accountService.getAccountUserMap());
    }
}