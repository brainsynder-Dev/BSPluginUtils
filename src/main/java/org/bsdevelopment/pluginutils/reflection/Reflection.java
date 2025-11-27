/*
 * Copyright © 2025
 * BSDevelopment <https://bsdevelopment.org>
 */

package org.bsdevelopment.pluginutils.reflection;

import io.papermc.lib.PaperLib;
import org.bsdevelopment.pluginutils.version.ServerVersion;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides reflection utilities for Minecraft and CraftBukkit internals.
 *
 * <p>This class offers methods to invoke methods, create instances, fetch fields,
 * and access other internals from Minecraft (NMS) and CraftBukkit classes.
 *
 * <p><b>Examples:</b>
 * <pre>
 * // Invoke a Minecraft method with parameters:
 * String result = Reflection.invokeMinecraftMethod("SomeClass", "subLoc", "methodName", targetObject, new Class[]{String.class}, "argument");
 *
 * // Create an instance of a Minecraft class:
 * Object instance = Reflection.createMinecraftInstance("SomeClass", "subLoc", new Class[]{int.class}, 42);
 *
 * // Retrieve a field value:
 * String fieldValue = Reflection.fetchMinecraftField("SomeClass", "subLoc", targetObject, "fieldName");
 *
 * // Invoke a CraftBukkit method:
 * Object result2 = Reflection.invokeCraftBukkitMethod("methodName", targetObject);
 *
 * // Get the underlying entity handle for a Bukkit Entity:
 * Object handle = Reflection.fetchEntityHandle(entity);
 * </pre>
 */
public class Reflection {
    private static final Map<Class<? extends Entity>, Method> ENTITY_HANDLE_CACHE = new ConcurrentHashMap<>();

    private static final Map<String, Class<?>> MINECRAFT_CLASS_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Class<?>> CRAFT_BUKKIT_CLASS_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Class<?>> BUKKIT_CLASS_CACHE = new ConcurrentHashMap<>();
    private static final Map<MethodKey, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Map<FieldKey, Field> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<ConstructorKey, Constructor<?>> CONSTRUCTOR_CACHE = new ConcurrentHashMap<>();

