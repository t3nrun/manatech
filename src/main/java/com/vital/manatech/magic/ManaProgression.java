package com.vital.manatech.magic;

/** Integer progression shared by saves, stone charges and the HUD. */
public final class ManaProgression {
    private static final int[] STARTS = {1, 11, 21, 31, 41, 51, 90};
    private static final int[] ENDS = {10, 20, 30, 40, 50, 89, 100};
    private ManaProgression() {}

    public static long power(int stage) {
        if (stage < 1 || stage > 7) throw new IllegalArgumentException("Mana stage: " + stage);
        return 1L << (7 * stage);
    }
    public static int rank(int level) {
        for (int i = 0; i < ENDS.length; i++) if (level <= ENDS[i]) return i + 1;
        return 7;
    }
    public static int firstLevel(int rank) { return STARTS[Math.clamp(rank, 1, 7) - 1]; }
    public static int lastLevel(int rank) { return ENDS[Math.clamp(rank, 1, 7) - 1]; }
    public static double maximum(int level) {
        level = Math.clamp(level, 1, 100);
        int rank = rank(level);
        long base = power(rank);
        if (rank == 7) return base;
        double fraction = (double)(level - firstLevel(rank)) / (lastLevel(rank) - firstLevel(rank) + 1);
        return base + (power(rank + 1) - base) * fraction;
    }
    /** Thirty absorbed MP are one progression MP; keep the remainder exactly. */
    public static long rankAbsorption(int rank) { return Math.multiplyExact(power(rank), 30); }
    public static int levelAt(int rank, long absorbed) {
        int first = firstLevel(rank), last = lastLevel(rank);
        int steps = rank == 7 ? last - first : last - first + 1;
        long threshold = rankAbsorption(rank);
        int offset = (int)Math.min(steps, Math.max(0, absorbed) * steps / threshold);
        return Math.min(last, first + offset);
    }
    public static long absorptionAtLevel(int level) {
        int rank = rank(level);
        int steps = rank == 7 ? lastLevel(rank) - firstLevel(rank) : lastLevel(rank) - firstLevel(rank) + 1;
        long numerator = rankAbsorption(rank) * (level - firstLevel(rank));
        return (numerator + steps - 1) / steps;
    }
}
