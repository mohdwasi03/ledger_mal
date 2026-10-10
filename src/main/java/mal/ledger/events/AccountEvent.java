package mal.ledger.events;

import mal.ledger.enums.Currency;

public record AccountEvent(String eventId, String accountId, String userId, Currency currency, int day) implements Event {
    @Override
    public int systemDay() {
        return 0;
    }

    @Override public int valueDay() { return day; }
}