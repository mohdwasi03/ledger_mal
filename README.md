#  In-Memory Account Ledger

A lightweight, domain-driven banking ledger in Java featuring **CQRS**, **Event Sourcing**, and **Bitemporal Logic** (tracking both **System Day** processing time and **Value Day** effective time).

---

## How to Run

1. Make the run script executable:
   ```bash
   chmod +x run.sh
   ```
2. Execute the build and run script:
   ```bash
   ./run.sh
   ```
3. At startup, choose your execution mode:
    * **Option 1**: Runs the automated 6-day simulation (demonstrating credits, debits, holds, settlements, retroactive value days, and installments).
    * **Option 2**: Enters the interactive command line prompt.

---

## Interactive Command Reference

Once in interactive mode, use these commands:

* `create <userId> <accountId> <AED|BHD> <bootstrapEventId>` — Initialize an account.
* `credit <accountId> <amount> <valueDay>` — Deposit funds.
* `debit <accountId> <amount> <valueDay>` — Withdraw funds (validates available balance).
* `auth <accountId> <authId> <amount> <valueDay>` — Place a temporary hold.
* `settle <accountId> <authId> <amount> <valueDay>` — Settle an existing hold.
* `reverse <accountId> <targetEventId> <valueDay> <systemDay> <reason>` — Audit/reverse a transaction.
* `advance` — Increment the system processing day by 1.
* `user-replay <userId>` — Print complete event stream history for a user.
*  balance <accountId> [targetDay] — Display the Ledger Balance, Available Balance, and fees for an account (defaults to current day if targetDay is omitted).
* `exit` — Quit the application.


## Example Commands Mirroring the Automated Simulation

To execute the automated scenario step-by-step in interactive mode:

### **Initialization & Day 1**
```text
create USER-1 ACC-001 AED EA
create USER-1 ACC-002 BHD EB
credit ACC-001 1200.00 1
debit ACC-001 950.00 1
balance ACC-001
```

### **Day 2**
```text
advance
auth ACC-001 Auth-A 200.00 2
balance ACC-001
```

### **Day 3**
```text
advance
credit ACC-001 400.00 3
balance ACC-001
```

### **Day 4**
```text
advance
settle ACC-001 Auth-A 185.00 4
settle ACC-001 Auth-Z 180.00 4
balance ACC-001
```

### **Day 5**
```text
advance
debit ACC-001 620.00 2
auth ACC-001 Auth-B 90.00 5
balance ACC-001 2
balance ACC-001
```

### **Day 6 (Reversal & Audit)**
```text
advance
reverse ACC-001 E7 2 6 Duplicate_debit_reversal
balance ACC-001
balance ACC-002
user-replay USER-1

exit
```
