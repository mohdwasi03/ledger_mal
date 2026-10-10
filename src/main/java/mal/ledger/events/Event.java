package mal.ledger.events;

public interface Event {
    String eventId();
    String accountId();
    int day();

    int systemDay();

    int valueDay();
}