    /**
     * Invokes a Minecraft (NMS) method on the given target.
     *
     * @param className      The Minecraft class name.
     * @param subLocation    The subpackage location, if any.
     * @param methodName     The name of the method to invoke.
     * @param target         The target object, or null for static methods.
     * @param parameterTypes The types of the method parameters.
     * @param args           The arguments to pass to the method.
     * @param <T>            The expected return type.
     *
     * @return The result of the method call, or null if an error occurs.
     */
    public static <T> T invokeMinecraftMethod(String className, String subLocation, String methodName, Object target, Class<?>[] parameterTypes, Object... args) {
        try {
            Class<?> clazz = resolveMinecraftClass(className, subLocation);
            if (clazz == null) return null;

            Method method = resolveMethod(clazz, methodName, parameterTypes);
            if (method == null) return null;

            return (T) method.invoke(target, args);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Invokes a Minecraft (NMS) method using the target's class name.
     *
     * @param methodName     The method name.
     * @param target         The target object.
     * @param parameterTypes The parameter types.
     * @param args           The method arguments.
     * @param <T>            The expected return type.
     *
     * @return The result of the method call.
     */
    public static <T> T invokeMinecraftMethod(String methodName, Object target, Class<?>[] parameterTypes, Object... args) {
        Objects.requireNonNull(target, "Target cannot be null");

        return invokeMinecraftMethod(target.getClass().getSimpleName(), "", methodName, target, parameterTypes, args);
    }

    /**
     * Invokes a Minecraft (NMS) method with no parameters.
     *
     * @param methodName The method name.
     * @param target     The target object.
     * @param <T>        The expected return type.
     *
     * @return The result of the method call.
     */
    public static <T> T invokeMinecraftMethod(String methodName, Object target) {
        Objects.requireNonNull(target, "Target cannot be null");

        return invokeMinecraftMethod(methodName, target, new Class[0], new Object[0]);
    }

    // =====================================
    // Minecraft (NMS) Methods
    // =====================================

    /**
     * Creates a new instance of a Minecraft (NMS) class.
     *
     * @param className      The Minecraft class name.
     * @param subLocation    The subpackage location, if any.
     * @param parameterTypes The parameter types for the constructor.
     * @param args           The arguments to pass to the constructor.
     *
     * @return A new instance, or null if an error occurs.
     */
    public static Object createMinecraftInstance(String className, String subLocation, Class<?>[] parameterTypes, Object... args) {
        try {
            Class<?> clazz = resolveMinecraftClass(className, subLocation);
            if (clazz == null) return null;

            Constructor<?> constructor = retrieveConstructor(clazz, parameterTypes);
            if (constructor == null) return null;

            return constructor.newInstance(args);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Fetches the value of a Minecraft (NMS) field.
     *
     * @param className   The Minecraft class name.
     * @param subLocation The subpackage location, if any.
     * @param target      The target object from which to fetch the field.
     * @param fieldName   The name of the field.
     * @param <T>         The expected field type.
     *
     * @return The value of the field.
     *
     * @throws NoSuchFieldException   If the field does not exist.
     * @throws IllegalAccessException If the field is not accessible.
     */
    public static <T> T fetchMinecraftField(String className, String subLocation, Object target, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Class<?> clazz = resolveMinecraftClass(className, subLocation);
        if (clazz == null) throw new NoSuchFieldException("Class could not be resolved for: " + className);

        Field field = retrieveField(clazz, fieldName);
        if (field == null) throw new NoSuchFieldException(fieldName);

        return (T) field.get(target);
    }

    /**
     * Attempts to fetch the value of one of several Minecraft (NMS) fields.
     *
     * @param target      The target object.
     * @param subLocation The subpackage location, if any.
     * @param fieldNames  Possible field names.
     * @param <T>         The expected field type.
     *
     * @return The value of the first matching field, or null if none are found.
     */
    public static <T> T fetchMinecraftFields(Object target, String subLocation, String... fieldNames) {
        for (String fieldName : fieldNames) {
            try {
                return fetchMinecraftField(target.getClass().getSimpleName(), subLocation, target, fieldName);
            } catch (NoSuchFieldException | IllegalAccessException ignored) {}
        }

        return null;
    }

    /**
     * Invokes a CraftBukkit method on the given target.
     *
     * @param className      The CraftBukkit class name.
     * @param methodName     The method name.
     * @param target         The target object, or null for static methods.
     * @param parameterTypes The parameter types.
     * @param args           The method arguments.
     * @param <T>            The expected return type.
     *
     * @return The result of the method call, or null if an error occurs.
     */
    public static <T> T invokeCraftBukkitMethod(String className, String methodName, Object target, Class<?>[] parameterTypes, Object... args) {
        try {
            Class<?> clazz = resolveCraftBukkitClass(className);
            if (clazz == null) return null;

            Method method = resolveMethod(clazz, methodName, parameterTypes);
            if (method == null) return null;

            return (T) method.invoke(target, args);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Invokes a CraftBukkit method using the target's class.
     *
     * @param methodName     The method name.
     * @param target         The target object.
     * @param parameterTypes The parameter types.
     * @param args           The method arguments.
     * @param <T>            The expected return type.
     *
     * @return The result of the method call.
     */
    public static <T> T invokeCraftBukkitMethod(String methodName, Object target, Class<?>[] parameterTypes, Object... args) {
        String version = ServerVersion.getVersion().getSpigotNMS() + ".";
        if (PaperLib.isPaper()) version = "";

        String className = target.getClass().getName().replace("org.bukkit.craftbukkit." + version, "");

        return invokeCraftBukkitMethod(className, methodName, target, parameterTypes, args);
    }

    /**
     * Invokes a CraftBukkit method with no parameters.
     *
     * @param methodName The method name.
     * @param target     The target object.
     * @param <T>        The expected return type.
     *
     * @return The result of the method call.
     */
    public static <T> T invokeCraftBukkitMethod(String methodName, Object target) {
        return invokeCraftBukkitMethod(methodName, target, new Class[0], new Object[0]);
    }

    // =====================================
    // CraftBukkit Methods
    // =====================================

    /**
     * Creates a new instance of a CraftBukkit class.
     *
     * @param className      The CraftBukkit class name.
     * @param parameterTypes The parameter types for the constructor.
     * @param args           The constructor arguments.
     *
     * @return A new instance, or null if an error occurs.
     */
    public static Object createCraftBukkitInstance(String className, Class<?>[] parameterTypes, Object... args) {
        try {
            Class<?> clazz = resolveCraftBukkitClass(className);
            if (clazz == null) return null;

            Constructor<?> constructor = retrieveConstructor(clazz, parameterTypes);
            if (constructor == null) return null;

            return constructor.newInstance(args);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Fetches the value of a CraftBukkit field.
     *
     * @param className The CraftBukkit class name.
     * @param target    The target object.
     * @param fieldName The name of the field.
     * @param <T>       The expected field type.
     *
     * @return The value of the field, or null if an error occurs.
     */
    public static <T> T fetchCraftBukkitField(String className, Object target, String fieldName) {
        try {
            Class<?> clazz = resolveCraftBukkitClass(className);
            if (clazz == null) return null;

            Field field = retrieveField(clazz, fieldName);
            if (field == null) return null;

            return (T) field.get(target);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Fetches the value of a CraftBukkit field using the target's class.
     *
     * @param target    The target object.
     * @param fieldName The field name.
     * @param <T>       The expected field type.
     *
     * @return The value of the field.
     */
    public static <T> T fetchCraftBukkitField(Object target, String fieldName) {
        String version = ServerVersion.getVersion().getSpigotNMS() + ".";
        if (PaperLib.isPaper()) version = "";

        String className = target.getClass().getName().replace("org.bukkit.craftbukkit." + version, "");

        return fetchCraftBukkitField(className, target, fieldName);
    }

    /**
     * Fetches the value of a static CraftBukkit field.
     *
     * @param className The CraftBukkit class name.
     * @param fieldName The static field name.
     * @param <T>       The expected field type.
     *
     * @return The field value.
     */
    public static <T> T fetchCraftBukkitStaticField(String className, String fieldName) {
        return fetchCraftBukkitField(className, null, fieldName);
    }

    /**
     * Retrieves the underlying Minecraft entity handle for a Bukkit Entity.
     * This method caches the reflective lookup for performance.
     *
     * @param entity The Bukkit Entity.
     *
     * @return The underlying Minecraft entity.
     */
    public static Object fetchEntityHandle(Entity entity) {
        try {
            Method handleMethod = ENTITY_HANDLE_CACHE.computeIfAbsent(entity.getClass(), type -> {
                try {
                    Method method = type.getMethod("getHandle");
                    method.setAccessible(true);
                    return method;
                } catch (NoSuchMethodException ex) {
                    ex.printStackTrace();
                    return null;
                }
            });

            if (handleMethod == null) return null;

            return handleMethod.invoke(entity);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * Retrieves the value of a private field from a target object.
     *
     * @param fieldName The field name.
     * @param clazz     The class that declares the field.
     * @param target    The target object.
     * @param <T>       The expected field type.
     *
     * @return The field value, or null if an error occurs.
     */
    public static <T> T fetchPrivateField(String fieldName, Class<?> clazz, Object target) {
        try {
            Field field = retrieveField(clazz, fieldName);
            if (field == null) return null;

            return (T) field.get(target);
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * Extracts the value from a given field of a target object.
     *
     * @param field  The field.
     * @param target The target object.
     *
     * @return The value of the field.
     */
    public static Object extractFieldValue(Field field, Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Retrieves a field from the specified class.
     *
     * @param clazz     The class containing the field.
     * @param fieldName The field name.
     *
     * @return The field, or null if not found.
     */
    public static Field retrieveField(Class<?> clazz, String fieldName) {
        FieldKey key = new FieldKey(clazz, fieldName);
        Field cached = FIELD_CACHE.get(key);
        if (cached != null) return cached;

        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            FIELD_CACHE.put(key, field);
            return field;
        } catch (Throwable ex) {
            return null;
        }
    }

    // =====================================
    // Field Access Helpers
    // =====================================

    /**
     * Makes the given field accessible.
     *
     * @param field The field to modify.
     *
     * @return The accessible field.
     */
    public static Field makeFieldAccessible(Field field) {
        if (field == null) return null;

        try {
            field.setAccessible(true);
            return field;
        } catch (Throwable ex) {
            return null;
        }
    }

    /**
     * Retrieves a constructor from the specified class.
     *
     * @param clazz          The class.
     * @param parameterTypes The parameter types for the constructor.
     *
     * @return The constructor, or null if not found.
     */
    public static Constructor<?> retrieveConstructor(Class<?> clazz, Class<?>... parameterTypes) {
        ConstructorKey key = new ConstructorKey(clazz, Arrays.asList(parameterTypes));
        Constructor<?> cached = CONSTRUCTOR_CACHE.get(key);
        if (cached != null) return cached;

        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            CONSTRUCTOR_CACHE.put(key, constructor);
            return constructor;
        } catch (Throwable ex) {
            return null;
        }
    }

    /**
     * Executes a method on a target object with the provided arguments.
     *
     * @param method The method to execute.
     * @param target The target object.
     * @param args   The method arguments.
     * @param <T>    The expected return type.
     *
     * @return The result of the method call, or null if an error occurs.
     */
    public static <T> T executeMethod(Method method, Object target, Object... args) {
        try {
            return (T) method.invoke(target, args);
        } catch (IllegalAccessException ex) {
            return null;
        } catch (InvocationTargetException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Executes a method on a target object with no arguments.
     *
     * @param method The method to execute.
     * @param target The target object.
     * @param <T>    The expected return type.
     *
     * @return The result of the method call.
     */
    public static <T> T executeMethod(Method method, Object target) {
        return executeMethod(method, target, new Object[0]);
    }

    /**
     * Fetches the underlying Minecraft world handle from a Bukkit World.
     *
     * @param world The Bukkit World.
     * @param <T>   The expected return type.
     *
     * @return The Minecraft world handle.
     */
    public static <T> T fetchWorldHandle(World world) {
        Class<?> craftWorldClass = resolveCraftBukkitClass("CraftWorld");
        if (craftWorldClass == null) return null;

        Method handleMethod = resolveMethod(craftWorldClass, "getHandle");
        if (handleMethod == null) return null;

        return executeMethod(handleMethod, world);
    }

    /**
     * Finds the first field in a class that matches the expected type and type parameter patterns.
     *
     * @param clazz            The class to search.
     * @param expectedType     The expected field type.
     * @param typeNamePatterns Patterns for matching generic type names.
     *
     * @return The matching field, or null if none found.
     */
    public static Field findFirstMatchingField(Class<?> clazz, Class<?> expectedType, String... typeNamePatterns) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getType() == expectedType && field.getGenericType() instanceof ParameterizedType) {
                Type[] types = ((ParameterizedType) field.getGenericType()).getActualTypeArguments();

                if (typeNamePatterns.length == types.length) {
                    boolean matches = true;

                    for (int i = 0; i < typeNamePatterns.length; i++) {
                        if (!((Class<?>) types[i]).getName().matches(typeNamePatterns[i])) {
                            matches = false;
                            break;
                        }
                    }

                    if (matches) {
                        Field accessible = makeFieldAccessible(field);
                        if (accessible != null) FIELD_CACHE.putIfAbsent(new FieldKey(clazz, accessible.getName()), accessible);

                        return accessible;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Resolves a Minecraft (NMS) class using the provided name.
     *
     * @param name The Minecraft class name.
     *
     * @return The Class object, or null if not found.
     */
    public static Class<?> resolveMinecraftClass(String name) {
        String version = ServerVersion.getVersion().getSpigotNMS() + ".";
        if (PaperLib.isPaper()) version = "";

        String string = "net.minecraft.server." + version + name;

        Class<?> cached = MINECRAFT_CLASS_CACHE.get(string);
        if (cached != null) return cached;

        try {
            Class<?> clazz = Class.forName(string);
            MINECRAFT_CLASS_CACHE.put(string, clazz);
            return clazz;
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * Resolves a Minecraft (NMS) class using the provided name and sub-location.
     *
     * @param name        The Minecraft class name.
     * @param subLocation The subpackage location.
     *
     * @return The Class object.
     */
    public static Class<?> resolveMinecraftClass(String name, String subLocation) {
        String string;

        if (subLocation == null || subLocation.isEmpty()) {
            string = "net.minecraft." + name;
        } else {
            string = "net.minecraft." + subLocation + "." + name;
        }

        Class<?> cached = MINECRAFT_CLASS_CACHE.get(string);
        if (cached != null) return cached;

        try {
            Class<?> clazz = Class.forName(string);
            MINECRAFT_CLASS_CACHE.put(string, clazz);
            return clazz;
        } catch (ClassNotFoundException ex) {
            return resolveMinecraftClass(name);
        }
    }

    /**
     * Resolves a CraftBukkit class using the provided class name.
     *
     * @param className The CraftBukkit class name.
     *
     * @return The Class object, or null if not found.
     */
    public static Class<?> resolveCraftBukkitClass(String className) {
        String version = ServerVersion.getVersion().getSpigotNMS() + ".";
        if (PaperLib.isPaper()) version = "";

        String string = "org.bukkit.craftbukkit." + version + className;

        Class<?> cached = CRAFT_BUKKIT_CLASS_CACHE.get(string);
        if (cached != null) return cached;

        try {
            Class<?> clazz = Class.forName(string);
            CRAFT_BUKKIT_CLASS_CACHE.put(string, clazz);
            return clazz;
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * Resolves a Bukkit class using the provided class name.
     *
     * @param className The Bukkit class name.
     *
     * @return The Class object, or null if not found.
     */
    public static Class<?> resolveBukkitClass(String className) {
        String string = "org.bukkit." + className;

        Class<?> cached = BUKKIT_CLASS_CACHE.get(string);
        if (cached != null) return cached;

        try {
            Class<?> clazz = Class.forName(string);
            BUKKIT_CLASS_CACHE.put(string, clazz);
            return clazz;
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    // =====================================
    // Class Resolution Methods
    // =====================================

    /**
     * Resolves a method from a class with the specified name and parameter types.
     *
     * @param clazz          The class to search.
     * @param methodName     The method name.
     * @param parameterTypes The parameter types.
     *
     * @return The Method object, or null if not found.
     */
    public static Method resolveMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        MethodKey key = new MethodKey(clazz, methodName, Arrays.asList(parameterTypes));
        Method cached = METHOD_CACHE.get(key);
        if (cached != null) return cached;

        try {
            Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            METHOD_CACHE.put(key, method);
            return method;
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Attempts to resolve one of several method names from a class.
     *
     * @param clazz          The class to search.
     * @param methodNames    An array of possible method names.
     * @param parameterTypes The parameter types.
     *
     * @return The first matching Method object.
     */
    public static Method resolveMethod(Class<?> clazz, String[] methodNames, Class<?>... parameterTypes) {
        for (String name : methodNames) {
            Method method = resolveMethod(clazz, name, parameterTypes);
            if (method != null) return method;
        }

        try {
            throw new NoSuchMethodException("None of the methods " + Arrays.toString(methodNames) + " were found in class " + clazz.getSimpleName());
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * Retrieves the value of a private static field.
     *
     * @param clazz     The class that declares the field.
     * @param fieldName The static field name.
     *
     * @return The field value.
     *
     * @throws Exception if the field cannot be accessed.
     */
    public Object fetchPrivateStaticField(Class<?> clazz, String fieldName) throws Exception {
        Field field = retrieveField(clazz, fieldName);
        if (field == null) throw new NoSuchFieldException(fieldName);

        return field.get(null);
    }

    private record MethodKey(Class<?> owner, String name, List<Class<?>> parameterTypes) {}
    private record FieldKey(Class<?> owner, String name) {}
    private record ConstructorKey(Class<?> owner, List<Class<?>> parameterTypes) {}
}
