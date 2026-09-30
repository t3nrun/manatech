package com.vital.manatech.magic;

/** Stable IDs shared by every layer and every table/stylus tier. */
public enum MagicModifier {
    CREATION("creation"), MOVEMENT("movement"), DENSITY("density"), COOLING("cooling"),
    AREA("area"), QUANTITY("quantity"), DEFENSE("defense"), LINK("link"), SPACE("space");
    private final String id;
    MagicModifier(String id) { this.id = id; }
    public String id() { return id; }
    public static MagicModifier bySymbol(int symbol) { return symbol >= 0 && symbol < values().length ? values()[symbol] : null; }
}
