# Architectural Edge Cases & Trade-offs

1. ## Strict Unmatched Settlement Rejection (`Auth-Z` / Unresolved Authorization)
- **Rule:** Any settlement referencing an authorization ID not present in the ledger must be **strictly rejected**. Funds must not leave the account, and the ledger balance remains unaffected.
- **Security Audit Requirement:** Upon rejecting an unmatched settlement, an immutable `SECURITY_AUDIT_LOG` record is generated capturing the reference ID, account identifier, amount, and `ERR_AUTH_NOT_FOUND` error code.

---


## 2. Authorization Hold Management (Expiry Out-of-Scope)
- **Available vs. Ledger Balance:** Approving an authorization hold reduces the account's **Available Balance** while leaving the official **Ledger Balance** untouched until a valid settlement arrives.
- **Out-of-Scope Boundary (Hold Expiry):** Automatic time-based expiration of active holds is explicitly **out of scope** for this core engine implementation. Holds remain active until explicitly settled.

---

## 3. Event Idempotency & Deduplication
- **Rule:** Every incoming event must carry a globally unique `eventId`. If an event with an already-registered `eventId` is submitted, the engine rejects it as a duplicate (`DUPLICATE_EVENT_ERROR`) to prevent double-posting.

---

## 4. Overdraft Fee Retention Policy on Reversals (Fee Stickiness)
- **Problem Statement & Context:** An accidental duplicate payment or debit (e.g., `E7`) pulls the ledger balance below zero, triggering an automated overdraft penalty fee (`FeeEvent`). Later, the underlying transaction is successfully reversed via a compensating event (`ReversalEvent`).
- **Architectural & Business Decision:** The engine **does not** automatically cascade or refund the overdraft fee upon reversing the causative debit.
- **Rationale & Real-World Justification:** During the window between the original debit and its subsequent reversal, the bank provided actual liquidity, honored the outgoing payment to an external merchant, and absorbed credit risk. The overdraft fee remains permanently intact in the audit trail as a valid service charge for temporary un-funded liquidity provision, maintaining strict separation between merchant-level commercial disputes and bank penalty policies.

---

## 5. Concurrent Requests & Stream-Level Optimistic Locking (Out-of-Scope)
- **Boundary Definition:** Handling distributed concurrency, multi-threaded worker race conditions, and **Stream-Level Optimistic Locking** (such as validating expected stream version numbers on every append) are explicitly **out of scope** for this single-threaded in-memory simulation engine.
- **Rationale:** The simulation focuses strictly on business logic correctness, event-sourcing re-aggregation, bitemporal interest accrual, and compensation patterns in a deterministic single-threaded runtime environment.

---

## Temporal Modeling: Simulation Days vs. Real-World Dates
- **Context:** For the scope of this interview simulation, the ledger tracks time using an integer-based day counter (`int day`, starting at Day 1 and advancing via `ADVANCE_DAY`) rather than calendar dates to avoid calendar edge cases and keep test assertions deterministic.
- **Real-World Production Consideration:** In a production-grade banking ledger, transactions require bi-temporal modeling using `java.time.Instant` or `java.time.LocalDate` with explicit time zones (e.g., UTC) to separate the **Effective/Value Date** (business accounting date) from the **Recorded Timestamp** (physical wall-clock ingestion time for compliance).