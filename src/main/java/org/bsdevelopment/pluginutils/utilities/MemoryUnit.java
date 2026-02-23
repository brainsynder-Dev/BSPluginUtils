/*
 * Copyright © 2025
 * BSDevelopment <https://bsdevelopment.org>
 */
package org.bsdevelopment.pluginutils.utilities;

import java.text.DecimalFormat;
import java.util.Objects;

public interface MemoryUnit {
    MemoryUnit BYTE     = new MemoryBuilder("B", 1L);
    MemoryUnit KILOBYTE = new MemoryBuilder("KB", 1L << 10); // 1024
    MemoryUnit MEGABYTE = new MemoryBuilder("MB", 1L << 20); // 1024^2
    MemoryUnit GIGABYTE = new MemoryBuilder("GB", 1L << 30); // 1024^3
    MemoryUnit TERABYTE = new MemoryBuilder("TB", 1L << 40); // 1024^4
    MemoryUnit PETABYTE = new MemoryBuilder("PB", 1L << 50); // 1024^5
    MemoryUnit EXABYTE  = new MemoryBuilder("EB", 1L << 60); // 1024^6

    MemoryUnit[] ORDERED = { BYTE, KILOBYTE, MEGABYTE, GIGABYTE, TERABYTE, PETABYTE, EXABYTE };

    long getUnitSize();

    String getSuffix();

    /**
     * Formats an input byte count into this unit (one decimal place for units above bytes).
     *
     * @param sizeInBytes raw bytes
     * @return formatted string, e.g. "1.5MB"
     */
    String format(long sizeInBytes);

    /**
     * Converts an input byte count into this unit (integer division / floor).
     *
     * @param sizeInBytes raw bytes
     * @return value in this unit, floored
     */
    long convert(long sizeInBytes);

    /**
     * @return the next larger unit (e.g., KB -> MB). If already at max, returns itself.
     */
    default MemoryUnit next() {
        return nextOf(this);
    }

    /**
     * @return the next smaller unit (e.g., MB -> KB). If already at min, returns itself.
     */
    default MemoryUnit previous() {
        return previousOf(this);
    }

    /**
     * Converts bytes to the given unit (integer division / floor).
     */
    static long convertTo(long sizeInBytes, MemoryUnit unit) {
        Objects.requireNonNull(unit, "unit");
        return unit.convert(sizeInBytes);
    }

    /**
     * Converts a value from one unit to another using integer division (floor).
     *
     * <p>Example: 1536 KB -> 1 MB</p>
     */
    static long convert(long value, MemoryUnit from, MemoryUnit to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        long bytes = toBytes(value, from);
        return to.convert(bytes);
    }

    /**
     * Converts a value from one unit to another as a double (keeps fractional part).
     *
     * <p>Example: 1536 KB -> 1.5 MB</p>
     */
    static double convertExact(long value, MemoryUnit from, MemoryUnit to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        long bytes = toBytes(value, from);
        if (to.getUnitSize() <= 0) return 0.0D;
        return (double) bytes / (double) to.getUnitSize();
    }

    /**
     * Converts a value to bytes (with overflow checking).
     */
    static long toBytes(long value, MemoryUnit from) {
        Objects.requireNonNull(from, "from");
        if (from.getUnitSize() <= 0) return 0L;
        return Math.multiplyExact(value, from.getUnitSize());
    }

    /**
     * Returns the "best fit" unit for a byte count.
     */
    static MemoryUnit bestFit(long sizeInBytes) {
        long abs = Math.abs(sizeInBytes);

        if (abs >= EXABYTE.getUnitSize())  return EXABYTE;
        if (abs >= PETABYTE.getUnitSize()) return PETABYTE;
        if (abs >= TERABYTE.getUnitSize()) return TERABYTE;
        if (abs >= GIGABYTE.getUnitSize()) return GIGABYTE;
        if (abs >= MEGABYTE.getUnitSize()) return MEGABYTE;
        if (abs >= KILOBYTE.getUnitSize()) return KILOBYTE;
        return BYTE;
    }

