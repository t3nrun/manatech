package com.vital.manatech.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Shared display format; stored mana and arithmetic keep their full precision. */
public final class CompactNumbers {
    private static final String[] SUFFIXES = {"", "k", "m", "b", "t", "qa", "qi"};

    private CompactNumbers() {}

    public static String format(double value) {
        if (Double.isNaN(value)) return "—";
        if (Double.isInfinite(value)) return value < 0 ? "−∞" : "∞";
        double scaled = Math.abs(value);
        int unit = 0;
        while (scaled >= 1000 && unit < SUFFIXES.length - 1) {
            scaled /= 1000;
            unit++;
        }
        if (scaled >= 1000) return String.format(Locale.ROOT, "%.2e", value);
        if (unit == 0) return Long.toString((long)value);
        BigDecimal rounded = BigDecimal.valueOf(scaled).setScale(2, RoundingMode.HALF_UP);
        if (rounded.compareTo(BigDecimal.valueOf(1000)) >= 0 && unit < SUFFIXES.length - 1) {
            rounded = rounded.movePointLeft(3);
            unit++;
        }
        return (value < 0 ? "-" : "") + rounded.stripTrailingZeros().toPlainString() + SUFFIXES[unit];
    }
}
