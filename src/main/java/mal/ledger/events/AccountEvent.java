package mal.ledger.events;

import mal.ledger.enums.Currency;

public record AccountEvent(
        String eventId,
        String accountId,
        Currency currency,
        int day
) implements DomainEvent {}