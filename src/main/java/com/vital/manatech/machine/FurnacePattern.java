package com.vital.manatech.machine;

/** Pure layout definition used by structure validation and regression checks. */
public final class FurnacePattern {
    private FurnacePattern() {}
    public static char cell(int x, int y, int z) {
        int borders = (x == 0 || x == 3 ? 1 : 0) + (y == 0 || y == 3 ? 1 : 0) + (z == 0 || z == 3 ? 1 : 0);
        if (borders >= 2) return 'B';
        if (borders == 0) return ' ';
        if (z == 0) return 'F';
        if (z == 3) return 'D';
        return 'A';
    }
    @FunctionalInterface public interface Matcher { boolean matches(int x, int y, int z, char expected); }
    public record Inspection(int matched, int x, int y, int z) { public boolean valid() { return matched == 64; } }
    public static Inspection inspect(Matcher matcher) {
        int matched = 0, errorX = -1, errorY = -1, errorZ = -1;
        for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) {
            if (matcher.matches(x, y, z, cell(x, y, z))) matched++;
            else if (errorX == -1) { errorX = x; errorY = y; errorZ = z; }
        }
        return new Inspection(matched, errorX, errorY, errorZ);
    }
}
