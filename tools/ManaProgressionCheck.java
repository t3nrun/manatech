import com.vital.manatech.magic.ManaProgression;

/** Boundary checks: rank transitions, integer absorption and large stone charges. */
public class ManaProgressionCheck {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        long[] charges = {128L, 16384L, 2097152L, 268435456L, 34359738368L, 4398046511104L};
        for (int stage = 1; stage <= 6; stage++) check(ManaProgression.power(stage) == charges[stage - 1], "Stone stage " + stage);
        check(ManaProgression.rank(10) == 1 && ManaProgression.rank(11) == 2, "First rank boundary");
        check(ManaProgression.rank(89) == 6 && ManaProgression.rank(90) == 7, "Emperor rank boundary");
        check(ManaProgression.levelAt(1, 383) == 1 && ManaProgression.levelAt(1, 384) == 2, "1/30 absorption threshold");
        check(ManaProgression.levelAt(1, 3839) == 10, "Level before rank promotion");
        check(ManaProgression.maximum(1) == 128 && ManaProgression.maximum(11) == 16384, "Maximum mana at rank starts");
        for (int level = 1; level <= 100; level++) {
            int rank = ManaProgression.rank(level);
            check(ManaProgression.levelAt(rank, ManaProgression.absorptionAtLevel(level)) == level, "Initial level " + level);
            check(Double.isFinite(ManaProgression.maximum(level)), "Finite maximum");
            if (level > 1) check(ManaProgression.maximum(level) >= ManaProgression.maximum(level - 1), "Monotonic mana");
        }
        check(ManaProgression.levelAt(7, ManaProgression.rankAbsorption(7)) == 100, "Final level");
        check(Math.multiplyExact(charges[5], 2) == 8796093022208L, "Highest crafting cost");
        System.out.println("Mana progression: all 100 levels and 6 stone charges passed.");
    }
}
