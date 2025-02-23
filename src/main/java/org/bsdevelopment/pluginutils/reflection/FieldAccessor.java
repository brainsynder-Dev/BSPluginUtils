package org.bsdevelopment.pluginutils.reflection;

/**
 * Provides methods to access a field in an object via reflection.
 *
 * <p>This interface defines methods for retrieving and setting a field value on a target object,
 * as well as checking if a field exists.
 *
 * <p><b>Examples:</b>
 * <pre>
 * // Create a FieldAccessor for the field "myField" of type String in MyClass
 * FieldAccessor&lt;String&gt; accessor = FieldAccessor.getField(MyClass.class, "myField", String.class);
 *
 * // Retrieve the field value from an instance of MyClass
 * String value = accessor.get(myInstance);
 *
 * // Set a new value to the field in the instance
 * accessor.set(myInstance, "newValue");
 *
 * // Check if the field exists in the target instance
 * boolean exists = accessor.hasField(myInstance);
 * </pre>
 *
 * @param <T> the type of the field
 */
public interface FieldAccessor<T> {

    /**
     * Retrieves the field value from the given target object.
     *
     * <p><b>Example:</b>
     * <pre>
     * String value = accessor.get(myInstance);
     * </pre>
     *
     * @param target the object from which to retrieve the field value
     * @return the field value
     */
    T get(Object target);

    /**
     * Sets the field value on the given target object.
     *
     * <p><b>Example:</b>
     * <pre>
     * accessor.set(myInstance, "newValue");
     * </pre>
     *
     * @param target the object on which to set the field value
     * @param value  the new field value
     */
    void set(Object target, T value);

    /**
     * Checks if the target object has the field.
     *
     * <p><b>Example:</b>
     * <pre>
     * boolean exists = accessor.hasField(myInstance);
     * </pre>
     *
     * @param target the object to check
     * @return true if the field exists on the target's class, false otherwise
     */
    boolean hasField(Object target);

    /**
     * Creates a FieldAccessor for a field with the specified name and type in the target class.
     *
     * <p><b>Example:</b>
     * <pre>
     * FieldAccessor&lt;String&gt; accessor = FieldAccessor.getField(MyClass.class, "myField", String.class);
     * </pre>
     *
     * @param target    the class containing the field
     * @param name      the field name; may be null to match any field with the correct type
     * @param fieldType the expected field type
     * @param <T>       the type of the field
     * @return a FieldAccessor for the field
     */
    static <T> FieldAccessor<T> getField(Class<?> target, String name, Class<T> fieldType) {
        return getField(target, name, fieldType, 0);
    }

    // Private helper method; no JavaDocs.
    private static <T> FieldAccessor<T> getField(Class<?> target, String name, Class<T> fieldType, int index) {
        while (true) {
            for (var field : target.getDeclaredFields()) {

                if (((name == null) || (field.getName().equals(name)))
                        && fieldType.isAssignableFrom(field.getType())
                        && (index-- <= 0)) {

                    field.setAccessible(true);

                    return new FieldAccessor<T>() {
                        @Override
                        public T get(Object target) {
                            try {
                                return (T) field.get(target);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException("Cannot access reflection.", e);
                            }
                        }

                        @Override
                        public void set(Object target, T value) {
                            try {
                                field.set(target, value);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException("Cannot access reflection.", e);
                            }
                        }

                        @Override
                        public boolean hasField(Object target) {
                            return field.getDeclaringClass().isAssignableFrom(target.getClass());
                        }
                    };
                }
            }

            if (target.getSuperclass() != null) {
                target = target.getSuperclass();
                continue;
            }

            throw new IllegalArgumentException("Cannot find field with type " + fieldType);
        }
    }
}
