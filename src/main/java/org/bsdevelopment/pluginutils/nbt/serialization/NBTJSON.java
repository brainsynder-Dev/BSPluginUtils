package org.bsdevelopment.pluginutils.nbt.serialization;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.WriterConfig;
import org.bsdevelopment.pluginutils.nbt.Tag;
import org.bsdevelopment.pluginutils.nbt.TagType;
import org.bsdevelopment.pluginutils.nbt.types.*;
import org.bsdevelopment.pluginutils.nbt.types.array.ByteArrayTag;
import org.bsdevelopment.pluginutils.nbt.types.array.IntArrayTag;
import org.bsdevelopment.pluginutils.nbt.types.array.LongArrayTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NBTJSON {
    /**
     * Converts an NBT {@link Tag} into a JSON string.
     *
     * @param tag The NBT Tag to serialize.
     * @param pretty If true, output is pretty-printed (indented).
     * @return A JSON string representation.
     */
    public static String writeToJsonString(Tag tag, boolean pretty) {
        JsonValue rootValue = tagToJsonValue(tag);
        return pretty ? rootValue.toString(WriterConfig.PRETTY_PRINT) : rootValue.toString();
    }

    /**
     * Parses a JSON string back into an NBT {@link Tag}, using type inference.
     *
     * @param json The JSON string to parse.
     * @return The NBT Tag inferred from the JSON data.
     * @throws com.eclipsesource.json.ParseException if the JSON is invalid
     */
    public static Tag readFromJsonString(String json) {
        JsonValue rootValue = Json.parse(json);
        return jsonValueToTag(rootValue);
    }

    // ------------------------------------------------------------------------
    // A) Convert NBT -> JSON (Tag -> JsonValue)
    // ------------------------------------------------------------------------

    private static JsonValue tagToJsonValue(Tag tag) {
        return switch (tag.getType()) {
            case END -> Json.NULL;  // represent EndTag as null in JSON
            case BYTE -> Json.value(((ByteTag) tag).value());
            case SHORT -> Json.value(((ShortTag) tag).value());
            case INT -> Json.value(((IntTag) tag).value());
            case LONG -> Json.value(((LongTag) tag).value());
            case FLOAT -> Json.value(((FloatTag) tag).value());
            case DOUBLE -> Json.value(((DoubleTag) tag).value());
            case STRING -> Json.value(((StringTag) tag).value());
            case BYTE_ARRAY -> byteArrayToJsonValue((ByteArrayTag) tag);
            case INT_ARRAY -> intArrayToJsonValue((IntArrayTag) tag);
            case LONG_ARRAY -> longArrayToJsonValue((LongArrayTag) tag);
            case LIST -> listTagToJsonValue((ListTag) tag);
            case COMPOUND -> compoundTagToJsonValue((CompoundTag) tag);
        };
    }

    private static JsonArray byteArrayToJsonValue(ByteArrayTag ba) {
        JsonArray array = Json.array();
        for (byte b : ba.value()) {
            array.add(b);
        }
        return array;
    }

    private static JsonArray intArrayToJsonValue(IntArrayTag ia) {
        JsonArray array = Json.array();
        for (int i : ia.value()) {
            array.add(i);
        }
        return array;
    }

    private static JsonArray longArrayToJsonValue(LongArrayTag la) {
        JsonArray array = Json.array();
        for (long l : la.value()) {
            array.add(l);
        }
        return array;
    }

    private static JsonArray listTagToJsonValue(ListTag list) {
        JsonArray array = Json.array();
        for (Tag element : list.getValue()) {
            array.add(tagToJsonValue(element));
        }
        return array;
    }

    private static JsonObject compoundTagToJsonValue(CompoundTag compound) {
        JsonObject obj = Json.object();
        for (Map.Entry<String, Tag> e : compound.copyMap().entrySet()) {
            obj.add(e.getKey(), tagToJsonValue(e.getValue()));
        }
        return obj;
    }

    // ------------------------------------------------------------------------
    // B) Convert JSON -> NBT (JsonValue -> Tag) using type inference
    // ------------------------------------------------------------------------

    private static Tag jsonValueToTag(JsonValue value) {
        if (value.isNull()) {
            return EndTag.INSTANCE;
        } else if (value.isBoolean()) {
            // Convert booleans to ByteTag(1 or 0)
            boolean bool = value.asBoolean();
            return new ByteTag((byte) (bool ? 1 : 0));
        } else if (value.isNumber()) {
            return numberToTag(value.asDouble());
        } else if (value.isString()) {
            return new StringTag(value.asString());
        } else if (value.isArray()) {
            return jsonArrayToTag(value.asArray());
        } else if (value.isObject()) {
            return jsonObjectToCompound(value.asObject());
        } else {
            throw new IllegalArgumentException("Unexpected JSON element type");
        }
    }

    /**
     * Converts a double to the smallest numeric tag that fits the value:
     * <ul>
     *   <li>If integral, choose Byte/Short/Int/Long based on range</li>
     *   <li>If fractional, choose Float if no precision lost, else Double</li>
     * </ul>
     */
    private static Tag numberToTag(double d) {
        if (d % 1 == 0) {
            // integral
            long lv = (long) d;
            if (lv >= Byte.MIN_VALUE && lv <= Byte.MAX_VALUE) {
                return new ByteTag((byte) lv);
            } else if (lv >= Short.MIN_VALUE && lv <= Short.MAX_VALUE) {
                return new ShortTag((short) lv);
            } else if (lv >= Integer.MIN_VALUE && lv <= Integer.MAX_VALUE) {
                return new IntTag((int) lv);
            } else {
                return new LongTag(lv);
            }
        } else {
            // fractional -> compare float vs double
            float f = (float) d;
            if (Double.compare(d, f) == 0) {
                return new FloatTag(f);
            } else {
                return new DoubleTag(d);
            }
        }
    }

    /**
     * Infers whether a JSON array is suitable for a numeric array tag
     * or must be a ListTag.
     */
    private static Tag jsonArrayToTag(JsonArray arr) {
        if (arr.isEmpty()) {
            // empty => ListTag with no elements
            return new ListTag(TagType.END, List.of());
        }

        // Check if all elements are numbers
        boolean allNumbers = true;
        boolean allIntegral = true;

        for (JsonValue v : arr) {
            if (!v.isNumber()) {
                allNumbers = false;
                break;
            }
            // If it is a Number, check fractional
            double d = v.asDouble();
            if (d % 1 != 0) {
                allIntegral = false;
            }
        }

        if (!allNumbers) {
            // Mixed content => parse each element individually
            return arrayAsListTag(arr);
        }

        if (!allIntegral) {
            // Some fractional => must be ListTag of FloatTag/DoubleTag
            return arrayAsListTag(arr);
        }

        // all integral => choose ByteArrayTag, IntArrayTag, or LongArrayTag
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        long[] longs = new long[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            long v = (long) arr.get(i).asDouble();
            longs[i] = v;
            if (v < min) min = v;
            if (v > max) max = v;
        }

        // If all fit in byte range
        if (min >= Byte.MIN_VALUE && max <= Byte.MAX_VALUE) {
            byte[] bytes = new byte[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                bytes[i] = (byte) longs[i];
            }
            return new ByteArrayTag(bytes);
        }

        // If all fit in int range
        if (min >= Integer.MIN_VALUE && max <= Integer.MAX_VALUE) {
            int[] ints = new int[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                ints[i] = (int) longs[i];
            }
            return new IntArrayTag(ints);
        }

        // Otherwise -> LongArrayTag
        return new LongArrayTag(longs);
    }

    private static Tag arrayAsListTag(JsonArray arr) {
        List<Tag> elements = new ArrayList<>(arr.size());
        for (JsonValue v : arr) {
            elements.add(jsonValueToTag(v));
        }
        // We'll guess the "element type" as the first element's type if uniform
        if (elements.isEmpty()) {
            return new ListTag(TagType.END, List.of());
        }
        TagType first = elements.get(0).getType();
        // Check if all are the same
        for (Tag t : elements) {
            if (t.getType() != first) {
                first = TagType.END;
                break;
            }
        }
        return new ListTag(first, elements);
    }

    private static CompoundTag jsonObjectToCompound(JsonObject obj) {
        Map<String, Tag> map = new HashMap<>();
        for (String key : obj.names()) {
            JsonValue val = obj.get(key);
            Tag subTag = jsonValueToTag(val);
            map.put(key, subTag);
        }
        return new CompoundTag(map);
    }
}
