package commons;

//AI used to undestand how to construct ENUM
public enum Unit {
    G("g", "g"),
    KG("kg", "kg"),
    ML("ml", "ml"),
    L("l", "l (litres)"),
    TSP("tsp", "tsp"),
    TBSP("tbsp", "tbsp"),
    CUP("cup", "cup"),
    PCS("pcs", "pcs (pieces)");

    private final String code;
    private final String label;

    Unit(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
