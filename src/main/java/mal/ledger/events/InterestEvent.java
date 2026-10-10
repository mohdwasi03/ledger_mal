package mal.ledger.events;

import java.math.BigDecimal;

public record InterestEvent(
        String eventId,
        String accountId,
        BigDecimal amount,
        int day
) implements Event {
    @Override
    public int systemDay() {
        return 0;
    }

    @Override public int valueDay() { return day; }
}
