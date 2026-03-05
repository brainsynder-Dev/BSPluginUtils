package org.bsdevelopment.pluginutils.command.arguments.range;

public record IntegerRange(int min, int max) {
    public boolean contains(int value) {
        return value >= min && value <= max;
    }

    @Override
    public String toString() {
        return min == max ? String.valueOf(min) : min + ".." + max;
    }
}
