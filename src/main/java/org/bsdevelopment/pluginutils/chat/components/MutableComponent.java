package org.bsdevelopment.pluginutils.chat.components;

import org.bsdevelopment.pluginutils.chat.Component;
import org.bsdevelopment.pluginutils.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A mutable implementation of {@link Component} that lets you
 * modify its style and children after creation.
 *
 * <p>By default, it has no style (equivalent to {@code Style.empty()})
 * and no children. You can also initialize it from an existing component
 * to get a mutable copy.
 */
public class MutableComponent implements Component {
    private Style style;
    private final List<Component> children;

    /**
     * Creates an empty mutable component with no style and no children.
     */
    public MutableComponent() {
        this.style = Style.empty();
        this.children = new ArrayList<>();
    }

    /**
     * Creates a mutable component initialized with the given style
     * and no children.
     *
     * @param initialStyle the initial style to apply
     */
    public MutableComponent(Style initialStyle) {
        this.style = Objects.requireNonNull(initialStyle, "initialStyle");
        this.children = new ArrayList<>();
    }

    /**
     * Creates a mutable copy of the given component.
     * Copies its style and children list (shallow copy of children).
     *
     * @param original the component to copy
     */
    public MutableComponent(Component original) {
        Objects.requireNonNull(original, "original");
        this.style = original.style();
        this.children = new ArrayList<>(original.children());
    }

    @Override
    public Style style() {
        return style;
    }

    @Override
    public List<Component> children() {
        return children;
    }

    /**
     * Updates this component’s style.
     *
     * @param newStyle the new style to set
     */
    public void setStyle(Style newStyle) {
        this.style = Objects.requireNonNull(newStyle, "newStyle");
    }

    /**
     * Adds a child component to the end of this component’s children.
     *
     * @param component the child to add
     */
    public void addChild(Component component) {
        children.add(Objects.requireNonNull(component, "component"));
    }

    /**
     * Inserts a child component at the given index.
     *
     * @param index     position to insert at (0-based)
     * @param component the child to insert
     */
    public void addChild(int index, Component component) {
        children.add(index, Objects.requireNonNull(component, "component"));
    }

    /**
     * Replaces the child at the specified position with the given component.
     *
     * @param index     position of the child to replace
     * @param component the new child component
     * @return the previous child at that position
     */
    public Component setChild(int index, Component component) {
        return children.set(index, Objects.requireNonNull(component, "component"));
    }

    /**
     * Removes the first occurrence of the given child component.
     *
     * @param component the child to remove
     * @return true if the child was present and removed
     */
    public boolean removeChild(Component component) {
        return children.remove(component);
    }

    /**
     * Clears all children from this component.
     */
    public void clearChildren() {
        children.clear();
    }
}
