package mal.ledger.enums;

public enum Currency {
    AED(2),
    BHD(3);

    private final int scale;

    Currency(int scale) {
        this.scale = scale;
    }

    public int getScale() {
        return scale;
    }

    public static Currency fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Currency code cannot be null");
        }
        try {
            return Currency.valueOf(code.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported or invalid currency code: " + code);
        }
    }
}