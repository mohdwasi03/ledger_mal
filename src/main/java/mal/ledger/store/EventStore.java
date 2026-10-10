package mal.ledger.store;

import mal.ledger.events.DomainEvent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class EventStore {
    private final Map<String, List<DomainEvent>> streams = new ConcurrentHashMap<>();
    private final Set<String> globalEventIds = Collections.synchronizedSet(new HashSet<>());
    private final ConcurrentHashMap<String, ReentrantLock> streamLocks = new ConcurrentHashMap<>();

    public void append(String accountId, int expectedVersion, List<DomainEvent> newEvents) {
        ReentrantLock lock = streamLocks.computeIfAbsent(accountId, k -> new ReentrantLock());
        lock.lock();
        try {
            List<DomainEvent> stream = streams.computeIfAbsent(accountId, k -> new ArrayList<>());

            // Optimistic concurrency check
            if (stream.size() != expectedVersion) {
                throw new IllegalStateException(String.format(
                        "Concurrency conflict on account %s. Expected version %d, but found %d",
                        accountId, expectedVersion, stream.size()
                ));
            }

            for (DomainEvent event : newEvents) {
                if (!globalEventIds.add(event.eventId())) {
                    throw new IllegalArgumentException("Duplicate event ID detected: " + event.eventId());
                }
                stream.add(event);
            }
        } finally {
            lock.unlock();
        }
    }

    public List<DomainEvent> getStream(String accountId) {
        return List.copyOf(streams.getOrDefault(accountId, Collections.emptyList()));
    }
}