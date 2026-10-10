package mal.ledger.enums;

public enum InterestType {
    DAILY_ACCRUAL("Daily Interest Accrual"),
    CAPITALIZATION("Periodic Interest Capitalization");

    private final String description;

    InterestType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}