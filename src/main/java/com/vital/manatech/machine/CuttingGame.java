package com.vital.manatech.machine;

/** Server clock drives each cut. Clients send a click, never their claimed score. */
public final class CuttingGame {
    public static final int CUTS=6;
    public static int cursor(long tick) {
        int phase=(int)Math.floorMod(tick,80);
        return phase<=40 ? phase*100/40 : (80-phase)*100/40;
    }
    public static int target(int seed,int cut) { return 20+Math.floorMod(seed+cut*37,61); }
    public static boolean hits(int cursor,int target) { return Math.abs(cursor-target)<=12; }
    private CuttingGame() {}
}
