package org.bsdevelopment.pluginutils.nbt.serialization;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.WriterConfig;
import org.bsdevelopment.pluginutils.nbt.BasicData;
import org.bsdevelopment.pluginutils.nbt.TagType;
import org.bsdevelopment.pluginutils.nbt.types.ByteData;
import org.bsdevelopment.pluginutils.nbt.types.CompoundData;
import org.bsdevelopment.pluginutils.nbt.types.DoubleData;
import org.bsdevelopment.pluginutils.nbt.types.EndData;
import org.bsdevelopment.pluginutils.nbt.types.FloatData;
import org.bsdevelopment.pluginutils.nbt.types.IntData;
import org.bsdevelopment.pluginutils.nbt.types.ListData;
import org.bsdevelopment.pluginutils.nbt.types.LongData;
import org.bsdevelopment.pluginutils.nbt.types.ShortData;
import org.bsdevelopment.pluginutils.nbt.types.StringData;
import org.bsdevelopment.pluginutils.nbt.types.array.ByteArrayData;
import org.bsdevelopment.pluginutils.nbt.types.array.IntArrayData;
import org.bsdevelopment.pluginutils.nbt.types.array.LongArrayData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NBTJSON {
    /**
     * Converts an NBT {@link BasicData} into a JSON string.
     *
     * @param tag The NBT Tag to serialize.
     * @param pretty If true, output is pretty-printed (indented).
     * @return A JSON string representation.
     */
    public static String writeToJsonString(BasicData tag, boolean pretty) {
        JsonValue rootValue = tagToJsonValue(tag);
        return pretty ? rootValue.toString(WriterConfig.PRETTY_PRINT) : rootValue.toString();
    }

    /**
     * Parses a JSON string back into an NBT {@link BasicData}, using type inference.
     *
     * @param json The JSON string to parse.
     * @return The NBT Tag inferred from the JSON data.
     * @throws com.eclipsesource.json.ParseException if the JSON is invalid
     */
    public static BasicData readFromJsonString(String json) {
        JsonValue rootValue = Json.parse(json);
        return jsonValueToTag(rootValue);
    }

    // ------------------------------------------------------------------------
    // A) Convert NBT -> JSON (Tag -> JsonValue)
    // ------------------------------------------------------------------------

    private static JsonValue tagToJsonValue(BasicData tag) {
        return switch (tag.getType()) {
            case END -> Json.NULL;  // represent EndTag as null in JSON
            case BYTE -> Json.value(((ByteData) tag).value());
            case SHORT -> Json.value(((ShortData) tag).value());
            case INT -> Json.value(((IntData) tag).value());
            case LONG -> Json.value(((LongData) tag).value());
            case FLOAT -> Json.value(((FloatData) tag).value());
            case DOUBLE -> Json.value(((DoubleData) tag).value());
            case STRING -> Json.value(((StringData) tag).value());
            case BYTE_ARRAY -> byteArrayToJsonValue((ByteArrayData) tag);
            case INT_ARRAY -> intArrayToJsonValue((IntArrayData) tag);
            case LONG_ARRAY -> longArrayToJsonValue((LongArrayData) tag);
            case LIST -> listTagToJsonValue((ListData) tag);
            case COMPOUND -> compoundTagToJsonValue((CompoundData) tag);
        };
    }

    private static JsonArray byteArrayToJsonValue(ByteArrayData ba) {
        JsonArray array = Json.array();
        for (byte b : ba.value()) {
            array.add(b);
        }
        return array;
    }

    private static JsonArray intArrayToJsonValue(IntArrayData ia) {
        JsonArray array = Json.array();
        for (int i : ia.value()) {
            array.add(i);
        }
        return array;
    }

    private static JsonArray longArrayToJsonValue(LongArrayData la) {
        JsonArray array = Json.array();
        for (long l : la.value()) {
            array.add(l);
        }
        return array;
    }

    private static JsonArray listTagToJsonValue(ListData list) {
        JsonArray array = Json.array();
        for (BasicData element : list.getValue()) {
            array.add(tagToJsonValue(element));
        }
        return array;
    }

    private static JsonObject compoundTagToJsonValue(CompoundData compound) {
        JsonObject obj = Json.object();
        for (Map.Entry<String, BasicData> e : compound.copyMap().entrySet()) {
            obj.add(e.getKey(), tagToJsonValue(e.getValue()));
        }
        return obj;
    }

    // ------------------------------------------------------------------------
    // B) Convert JSON -> NBT (JsonValue -> Tag) using type inference
    // ------------------------------------------------------------------------

    private static BasicData jsonValueToTag(JsonValue value) {
        if (value.isNull()) {
            return EndData.INSTANCE;
        } else if (value.isBoolean()) {
            // Convert booleans to ByteTag(1 or 0)
            boolean bool = value.asBoolean();
            return new ByteData((byte) (bool ? 1 : 0));
        } else if (value.isNumber()) {
            return numberToTag(value.asDouble());
        } else if (value.isString()) {
            return new StringData(value.asString());
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
    private static BasicData numberToTag(double d) {
        if (d % 1 == 0) {
            // integral
            long lv = (long) d;
            if (lv >= Byte.MIN_VALUE && lv <= Byte.MAX_VALUE) {
                return new ByteData((byte) lv);
            } else if (lv >= Short.MIN_VALUE && lv <= Short.MAX_VALUE) {
                return new ShortData((short) lv);
            } else if (lv >= Integer.MIN_VALUE && lv <= Integer.MAX_VALUE) {
                return new IntData((int) lv);
            } else {
                return new LongData(lv);
            }
        } else {
            // fractional -> compare float vs double
            float f = (float) d;
            if (Double.compare(d, f) == 0) {
                return new FloatData(f);
            } else {
                return new DoubleData(d);
            }
        }
    }

    /**
     * Infers whether a JSON array is suitable for a numeric array tag
     * or must be a ListTag.
     */
    private static BasicData jsonArrayToTag(JsonArray arr) {
        if (arr.isEmpty()) {
            // empty => ListTag with no elements
            return new ListData(TagType.END, List.of());
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
            return new ByteArrayData(bytes);
        }

        // If all fit in int range
        if (min >= Integer.MIN_VALUE && max <= Integer.MAX_VALUE) {
            int[] ints = new int[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                ints[i] = (int) longs[i];
            }
            return new IntArrayData(ints);
        }

        // Otherwise -> LongArrayTag
        return new LongArrayData(longs);
    }

    private static BasicData arrayAsListTag(JsonArray arr) {
        List<BasicData> elements = new ArrayList<>(arr.size());
        for (JsonValue v : arr) {
            elements.add(jsonValueToTag(v));
        }
        // We'll guess the "element type" as the first element's type if uniform
        if (elements.isEmpty()) {
            return new ListData(TagType.END, List.of());
        }
        TagType first = elements.get(0).getType();
        // Check if all are the same
        for (BasicData t : elements) {
            if (t.getType() != first) {
                first = TagType.END;
                break;
            }
        }
        return new ListData(first, elements);
    }

    private static CompoundData jsonObjectToCompound(JsonObject obj) {
        Map<String, BasicData> map = new HashMap<>();
        for (String key : obj.names()) {
            JsonValue val = obj.get(key);
            BasicData subTag = jsonValueToTag(val);
            map.put(key, subTag);
        }
        return new CompoundData(map);
    }
}
