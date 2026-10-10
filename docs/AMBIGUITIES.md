# Architectural Edge Cases & Trade-offs 

## 1. Strict Unmatched Settlement Rejection (`Auth-Z` / Unresolved Authorization)
* **Rule:** Any settlement referencing an authorization ID not present in the ledger must be **strictly rejected**. Funds must not leave the account, and the ledger balance remains unaffected.
* **Security Audit Requirement:** Upon rejecting an unmatched settlement, an immutable `SECURITY_AUDIT_LOG` record is generated capturing the reference ID, account identifier, amount, and `ERR_AUTH_NOT_FOUND` error code.

---

## 2. Dynamic Fee & Interest Restoration Following Reversals
* **Compensating Fee Reversal:** Processing a reversal automatically triggers a system-generated compensating `FEE_REVERSAL` credit event referencing the original fee to restore the net customer fee balance to zero.
* **Bi-Temporal Interest Recalculation:** The projection engine re-evaluates positive closing balances for historical periods without the reversed debit, ensuring capitalized `INTEREST` credits correctly incorporate restored earnings.

---

## 3. Authorization Hold Management (Expiry Out-of-Scope)
* **Available vs. Ledger Balance:** Approving an authorization hold reduces the account's **Available Balance** while leaving the official **Ledger Balance** untouched until a valid settlement arrives.
* **Out-of-Scope Boundary (Hold Expiry):** Automatic time-based expiration of active holds is explicitly **out of scope** for this core engine implementation. Holds remain active until explicitly settled.

---

## 4. Event Idempotency & Deduplication
* **Rule:** Every incoming event must carry a globally unique `eventId`. If an event with an already-registered `eventId` is submitted, the engine rejects it as a duplicate (`DUPLICATE_EVENT_ERROR`) to prevent double-posting.

## 5. Stream-Level Optimistic Locking

### Problem Statement & Context
* **Trigger Scenario:** Multiple concurrent threads or distributed worker nodes attempt to append new financial events (e.g., deposits, debits, settlements) to the exact same account stream simultaneously, risking race conditions and out-of-order execution.

### Resolution & Justification
* **Expected Version Validation:** Every append operation to an account stream must supply the expected current version number of that stream.
* **Concurrency Exception Rule:** If the event store detects that another transaction has advanced the stream version in the interim, the write is aborted and a `CONCURRENCY_EXCEPTION` is thrown. The calling service must re-read the latest state, re-validate business invariants, and retry. This avoids expensive database row-locking while guaranteeing total ordering per account.