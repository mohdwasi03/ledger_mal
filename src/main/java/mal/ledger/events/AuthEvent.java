package mal.ledger.events;

import java.math.BigDecimal;

public record AuthEvent(
        String eventId,
        String accountId,
        String authId,
        BigDecimal amount,
        int day,
        int valueDay) implements Event {
    @Override
    public int systemDay() {
        return 0;
    }

    @Override public int valueDay() { return day; }
}