package org.bsdevelopment.pluginutils.command.arguments.range;

public record DoubleRange(double min, double max) {
    public boolean contains(double value) {
        return value >= min && value <= max;
    }

    @Override
    public String toString() {
        return min == max ? String.valueOf(min) : min + ".." + max;
    }
}
