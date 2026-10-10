# Refused Requirements & Invalid Acceptance Criteria 

## 1. Refusal of Force-Post Clearing Without Valid Authorization 
- **Refused Premise:** "Settlements without prior authorization holds should be force-posted as debits to avoid payment network clearing disputes."
- **Reason for Rejection:** Violates core fund integrity and security constraints. Any settlement referencing an authorization ID not already present in the event store must be rejected immediately without deducting customer funds.

## 2. Refusal to Mutate Historical Event Records
- **Refused Premise:** "Reversing transaction E requires deleting E and its associated fee event directly from the events."
- **Reason for Rejection:** Violates the foundational append-only constraint of event sourcing. Reversals and fee restorations are achieved via explicit stateful compensating events, never by mutating or deleting stored historical event rows.

## 3. Refusal of Implicit Account Creation
- **Refused Premise:** "Transactions or settlements can be posted to accounts that haven't been explicitly initialized yet, creating accounts on the fly."
- **Reason for Rejection:** Violates strict account registration policies. Accounts must be formally initialized with a genesis event (`ACCOUNT_CREATED`) and a valid currency code (`AED`/`BHD`) before receiving transactional events.

## 4. Refusal of Automatic Hold Expiration
- **Refused Premise:** "Active authorization holds must automatically expire and release funds back to the available balance after 7 days."
- **Reason for Rejection:** Marked as **out-of-scope** for the current core ledger engine implementation. Holds remain active in the projection map until explicitly settled or released via domain events.

## 5. Refusal of Discarding Rounding Remainders (Conservation Violation)
- **Refused Premise:** "When splitting totals like 10.000 BHD into instalments, minor rounding residue or remainders can be discarded or dropped since fractions of a unit are negligible."
- **Reason for Rejection:** Violates strict zero-sum financial conservation. Discarding remainders creates ledger imbalances. We enforce strict **penny-drop distribution**, attaching all rounding residue to the final instalment (`3.334 BHD`) so that the sum of parts equals the exact total without losing or gaining any currency units.