    /**
     * Formats a byte count using {@link #bestFit(long)}.
     */
    static String formatBest(long sizeInBytes) {
        return bestFit(sizeInBytes).format(sizeInBytes);
    }

    /**
     * Converts a value to the next larger unit (keeps fractional part).
     *
     * <p>Example: 1536 KB -> 1.5 MB</p>
     */
    static Scaled toNextUnit(long value, MemoryUnit from) {
        Objects.requireNonNull(from, "from");
        MemoryUnit next = nextOf(from);
        if (next == from) {
            return new Scaled((double) value, from);
        }
        double exact = convertExact(value, from, next);
        return new Scaled(exact, next);
    }

    /**
     * Converts a value to the previous smaller unit (keeps fractional part).
     *
     * <p>Example: 1.5 MB can't be represented as a long input here; this is mainly for whole values,
     * e.g. 2 MB -> 2048 KB (exact as double).</p>
     */
    static Scaled toPreviousUnit(long value, MemoryUnit from) {
        Objects.requireNonNull(from, "from");
        MemoryUnit prev = previousOf(from);
        if (prev == from) {
            return new Scaled((double) value, from);
        }
        // from -> bytes -> prev, exact
        double exact = convertExact(value, from, prev);
        return new Scaled(exact, prev);
    }

    /**
     * Finds the next unit in {@link #ORDERED}.
     */
    static MemoryUnit nextOf(MemoryUnit unit) {
        Objects.requireNonNull(unit, "unit");
        int idx = indexOf(unit);
        if (idx < 0 || idx >= ORDERED.length - 1) return unit;
        return ORDERED[idx + 1];
    }

    /**
     * Finds the previous unit in {@link #ORDERED}.
     */
    static MemoryUnit previousOf(MemoryUnit unit) {
        Objects.requireNonNull(unit, "unit");
        int idx = indexOf(unit);
        if (idx <= 0) return unit;
        return ORDERED[idx - 1];
    }

    static int indexOf(MemoryUnit unit) {
        for (int i = 0; i < ORDERED.length; i++) {
            if (ORDERED[i] == unit) return i;
        }
        for (int i = 0; i < ORDERED.length; i++) {
            MemoryUnit candidate = ORDERED[i];
            if (candidate.getUnitSize() == unit.getUnitSize() && Objects.equals(candidate.getSuffix(), unit.getSuffix())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * A scaled value + unit pair (useful when converting to next/previous unit).
     *
     * @param value the numeric value in {@link #unit()}
     * @param unit  the unit the value is expressed in
     */
    record Scaled(double value, MemoryUnit unit) {

        /**
         * Formats using one decimal place when needed, matching the unit formatting style.
         */
        public String format() {
            if (unit == null) return Double.toString(value);
            if (unit.getUnitSize() == 1L) return (long) value + unit.getSuffix();
            return MemoryBuilder.ONE_DECIMAL.get().format(value) + unit.getSuffix();
        }
    }

    class MemoryBuilder implements MemoryUnit {
        private static final ThreadLocal<DecimalFormat> ONE_DECIMAL = ThreadLocal.withInitial(() -> new DecimalFormat("0.#"));

        private final String suffix;
        private final long unitSize;

        MemoryBuilder(String suffix, long unitSize) {
            this.suffix = suffix;
            this.unitSize = unitSize;
        }

        @Override
        public long getUnitSize() {
            return unitSize;
        }

        @Override
        public String getSuffix() {
            return suffix;
        }

        @Override
        public String format(long sizeInBytes) {
            if (unitSize <= 0) return sizeInBytes + "B";

            if (unitSize == 1L) return sizeInBytes + suffix;

            double value = (double) sizeInBytes / (double) unitSize;
            return ONE_DECIMAL.get().format(value) + suffix;
        }

        @Override
        public long convert(long sizeInBytes) {
            if (unitSize <= 0) return 0;
            return sizeInBytes / unitSize;
        }
    }
}
