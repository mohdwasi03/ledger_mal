package mal.ledger.enums;

public enum FeeType {
    OVERDRAFT("Overdraft Assessment Fee"),
    FEE_REVERSAL("Compensating Fee Reversal");

    private final String description;

    FeeType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
