package mal.ledger.commands;

import mal.ledger.util.CommandResult;
import mal.ledger.store.EventStore;

public interface LedgerCommand {
    CommandResult execute(EventStore eventStore, int currentDay);
}