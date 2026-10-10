# Numeric Precision & Scale Documentation

## 1. Representation Choice: `BigDecimal` vs `long` (Fils)
The system transitioned from primitive fixed-point integer representations (`long` in fils) to Java's `java.math.BigDecimal` to support multi-currency scale configurations without custom conversion overhead.

### Precision Settings per Currency
* **AED (UAE Dirham):** 2 decimal places (`setScale(2, RoundingMode.HALF_UP)`).
* **BHD (Bahraini Dinar):** 3 decimal places (`setScale(3, RoundingMode.HALF_UP)`).

---

## 2. Rounding Modes: Code Mapping & Rationale

| Rounding Mode |  Usage | Rationale & Why Selected | Financial & Customer Impact |
|---|---|---|---|
| **`RoundingMode.HALF_UP`** | Daily interest calculation | Standard commercial rounding. Rounds $\ge 0.5$ **UP**. Selected for daily interest credits to ensure accounting compliance. | **Interest Credits:** `0.115 AED` $\rightarrow$ `0.12 AED` (**Customer gains +0.005**).<br>**Fees:** `10.005 AED` $\rightarrow$ `10.01 AED` (**Customer loses -0.005**). |
| **`RoundingMode.DOWN`** | Instalment Allocation Strategy | Strict truncation toward zero (drops extra decimals). Used for base instalment divisions to prevent artificial currency creation. | **Instalments:** Splitting `10.000 BHD` into 3 parts truncates intermediate parts to `3.333 BHD`. Final instalment absorbs residual (`3.334 BHD`). **Exact zero loss/gain**. |
| **`RoundingMode.HALF_DOWN`** |  Overdraft fee calculation | Rounds $\le 0.5$ **DOWN** toward zero (rounds UP only if $> 0.5$). Applied in fee calculations where boundary cases must favor the customer. | **Fee Debits:** `10.005 AED` $\rightarrow$ `10.00 AED` (**Customer gains +0.005** by paying lower fee). |

---

## 3. Mathematical Models

### 1. Daily Interest Accrual ($0.04\%$)
* **Daily Rate:** `0.0004`
* **Rule:** Calculated strictly on positive closing ledger balances at End of Day.
* **Rounding Mode:** `RoundingMode.HALF_UP` applied to each daily accrual to match currency scale.
* **Capitalization:** Daily accruals accumulate in memory and capitalize as a single `INTEREST` credit event on Day 6.

$$\text{Daily Accrual} = \text{round}_{\text{HALF\_UP}}(\text{Ledger Balance} \times 0.0004)$$

#### Example (AED Account):
* Day 4 Closing Balance: `285.00 AED`
* Raw Interest: $285.00 \times 0.0004 = 0.1140 \text{ AED}$
* Rounded (`HALF_UP`): **`0.11 AED`**

---

### 2. Absorbing Instalments (BHD Credit Penny-Drop Strategy)
Splitting `10.000 BHD` across 3 instalments yields repeating decimals ($3.3333\dots$). The system applies a penny-drop allocation using `RoundingMode.DOWN` to eliminate balance leakage:

1. **Base Instalment Calculation:** Truncate using `RoundingMode.DOWN` down to scale 3 ($10.000 / 3 = 3.333 \text{ BHD}$).
2. **Absorbing Final Instalment:** Subtract accumulated base amounts from the total credit:
   $$\text{Final Instalment} = 10.000 - (3.333 \times 2) = \mathbf{3.334 \text{ BHD}}$$

* **Instalment 1:** `3.333 BHD`
* **Instalment 2:** `3.333 BHD`
* **Instalment 3 (Absorbing):** `3.334 BHD`
* **Net Customer & Bank Impact:** **EXACT ZERO LOSS/GAIN** ($3.333 + 3.333 + 3.334 = 10.000 \text{ BHD}$).