package org.bsdevelopment.pluginutils.utilities;

import org.bsdevelopment.nbt.*;
import org.bsdevelopment.pluginutils.reflection.Reflection;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class NBTCodec {
    private static boolean initialized = false;

    private static Class<?> craftItemStackClass;
    private static Class<?> nmsItemStackClass;
    private static Class<?> asBukkitCopyParameterClass;
    private static Class<?> tagClass;
    private static Class<?> compoundTagClass;
    private static Class<?> tagParserClass;
    private static Class<?> dynamicOpsClass;
    private static Class<?> encoderClass;
    private static Class<?> decoderClass;
    private static Class<?> dataResultClass;

    private static Object nbtOpsInstance;
    private static Object itemStackCodec;

    public static boolean isSupported() {
        try {
            ensureInitialized();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String bukkitToNbtString(ItemStack bukkitStack) {
        if (bukkitStack == null || bukkitStack.getType().isAir()) return "{}";
        ensureInitialized();

        try {
            return encodeToMojangTag(bukkitStack).toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to convert ItemStack to NBT string", e);
        }
    }

    public static StorageTagCompound bukkitToStorageTag(ItemStack bukkitStack) {
        if (bukkitStack == null || bukkitStack.getType().isAir()) return new StorageTagCompound();
        ensureInitialized();

        try {
            return toStorageCompound(encodeToMojangTag(bukkitStack));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to convert ItemStack to StorageTagCompound", e);
        }
    }

    public static ItemStack nbtStringToBukkit(String snbt) {
        if (snbt == null || snbt.isBlank() || snbt.equals("{}")) return null;
        ensureInitialized();

        try {
            Object decoded = Reflection.resolveMethod(decoderClass, "parse", dynamicOpsClass, Object.class).invoke(itemStackCodec, nbtOpsInstance, parseMojangTag(snbt));
            Object nmsStack = Reflection.resolveMethod(dataResultClass, "getOrThrow").invoke(decoded);

            return (ItemStack) Reflection.resolveMethod(craftItemStackClass, "asBukkitCopy", asBukkitCopyParameterClass).invoke(null, nmsStack);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid SNBT ItemStack string", e);
        }
    }

    public static StorageTagCompound nbtStringToStorageTag(String snbt) {
        if (snbt == null || snbt.isBlank() || snbt.equals("{}")) return new StorageTagCompound();
        ensureInitialized();

        try {
            return toStorageCompound(parseMojangTag(snbt));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid SNBT string", e);
        }
    }

    public static ItemStack storageTagToBukkit(StorageTagCompound compound) {
        if (compound == null || compound.hasNoTags()) return null;
        return nbtStringToBukkit(compound.toString());
    }

    private static void ensureInitialized() {
        if (initialized) return;

        try {
            craftItemStackClass = Reflection.resolveCraftBukkitClass("inventory.CraftItemStack");
            nmsItemStackClass = Reflection.resolveMinecraftClass("ItemStack", "world.item");
            asBukkitCopyParameterClass = resolveAsBukkitCopyParameterClass();
            tagClass = Reflection.resolveMinecraftClass("Tag", "nbt");
            compoundTagClass = Reflection.resolveMinecraftClass("CompoundTag", "nbt");
            tagParserClass = Reflection.resolveMinecraftClass("TagParser", "nbt");
            dynamicOpsClass = Class.forName("com.mojang.serialization.DynamicOps");
            encoderClass = Class.forName("com.mojang.serialization.Encoder");
            decoderClass = Class.forName("com.mojang.serialization.Decoder");
            dataResultClass = Class.forName("com.mojang.serialization.DataResult");

            nbtOpsInstance = Reflection.retrieveField(Reflection.resolveMinecraftClass("NbtOps", "nbt"), "INSTANCE").get(null);
            itemStackCodec = Reflection.retrieveField(nmsItemStackClass, "CODEC").get(null);

            initialized = true;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize NBTCodec reflection handles", e);
        }
    }

    private static Class<?> resolveAsBukkitCopyParameterClass() {
        try {
            craftItemStackClass.getDeclaredMethod("asBukkitCopy", nmsItemStackClass);
            return nmsItemStackClass;
        } catch (NoSuchMethodException ex) {
            return Reflection.resolveMinecraftClass("ItemInstance", "world.item");
        }
    }

    private static Object encodeToMojangTag(ItemStack bukkitStack) throws Exception {
        Object nmsStack = Reflection.resolveMethod(craftItemStackClass, "asNMSCopy", ItemStack.class).invoke(null, bukkitStack);
        Object encoded = Reflection.resolveMethod(encoderClass, "encodeStart", dynamicOpsClass, Object.class).invoke(itemStackCodec, nbtOpsInstance, nmsStack);
        Object mojangTag = Reflection.resolveMethod(dataResultClass, "getOrThrow").invoke(encoded);

        if (!compoundTagClass.isInstance(mojangTag))
            throw new IllegalStateException("Expected a CompoundTag, got " + mojangTag.getClass());
        return mojangTag;
    }

    private static Object parseMojangTag(String snbt) throws Exception {
        return Reflection.resolveMethod(tagParserClass, "parseCompoundFully", String.class).invoke(null, snbt);
    }

    private static StorageBase toStorageBase(Object mojangTag) throws Exception {
        byte typeId = (byte) Reflection.resolveMethod(tagClass, "getId").invoke(mojangTag);

        return switch (typeId) {
            case 1 -> new StorageTagByte((byte) readTagValue(mojangTag, "asByte"));
            case 2 -> new StorageTagShort((short) readTagValue(mojangTag, "asShort"));
            case 3 -> new StorageTagInt((int) readTagValue(mojangTag, "asInt"));
            case 4 -> new StorageTagLong((long) readTagValue(mojangTag, "asLong"));
            case 5 -> new StorageTagFloat((float) readTagValue(mojangTag, "asFloat"));
            case 6 -> new StorageTagDouble((double) readTagValue(mojangTag, "asDouble"));
            case 7 -> new StorageTagByteArray((byte[]) readTagValue(mojangTag, "asByteArray"));
            case 8 -> new StorageTagString((String) readTagValue(mojangTag, "asString"));
            case 9 -> toStorageList(mojangTag);
            case 10 -> toStorageCompound(mojangTag);
            case 11 -> new StorageTagIntArray((int[]) readTagValue(mojangTag, "asIntArray"));
            case 12 -> new StorageTagLongArray((long[]) readTagValue(mojangTag, "asLongArray"));
            default -> new StorageTagCompound();
        };
    }

    private static StorageTagCompound toStorageCompound(Object mojangTag) throws Exception {
        StorageTagCompound compound = new StorageTagCompound();
        Set<?> keys = (Set<?>) Reflection.resolveMethod(compoundTagClass, "keySet").invoke(mojangTag);
        Method valueGetter = Reflection.resolveMethod(compoundTagClass, "get", String.class);

        for (Object key : keys) {
            Object child = valueGetter.invoke(mojangTag, key);
            if (child != null) compound.setTag((String) key, toStorageBase(child));
        }

        return compound;
    }

    private static StorageTagList toStorageList(Object mojangTag) throws Exception {
        StorageTagList list = new StorageTagList();

        for (Object child : (List<?>) mojangTag) list.appendTag(toStorageBase(child));

        return list;
    }

    private static Object readTagValue(Object mojangTag, String accessorName) throws Exception {
        Optional<?> value = (Optional<?>) Reflection.resolveMethod(tagClass, accessorName).invoke(mojangTag);
        return value.orElseThrow(() -> new IllegalStateException("Tag did not provide a value for " + accessorName));
    }
}
