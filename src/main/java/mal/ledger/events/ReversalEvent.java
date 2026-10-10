package mal.ledger.events;

public record ReversalEvent(
        String eventId,
        String accountId,
        String targetEventId,
        int day,
        int valueDay, String reason) implements Event {
    @Override
    public int systemDay() {
        return 0;
    }

    @Override public int valueDay() { return day; }
}