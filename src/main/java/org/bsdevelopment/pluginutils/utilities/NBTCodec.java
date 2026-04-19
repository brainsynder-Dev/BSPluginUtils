package org.bsdevelopment.pluginutils.utilities;

import org.bsdevelopment.nbt.*;
import org.bsdevelopment.pluginutils.reflection.Reflection;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.Set;

public class NBTCodec {
    private static boolean initialized = false;

    private static Class<?> craftItemStackClass;
    private static Class<?> nmsItemStackClass;
    private static Class<?> compoundTagClass;
    private static Class<?> tagParserClass;
    private static Class<?> dynamicOpsClass;
    private static Class<?> encoderClass;
    private static Class<?> decoderClass;
    private static Class<?> dataResultClass;

    private static Object nbtOpsInstance;
    private static Object itemStackCodec;

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
            return (StorageTagCompound) decodeFromMojang(encodeToMojangTag(bukkitStack));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to convert ItemStack to StorageTagCompound", e);
        }
    }

    public static ItemStack nbtStringToBukkit(String snbt) {
        if (snbt == null || snbt.isBlank() || snbt.equals("{}")) return null;
        ensureInitialized();
        try {
            Object compound = Reflection.resolveMethod(tagParserClass, new String[]{"parseCompoundFully", "parseCompound"}, String.class).invoke(null, snbt);
            Object dataResult = Reflection.resolveMethod(decoderClass, "parse", dynamicOpsClass, Object.class).invoke(itemStackCodec, nbtOpsInstance, compound);
            Object nms = Reflection.resolveMethod(dataResultClass, "getOrThrow").invoke(dataResult);

            return (ItemStack) Reflection.resolveMethod(craftItemStackClass, "asBukkitCopy", nmsItemStackClass).invoke(null, nms);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid SNBT ItemStack string", e);
        }
    }

    private static void ensureInitialized() {
        if (initialized) return;
        try {
            craftItemStackClass = Reflection.resolveCraftBukkitClass("inventory.CraftItemStack");
            nmsItemStackClass = Reflection.resolveMinecraftClass("ItemStack", "world.item");
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
            throw new RuntimeException("Failed to initialize NBTCodec reflection handles", e);
        }
    }

    private static Object encodeToMojangTag(ItemStack bukkitStack) throws Exception {
        Object nms = Reflection.resolveMethod(craftItemStackClass, "asNMSCopy", ItemStack.class).invoke(null, bukkitStack);
        Object dataResult = Reflection.resolveMethod(encoderClass, "encodeStart", dynamicOpsClass, Object.class).invoke(itemStackCodec, nbtOpsInstance, nms);
        Object tag = Reflection.resolveMethod(dataResultClass, "getOrThrow").invoke(dataResult);
        if (!compoundTagClass.isInstance(tag)) throw new IllegalStateException("Expected CompoundTag, got " + tag.getClass());
        return tag;
    }

    private static StorageBase decodeFromMojang(Object tag) throws Exception {
        Class<?> cls = tag.getClass();
        byte id = (byte) Reflection.resolveMethod(cls, "getId").invoke(tag);
        return switch (id) {
            case 1 -> new StorageTagByte(Byte.parseByte(getTagString(tag)));
            case 2 -> new StorageTagShort(Short.parseShort(getTagString(tag)));
            case 3 -> new StorageTagInt(Integer.parseInt(getTagString(tag)));
            case 4 -> new StorageTagLong(Long.parseLong(getTagString(tag)));
            case 5 -> new StorageTagFloat(Float.parseFloat(getTagString(tag)));
            case 6 -> new StorageTagDouble(Double.parseDouble(getTagString(tag)));
            case 7 -> new StorageTagByteArray((byte[]) Reflection.resolveMethod(cls, "getAsByteArray").invoke(tag));
            case 8 -> new StorageTagString(getTagString(tag));
            case 9 -> {
                StorageTagList list = new StorageTagList();
                int size = (int) Reflection.resolveMethod(cls, "size").invoke(tag);
                Method getter = Reflection.resolveMethod(cls, "get", int.class);
                for (int i = 0; i < size; i++) {
                    Object child = getter.invoke(tag, i);
                    list.appendTag(decodeFromMojang(child));
                }
                yield list;
            }
            case 10 -> {
                StorageTagCompound compound = new StorageTagCompound();
                Set<String> keys = (Set<String>) Reflection.resolveMethod(cls, new String[]{"getAllKeys", "keySet"}).invoke(tag);
                Method getter = Reflection.resolveMethod(cls, "get", String.class);
                for (String key : keys) {
                    Object child = getter.invoke(tag, key);
                    if (child != null) compound.setTag(key, decodeFromMojang(child));
                }
                yield compound;
            }
            case 11 -> new StorageTagIntArray((int[]) Reflection.resolveMethod(cls, "getAsIntArray").invoke(tag));
            case 12 -> new StorageTagLongArray((long[]) Reflection.resolveMethod(cls, "getAsLongArray").invoke(tag));
            default -> new StorageTagCompound();
        };
    }

    private static String getTagString(Object tag) throws Exception {
        return (String) Reflection.resolveMethod(tag.getClass(), "getAsString").invoke(tag);
    }
}
