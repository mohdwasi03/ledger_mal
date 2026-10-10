package mal.ledger.events;

public interface DomainEvent {
    String eventId();
    String accountId();
    int day();
}