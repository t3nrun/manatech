package com.vital.manatech.rune;

/** Progression shared by the stylus and the inscription table. */
public final class RuneTier {
    /** Tool tiers unlock a prefix of the same twelve authored layers. */
    public static final int MAX = 7;
    private RuneTier() {}

    public static int layers(int tier) {
        return (int)Math.round(12.0 * Math.clamp(tier,1,MAX) / MAX);
    }
    public static int effectiveTier(int stylus, int table) { return Math.clamp(Math.min(stylus, table), 1, MAX); }
    public static boolean allowsScheme(int tier, int layer) { return layer >= 0 && layer < layers(tier); }
    /** Stylus tier limits peripheral marks; central Creation is separate. */
    public static int slots(int tier) {
        return switch(Math.clamp(tier,1,MAX)) { case 1 -> 2; case 2 -> 3; case 3 -> 4; case 4 -> 5; case 5 -> 6; default -> 8; };
    }
    public static java.util.List<Integer> visibleSlots(int tier) {
        return java.util.List.of(0,4,2,6,1,5,3,7).subList(0,slots(tier));
    }
    public static boolean allowsSymbols(int tier, DiagramLayer layer) {
        int marks=0;
        java.util.Set<Integer> occupied=new java.util.HashSet<>();
        for(var symbol:layer.symbols()) {
            if(!occupied.add(symbol.slot()) || symbol.slot()<0 || symbol.slot()>DiagramLayer.CENTER_SLOT) return false;
            if(symbol.slot()==DiagramLayer.CENTER_SLOT) { if(symbol.id()!=0) return false; }
            else if(symbol.id()==0 || !visibleSlots(tier).contains(symbol.slot()) || ++marks>slots(tier)) return false;
        }
        return true;
    }
    public static int symbols(int tier) { return com.vital.manatech.magic.MagicModifier.values().length; }
    public static int circleForLayer(int layer) {
        int count=Math.clamp(layer,1,12);
        for(int circle=1;circle<=MAX;circle++) if(count<=layers(circle))return circle;
        return MAX;
    }
    /** First authored layer (zero based) in a circle's group. */
    public static int firstLayer(int circle) { return circle<=1?0:layers(circle-1); }
}
