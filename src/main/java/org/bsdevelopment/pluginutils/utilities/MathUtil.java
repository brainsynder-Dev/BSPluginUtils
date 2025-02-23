package org.bsdevelopment.pluginutils.utilities;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.Random;

/**
 * A utility class providing various mathematical operations and utilities.
 * <p>
 * This class merges functionalities from several original utility classes into a single class,
 * cleaned up to remove duplicated or similar methods, and renamed for clarity.
 * <p>
 * Usage example:
 * <blockquote><pre>
 * // Clamping a value
 * double clamped = MathUtil.clamp(10.5, 5.0); // clamped = 5.0
 *
 * // Getting the floor of a double
 * int floored = MathUtil.floor(3.9); // floored = 3
 *
 * // Normalizing a vector
 * Vector v = new Vector(10, 2, 5);
 * MathUtil.normalize(v); // v is now unit length
 * </pre></blockquote>
 */
public final class MathUtil {
    // -------------------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------------------

    public static final float FLOAT_ROUNDING_ERROR = 1.0E-6F;
    public static final float PI = 3.1415927F;
    public static final float PI2 = 6.2831855F;
    public static final float SQRT_3 = 1.7320508F;
    public static final float E = 2.7182817F;

    private static final int SIN_BITS = 14;
    private static final int SIN_MASK = 16383;
    private static final int SIN_COUNT = 16384;
    private static final float RAD_FULL = 6.2831855F;
    private static final float DEG_FULL = 360.0F;
    private static final float RAD_TO_INDEX = 2607.5945F;
    private static final float DEG_TO_INDEX = 45.511112F;

    public static final float DEG_TO_RAD = 0.017453292F;
    public static final float RAD_TO_DEG = 57.29577951F;

    private static final int ATAN2_BITS = 7;
    private static final int ATAN2_COUNT = 16384;
    private static final int ATAN2_DIM = (int) Math.sqrt(16384.0D);
    private static final float INV_ATAN2_DIM_MINUS_1;

    private static final int CHUNK_BITS = 4;
    private static final int CHUNK_VALUES = 16;

    private static final Random RANDOM = new Random();

    static {
        INV_ATAN2_DIM_MINUS_1 = 1.0F / (ATAN2_DIM - 1);
    }

    // -------------------------------------------------------------------------------------
    // Basic numeric operations (floor, ceil, round, etc.)
    // -------------------------------------------------------------------------------------

    /**
     * Floors a double value to the nearest integer towards negative infinity.
     * <p>Example usage:
     * <blockquote><pre>
     * int f = MathUtil.floor(3.9); // = 3
     * </pre></blockquote>
     *
     * @param value the value
     * @return floored value
     */
    public static int floor(double value) {
        int i = (int) value;
        return (value < i) ? i - 1 : i;
    }

    /**
     * Ceils a double value to the nearest integer towards positive infinity.
     * <p>Example usage:
     * <blockquote><pre>
     * int c = MathUtil.ceil(3.1); // = 4
     * </pre></blockquote>
     *
     * @param value the value
     * @return ceiled value
     */
    public static int ceil(double value) {
        return -floor(-value);
    }

    /**
     * Rounds a double value to the given number of decimal places.
     * <p>Example usage:
     * <blockquote><pre>
     * double rounded = MathUtil.round(3.14159, 2); // e.g. 3.14
     * </pre></blockquote>
     *
     * @param value    the value
     * @param decimals number of decimals
     * @return rounded value
     */
    public static double round(double value, int decimals) {
        double p = Math.pow(10, decimals);
        return Math.round(value * p) / p;
    }

    /**
     * Trims a double value to a certain number of decimal places.
     * <p>Example usage:
     * <blockquote><pre>
     * double trimmed = MathUtil.trim(2, 3.14159); // e.g. 3.14
     * </pre></blockquote>
     *
     * @param decimals number of decimal places
     * @param d        the value to trim
     * @return trimmed value
     */
    public static double trim(int decimals, double d) {
        StringBuilder format = new StringBuilder("#.#");
        for (int i = 1; i < decimals; i++) {
            format.append("#");
        }
        DecimalFormat df = new DecimalFormat(format.toString());
        return Double.parseDouble(df.format(d));
    }

    /**
     * Returns 0.0 if the given value is NaN, otherwise returns the value.
     * <p>Example usage:
     * <blockquote><pre>
     * double result = MathUtil.fixNaN(Double.NaN); // result = 0.0
     * </pre></blockquote>
     *
     * @param value value to check
     * @return value or 0.0 if NaN
     */
    public static double fixNaN(double value) {
        return fixNaN(value, 0.0);
    }

    /**
     * Returns {@code def} if the given value is NaN, otherwise returns the value.
     * <p>Example usage:
     * <blockquote><pre>
     * double result = MathUtil.fixNaN(Double.NaN, 5.0); // result = 5.0
     * </pre></blockquote>
     *
     * @param value value to check
     * @param def   default value
     * @return value or {@code def} if NaN
     */
    public static double fixNaN(double value, double def) {
        return Double.isNaN(value) ? def : value;
    }

    /**
     * Checks if a float is close to zero.
     * <p>Example usage:
     * <blockquote><pre>
     * boolean z = MathUtil.isZero(0.0000001f); // true
     * </pre></blockquote>
     *
     * @param value the value
     * @return true if near zero
     */
    public static boolean isZero(float value) {
        return Math.abs(value) <= FLOAT_ROUNDING_ERROR;
    }

    /**
     * Checks if a float is close to zero, within a custom tolerance.
     * <p>Example usage:
     * <blockquote><pre>
     * boolean z = MathUtil.isZero(0.001f, 0.01f); // true
     * </pre></blockquote>
     *
     * @param value     the value
     * @param tolerance tolerance
     * @return true if near zero
     */
    public static boolean isZero(float value, float tolerance) {
        return Math.abs(value) <= tolerance;
    }

    /**
     * Checks if two floats are approximately equal.
     * <p>Example usage:
     * <blockquote><pre>
     * boolean eq = MathUtil.isEqual(1.0f, 1.0000001f); // true
     * </pre></blockquote>
     *
     * @param a first
     * @param b second
     * @return true if near equal
     */
    public static boolean isEqual(float a, float b) {
        return Math.abs(a - b) <= FLOAT_ROUNDING_ERROR;
    }

    /**
     * Checks if two floats are approximately equal, within a custom tolerance.
     * <p>Example usage:
     * <blockquote><pre>
     * boolean eq = MathUtil.isEqual(1.0f, 1.1f, 0.2f); // true
     * </pre></blockquote>
     *
     * @param a         first
     * @param b         second
     * @param tolerance tolerance
     * @return true if near equal
     */
    public static boolean isEqual(float a, float b, float tolerance) {
        return Math.abs(a - b) <= tolerance;
    }

    // -------------------------------------------------------------------------------------
    // Clamping
    // -------------------------------------------------------------------------------------

    /**
     * Clamps the value between the specified minimum and maximum.
     * <p>Example usage:
     * <blockquote><pre>
     * double clamped = MathUtil.clamp(5.7, 2.0, 4.0); // 4.0
     * </pre></blockquote>
     *
     * @param value the value
     * @param min   minimum
     * @param max   maximum
     * @return clamped value
     */
    public static double clamp(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    /**
     * Clamps the float value between the specified minimum and maximum.
     * <p>Example usage:
     * <blockquote><pre>
     * float clamped = MathUtil.clamp(5.7f, 2.0f, 4.0f); // 4.0f
     * </pre></blockquote>
     *
     * @param value the value
     * @param min   min
     * @param max   max
     * @return clamped value
     */
    public static float clamp(float value, float min, float max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    /**
     * Clamps the integer value between the specified minimum and maximum.
     * <p>Example usage:
     * <blockquote><pre>
     * int clamped = MathUtil.clamp(10, 2, 8); // 8
     * </pre></blockquote>
     *
     * @param value the value
     * @param min   min
     * @param max   max
     * @return clamped value
     */
    public static int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    // -------------------------------------------------------------------------------------
    // Angle methods
    // -------------------------------------------------------------------------------------

    /**
     * Wraps the angle (in degrees) to be between -180 and 180.
     * <p>Example usage:
     * <blockquote><pre>
     * float wrapped = MathUtil.wrapAngleDeg(270f); // -90f
     * </pre></blockquote>
     *
     * @param angle angle in degrees
     * @return angle wrapped to the range [-180, 180]
     */
    public static float wrapAngleDeg(float angle) {
        while (angle <= -180f) angle += 360f;
        while (angle > 180f) angle -= 360f;
        return angle;
    }

    /**
     * Returns the absolute angle difference (in degrees) between two angles.
     * <p>Example usage:
     * <blockquote><pre>
     * float diff = MathUtil.angleDifferenceDeg(30f, 90f); // 60f
     * </pre></blockquote>
     *
     * @param angle1 first angle
     * @param angle2 second angle
     * @return absolute angle difference
     */
    public static float angleDifferenceDeg(float angle1, float angle2) {
        float wrapped = wrapAngleDeg(angle1 - angle2);
        return Math.abs(wrapped);
    }

    // -------------------------------------------------------------------------------------
    // Vector lengths, offsets, etc.
    // -------------------------------------------------------------------------------------

    /**
     * Returns the sum of squares of all given values.
     * <p>Example usage:
     * <blockquote><pre>
     * double ls = MathUtil.lengthSquared(3,4); // 3^2 + 4^2 = 25
     * </pre></blockquote>
     *
     * @param values numbers to square and sum
     * @return sum of squares
     */
    public static double lengthSquared(double... values) {
        double total = 0;
        for (double v : values) {
            total += v * v;
        }
        return total;
    }

    /**
     * Returns the square root of {@link #lengthSquared(double...)}.
     * <p>Example usage:
     * <blockquote><pre>
     * double length = MathUtil.length(3,4); // 5
     * </pre></blockquote>
     *
     * @param values numbers
     * @return sqrt of sum of squares
     */
    public static double length(double... values) {
        return Math.sqrt(lengthSquared(values));
    }

    /**
     * Sets the length of a Vector to the specified length.
     * <p>Example usage:
     * <blockquote><pre>
     * Vector v = new Vector(3,4,0);
     * MathUtil.setVectorLength(v, 10.0); // now length is 10
     * </pre></blockquote>
     *
     * @param vector the vector
     * @param length the new length (may be negative to reverse direction)
     */
    public static void setVectorLength(Vector vector, double length) {
        setVectorLengthSquared(vector, Math.signum(length) * length * length);
    }

    /**
     * Sets the length-squared of a Vector to the specified amount.
     * <p>Example usage:
     * <blockquote><pre>
     * Vector v = new Vector(3,4,0);
     * MathUtil.setVectorLengthSquared(v, 25.0); // now length is 5
     * </pre></blockquote>
     *
     * @param vector        the vector
     * @param lengthSquared the new length squared (may be negative to reverse direction)
     */
    public static void setVectorLengthSquared(Vector vector, double lengthSquared) {
        double vlen = vector.lengthSquared();
        if (Math.abs(vlen) > 0.0001) {
            if (lengthSquared < 0) {
                vector.multiply(-Math.sqrt(-lengthSquared / vlen));
            } else {
                vector.multiply(Math.sqrt(lengthSquared / vlen));
            }
        }
    }

    /**
     * Calculates the distance (in 3D) between two vectors.
     * <p>Example usage:
     * <blockquote><pre>
     * double dist = MathUtil.distance3D(vecA, vecB);
     * </pre></blockquote>
     *
     * @param a first vector
     * @param b second vector
     * @return distance
     */
    public static double distance3D(Vector a, Vector b) {
        return a.clone().subtract(b).length();
    }

    /**
     * Calculates the distance (in 2D, ignoring Y) between two vectors.
     * <p>Example usage:
     * <blockquote><pre>
     * double dist2d = MathUtil.distance2D(vecA, vecB);
     * </pre></blockquote>
     *
     * @param a first vector
     * @param b second vector
     * @return distance in 2D
     */
    public static double distance2D(Vector a, Vector b) {
        Vector a2 = a.clone();
        Vector b2 = b.clone();
        a2.setY(0);
        b2.setY(0);
        return a2.subtract(b2).length();
    }

    // -------------------------------------------------------------------------------------
    // Random operations
    // -------------------------------------------------------------------------------------

    /**
     * Returns a random integer in [min..max].
     * <p>Example usage:
     * <blockquote><pre>
     * int rand = MathUtil.randomInt(2, 5); // e.g. 3
     * </pre></blockquote>
     *
     * @param min lower bound
     * @param max upper bound
     * @return random int
     */
    public static int randomInt(int min, int max) {
        if (min > max) {
            int tmp = min;
            min = max;
            max = tmp;
        }
        return min + RANDOM.nextInt(max - min + 1);
    }

    /**
     * Returns a random float in [0..1].
     * <p>Example usage:
     * <blockquote><pre>
     * float f = MathUtil.randomFloat();
     * </pre></blockquote>
     *
     * @return random float
     */
    public static float randomFloat() {
        return RANDOM.nextFloat();
    }

    /**
     * Returns a random float in [min..max].
     * <p>Example usage:
     * <blockquote><pre>
     * float f = MathUtil.randomFloat(2f, 5f); // e.g. 3.2
     * </pre></blockquote>
     *
     * @param min lower bound
     * @param max upper bound
     * @return random float
     */
    public static float randomFloat(float min, float max) {
        if (min > max) {
            float tmp = min;
            min = max;
            max = tmp;
        }
        return min + RANDOM.nextFloat() * (max - min);
    }

    /**
     * Returns a random double in [min..max].
     * <p>Example usage:
     * <blockquote><pre>
     * double rand = MathUtil.randomDouble(2.0, 5.0);
     * </pre></blockquote>
     *
     * @param min lower bound
     * @param max upper bound
     * @return random double
     */
    public static double randomDouble(double min, double max) {
        if (min > max) {
            double tmp = min;
            min = max;
            max = tmp;
        }
        return min + (RANDOM.nextDouble() * (max - min));
    }

    /**
     * Returns a random boolean.
     * <p>Example usage:
     * <blockquote><pre>
     * boolean b = MathUtil.randomBoolean();
     * </pre></blockquote>
     *
     * @return random boolean
     */
    public static boolean randomBoolean() {
        return RANDOM.nextBoolean();
    }

    /**
     * Returns a random boolean, true with probability {@code chance}.
     * <p>Example usage:
     * <blockquote><pre>
     * boolean b = MathUtil.randomBoolean(0.25f); // 25% chance
     * </pre></blockquote>
     *
     * @param chance probability in [0..1]
     * @return random boolean
     */
    public static boolean randomBoolean(float chance) {
        return (randomFloat() < chance);
    }

    /**
     * Returns a random byte in [0..max].
     * <p>Example usage:
     * <blockquote><pre>
     * byte b = MathUtil.randomByte(10); // 0..10
     * </pre></blockquote>
     *
     * @param max upper bound
     * @return random byte
     */
    public static byte randomByte(int max) {
        return (byte) randomInt(0, max);
    }

    /**
     * Returns a random angle in radians in [0..2PI).
     * <p>Example usage:
     * <blockquote><pre>
     * double angle = MathUtil.randomAngle();
     * </pre></blockquote>
     *
     * @return random angle in radians
     */
    public static double randomAngle() {
        return RANDOM.nextDouble() * (Math.PI * 2);
    }

    // -------------------------------------------------------------------------------------
    // Inversions & interpolation
    // -------------------------------------------------------------------------------------

    /**
     * Returns true if one value is negative and the other is positive.
     * <p>Example usage:
     * <blockquote><pre>
     * boolean opp = MathUtil.oppositeSigns(5, -3); // true
     * </pre></blockquote>
     *
     * @param a first value
     * @param b second value
     * @return true if sign(a) != sign(b)
     */
    public static boolean oppositeSigns(double a, double b) {
        return (a > 0 && b < 0) || (a < 0 && b > 0);
    }

    /**
     * Returns {@code value} or {@code -value}, depending on {@code negative}.
     * <p>Example usage:
     * <blockquote><pre>
     * double inv = MathUtil.invert(5, true); // -5
     * </pre></blockquote>
     *
     * @param value    the value
     * @param negative whether to invert
     * @return value or -value
     */
    public static double invert(double value, boolean negative) {
        return negative ? -value : value;
    }

    /**
     * Performs a linear interpolation between oldVal and newVal by factor.
     * Factor is typically in [0..1], but not strictly enforced.
     * <p>Example usage:
     * <blockquote><pre>
     * double val = MathUtil.lerp(0.0, 10.0, 0.75); // 7.5
     * </pre></blockquote>
     *
     * @param oldVal the old value
     * @param newVal the new value
     * @param factor fraction in [0..1]
     * @return interpolated value
     */
    public static double lerp(double oldVal, double newVal, double factor) {
        if (Double.isNaN(factor) || factor > 1) {
            return newVal;
        } else if (factor < 0) {
            return oldVal;
        } else {
            return oldVal + (newVal - oldVal) * factor;
        }
    }

    /**
     * Performs linear interpolation between two Vectors.
     * <p>Example usage:
     * <blockquote><pre>
     * Vector v1 = new Vector(0,0,0);
     * Vector v2 = new Vector(10,10,10);
     * Vector lerped = MathUtil.lerp(v1, v2, 0.5); // (5,5,5)
     * </pre></blockquote>
     *
     * @param from   start vector
     * @param to     end vector
     * @param factor fraction in [0..1]
     * @return interpolated vector
     */
    public static Vector lerp(Vector from, Vector to, double factor) {
        double x = lerp(from.getX(), to.getX(), factor);
        double y = lerp(from.getY(), to.getY(), factor);
        double z = lerp(from.getZ(), to.getZ(), factor);
        return new Vector(x, y, z);
    }

    /**
     * Performs linear interpolation between two Locations (worlds must match).
     * <p>Example usage:
     * <blockquote><pre>
     * Location loc1 = ...;
     * Location loc2 = ...;
     * Location lerped = MathUtil.lerp(loc1, loc2, 0.5);
     * </pre></blockquote>
     *
     * @param from   start location
     * @param to     end location
     * @param factor fraction in [0..1]
     * @return new interpolated location
     */
    public static Location lerp(Location from, Location to, double factor) {
        if (from.getWorld() != to.getWorld()) {
            // fallback
            return from.clone();
        }
        double x = lerp(from.getX(), to.getX(), factor);
        double y = lerp(from.getY(), to.getY(), factor);
        double z = lerp(from.getZ(), to.getZ(), factor);
        float yaw = (float) lerp(from.getYaw(), to.getYaw(), factor);
        float pitch = (float) lerp(from.getPitch(), to.getPitch(), factor);
        return new Location(from.getWorld(), x, y, z, yaw, pitch);
    }

    // -------------------------------------------------------------------------------------
    // Checking numeric types
    // -------------------------------------------------------------------------------------

    /**
     * Checks if an object is an integer (via Integer.parseInt).
     * <p>Example usage:
     * <blockquote><pre>
     * boolean isInt = MathUtil.isInteger("123"); // true
     * </pre></blockquote>
     *
     * @param obj the object to check
     * @return true if integer, false otherwise
     */
    public static boolean isInteger(Object obj) {
        try {
            Integer.parseInt(obj.toString());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Checks if an object is a double (via Double.parseDouble).
     * <p>Example usage:
     * <blockquote><pre>
     * boolean isDbl = MathUtil.isDouble("3.14"); // true
     * </pre></blockquote>
     *
     * @param obj the object to check
     * @return true if double, false otherwise
     */
    public static boolean isDouble(Object obj) {
        try {
            Double.parseDouble(obj.toString());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    // -------------------------------------------------------------------------------------
    // Angles, pitch, yaw, rotation
    // -------------------------------------------------------------------------------------

    /**
     * Gets the inverse tangent of the value (in degrees).
     * <p>Example usage:
     * <blockquote><pre>
     * float angle = MathUtil.atanDeg(1.0); // ~45 degrees
     * </pre></blockquote>
     *
     * @param value the value
     * @return angle in degrees
     */
    public static float atanDeg(double value) {
        return (float) (internalAtan(value) * RAD_TO_DEG);
    }

    /**
     * Gets the inverse tangent2 of (y,x) (in degrees).
     * <p>Example usage:
     * <blockquote><pre>
     * float angle = MathUtil.atan2Deg(1, 1); // ~45 degrees
     * </pre></blockquote>
     *
     * @param y y-value
     * @param x x-value
     * @return angle in degrees
     */
    public static float atan2Deg(double y, double x) {
        return (float) (internalAtan2(y, x) * RAD_TO_DEG);
    }

    /**
     * Rotates a vector around yaw and pitch (in degrees).
     * <p>Example usage:
     * <blockquote><pre>
     * Vector rotated = MathUtil.rotateVectorDeg(90, 0, new Vector(1,0,0));
     * </pre></blockquote>
     *
     * @param yaw   yaw in degrees
     * @param pitch pitch in degrees
     * @param vec   vector to rotate
     * @return new rotated vector
     */
    public static Vector rotateVectorDeg(float yaw, float pitch, Vector vec) {
        return rotateVectorDeg(yaw, pitch, vec.getX(), vec.getY(), vec.getZ());
    }

    /**
     * Rotates a 3D-vector by yaw and pitch (both in degrees).
     * <p>Example usage:
     * <blockquote><pre>
     * Vector rotated = MathUtil.rotateVectorDeg(90, 30, 1, 0, 0);
     * </pre></blockquote>
     *
     * @param yaw   yaw in degrees
     * @param pitch pitch in degrees
     * @param x     x
     * @param y     y
     * @param z     z
     * @return new rotated vector
     */
    public static Vector rotateVectorDeg(float yaw, float pitch, double x, double y, double z) {
        float ry = yaw * DEG_TO_RAD;
        double siny = Math.sin(ry);
        double cosy = Math.cos(ry);

        float rp = pitch * DEG_TO_RAD;
        double sinp = Math.sin(rp);
        double cosp = Math.cos(rp);

        Vector v = new Vector();
        v.setX((x * siny) - (y * cosy * sinp) - (z * cosy * cosp));
        v.setY((y * cosp) - (z * sinp));
        v.setZ(-(x * cosy) - (y * siny * sinp) - (z * siny * cosp));
        return v;
    }

    /**
     * Returns the pitch (in degrees) of a vector, measuring up/down angle.
     * <p>Example usage:
     * <blockquote><pre>
     * float pitch = MathUtil.getPitchDeg(new Vector(0,1,0)); // ~-90
     * </pre></blockquote>
     *
     * @param vec the vector
     * @return pitch in degrees
     */
    public static float getPitchDeg(Vector vec) {
        double x = vec.getX();
        double y = vec.getY();
        double z = vec.getZ();
        double xz = Math.sqrt(x * x + z * z);
        double pitch = Math.toDegrees(Math.atan(xz / y));
        if (y <= 0.0D) pitch += 90.0D;
        else pitch -= 90.0D;
        return (float) pitch;
    }

    /**
     * Returns the yaw (in degrees) of a vector, measuring left/right angle.
     * <p>Example usage:
     * <blockquote><pre>
     * float yaw = MathUtil.getYawDeg(new Vector(1,0,0)); // ~-0
     * </pre></blockquote>
     *
     * @param vec the vector
     * @return yaw in degrees
     */
    public static float getYawDeg(Vector vec) {
        double x = vec.getX();
        double z = vec.getZ();
        double yaw = Math.toDegrees(Math.atan(-x / z));
        if (z < 0.0D) yaw += 180.0D;
        return (float) yaw;
    }

    /**
     * Gets a direction vector from yaw/pitch (in degrees).
     * <p>Example usage:
     * <blockquote><pre>
     * Vector dir = MathUtil.directionDeg(90, 0); // points along negative X
     * </pre></blockquote>
     *
     * @param yaw   yaw in degrees
     * @param pitch pitch in degrees
     * @return direction vector
     */
    public static Vector directionDeg(float yaw, float pitch) {
        double ry = yaw * DEG_TO_RAD;
        double rp = pitch * DEG_TO_RAD;
        Vector v = new Vector();
        v.setY(-Math.sin(rp));
        double h = Math.cos(rp);
        v.setX(-h * Math.sin(ry));
        v.setZ(h * Math.cos(ry));
        return v;
    }

    // -------------------------------------------------------------------------------------
    // Sine/cosine approximations (fast tables)
    // -------------------------------------------------------------------------------------

    /**
     * A fast sine approximation, based on a precomputed table (input is radians).
     * <p>Example usage:
     * <blockquote><pre>
     * float s = MathUtil.sin(3.14f);
     * </pre></blockquote>
     *
     * @param radians angle in radians
     * @return sine of the angle
     */
    public static float sin(float radians) {
        return Sin.table[(int) (radians * RAD_TO_INDEX) & SIN_MASK];
    }

    /**
     * A fast cosine approximation, using the sine table (input is radians).
     * <p>Example usage:
     * <blockquote><pre>
     * float c = MathUtil.cos(3.14f);
     * </pre></blockquote>
     *
     * @param radians angle in radians
     * @return cosine of the angle
     */
    public static float cos(float radians) {
        return Sin.table[(int) ((radians + 1.5707964F) * RAD_TO_INDEX) & SIN_MASK];
    }

    /**
     * A fast sine approximation, for input in degrees.
     * <p>Example usage:
     * <blockquote><pre>
     * float s = MathUtil.sinDeg(180f); // 0
     * </pre></blockquote>
     *
     * @param degrees angle in degrees
     * @return sine of the angle
     */
    public static float sinDeg(float degrees) {
        return Sin.table[(int) (degrees * DEG_TO_INDEX) & SIN_MASK];
    }

    /**
     * A fast cosine approximation, for input in degrees.
     * <p>Example usage:
     * <blockquote><pre>
     * float c = MathUtil.cosDeg(180f); // -1
     * </pre></blockquote>
     *
     * @param degrees angle in degrees
     * @return cosine of the angle
     */
    public static float cosDeg(float degrees) {
        return Sin.table[(int) ((degrees + 90.0F) * DEG_TO_INDEX) & SIN_MASK];
    }

    // -------------------------------------------------------------------------------------
    // Random Vector & others
    // -------------------------------------------------------------------------------------

    /**
     * Returns a random {@code Vector} of unit length, pointing in a random 3D direction.
     * <p>Example usage:
     * <blockquote><pre>
     * Vector randVec = MathUtil.randomUnitVector3D();
     * </pre></blockquote>
     * @return random unit vector
     */
    public static Vector randomUnitVector3D() {
        double x = RANDOM.nextDouble() * 2.0 - 1.0;
        double y = RANDOM.nextDouble() * 2.0 - 1.0;
        double z = RANDOM.nextDouble() * 2.0 - 1.0;
        return new Vector(x, y, z).normalize();
    }

    /**
     * Returns a random 2D circle vector on the XZ plane, length = 1, y=0.
     * <p>Example usage:
     * <blockquote><pre>
     * Vector circleVec = MathUtil.randomUnitVector2D();
     * </pre></blockquote>
     * @return random circle vector
     */
    public static Vector randomUnitVector2D() {
        double rnd = RANDOM.nextDouble() * 2.0 * Math.PI;
        return new Vector(Math.cos(rnd), 0.0D, Math.sin(rnd));
    }

    /**
     * Returns a random Material from the given array.
     * <p>Example usage:
     * <blockquote><pre>
     * Material mat = MathUtil.randomMaterial(new Material[]{Material.STONE, Material.DIRT});
     * </pre></blockquote>
     * @param materials array
     * @return random choice
     */
    public static Material randomMaterial(Material[] materials) {
        return materials[randomInt(0, materials.length - 1)];
    }

    // -------------------------------------------------------------------------------------
    // Converting between chunk coords
    // -------------------------------------------------------------------------------------

    /**
     * Converts a location coordinate (double) into a chunk coordinate.
     * <p>Example usage:
     * <blockquote><pre>
     * int chunk = MathUtil.toChunk(100.0);
     * </pre></blockquote>
     * @param loc location
     * @return chunk coordinate
     */
    public static int toChunk(double loc) {
        return floor(loc / CHUNK_VALUES);
    }

    /**
     * Converts a location coordinate (int) into a chunk coordinate.
     * <p>Example usage:
     * <blockquote><pre>
     * int chunk = MathUtil.toChunk(100);
     * </pre></blockquote>
     * @param loc location
     * @return chunk coordinate
     */
    public static int toChunk(int loc) {
        return loc >> CHUNK_BITS;
    }

    // -------------------------------------------------------------------------------------
    // Apply velocity if not NPC
    // -------------------------------------------------------------------------------------

    /**
     * Applies a velocity to an entity if it does not have NPC metadata.
     * <p>Example usage:
     * <blockquote><pre>
     * MathUtil.applyVelocity(player, new Vector(0,1,0));
     * </pre></blockquote>
     *
     * @param ent the entity
     * @param velocity the velocity
     */
    public static void applyVelocity(Entity ent, Vector velocity) {
        if (!ent.hasMetadata("NPC")) {
            ent.setVelocity(velocity);
        }
    }

    // -------------------------------------------------------------------------------------
    // Movement transformations
    // -------------------------------------------------------------------------------------

    /**
     * Moves a Location in its own yaw/pitch direction by the specified offset.
     * <p>Example usage:
     * <blockquote><pre>
     * Location newLoc = MathUtil.moveLocationDeg(player.getLocation(), 1, 0, 0);
     * </pre></blockquote>
     *
     * @param loc the location
     * @param dx  x offset
     * @param dy  y offset
     * @param dz  z offset
     * @return new location
     */
    public static Location moveLocationDeg(Location loc, double dx, double dy, double dz) {
        Vector off = rotateVectorDeg(loc.getYaw(), loc.getPitch(), dx, dy, dz);
        double x = loc.getX() + off.getX();
        double y = loc.getY() + off.getY();
        double z = loc.getZ() + off.getZ();
        return new Location(loc.getWorld(), x, y, z, loc.getYaw(), loc.getPitch());
    }

    /**
     * Moves a Location in its own yaw/pitch direction by the given vector offset.
     * <p>Example usage:
     * <blockquote><pre>
     * Location newLoc = MathUtil.moveLocationDeg(player.getLocation(), new Vector(1,0,0));
     * </pre></blockquote>
     *
     * @param loc the location
     * @param offset offset vector
     * @return new location
     */
    public static Location moveLocationDeg(Location loc, Vector offset) {
        return moveLocationDeg(loc, offset.getX(), offset.getY(), offset.getZ());
    }

    // -------------------------------------------------------------------------------------
    // Trajectory computations
    // -------------------------------------------------------------------------------------

    /**
     * Returns a normalized direction vector (3D) from one Vector to another.
     * <p>Example:
     * <blockquote><pre>
     * Vector dir = MathUtil.trajectory3D(vecA, vecB);
     * </pre></blockquote>
     * @param from source
     * @param to   target
     * @return normalized direction
     */
    public static Vector trajectory3D(Vector from, Vector to) {
        return to.clone().subtract(from).normalize();
    }

    /**
     * Returns a normalized direction vector (2D, ignoring Y) from one Vector to another.
     * <p>Example:
     * <blockquote><pre>
     * Vector dir2D = MathUtil.trajectory2D(vecA, vecB);
     * </pre></blockquote>
     * @param from source
     * @param to   target
     * @return normalized direction in XZ
     */
    public static Vector trajectory2D(Vector from, Vector to) {
        Vector diff = to.clone().subtract(from);
        diff.setY(0);
        return diff.normalize();
    }

    /**
     * Returns the yaw angle (in degrees) from one location to another (horizontal only).
     * <p>Example usage:
     * <blockquote><pre>
     * float yaw = MathUtil.getYawToDeg(locA, locB);
     * </pre></blockquote>
     * @param from source
     * @param to target
     * @return yaw angle in degrees
     */
    public static float getYawToDeg(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        return atan2Deg(dz, dx) - 180f;
    }

    // -------------------------------------------------------------------------------------
    // Private: approximate atan using polynomial expansions
    // -------------------------------------------------------------------------------------

    private static double internalAtan(double arg) {
        if (arg > 0.0D) {
            return msatan(arg);
        } else {
            return -msatan(-arg);
        }
    }

    private static double internalAtan2(double arg1, double arg2) {
        if (arg1 + arg2 == arg1) {
            return arg1 >= 0.0D ? (Math.PI / 2) : -(Math.PI / 2);
        } else {
            arg1 = internalAtan(arg1 / arg2);
            if (arg2 < 0.0D) {
                return arg1 <= 0.0D ? arg1 + Math.PI : arg1 - Math.PI;
            }
            return arg1;
        }
    }

    private static double msatan(double arg) {
        if (arg < 0.41421356237309503D) {
            return mxatan(arg);
        } else if (arg > 2.414213562373095D) {
            return (Math.PI / 2) - mxatan(1.0D / arg);
        } else {
            return (Math.PI / 4) + mxatan((arg - 1.0D) / (arg + 1.0D));
        }
    }

    private static double mxatan(double arg) {
        // Polynomial expansion constants from original code
        double argsq = arg * arg;
        double p = (((16.15364129822302D * argsq + 268.42548195503974D) * argsq
                + 1153.029351540485D) * argsq + 1780.406316433197D) * argsq + 896.7859740366387D;

        double q = ((((argsq + 58.95697050844462D) * argsq + 536.2653740312153D) * argsq
                + 1666.7838148816338D) * argsq + 2079.33497444541D) * argsq + 896.7859740366387D;

        return (p / q) * arg;
    }

    // -------------------------------------------------------------------------------------
    // Internal table for fast sin/cos
    // -------------------------------------------------------------------------------------

    private static class Sin {
        static final float[] table = new float[SIN_COUNT];
        static {
            for (int i = 0; i < SIN_COUNT; i++) {
                table[i] = (float) Math.sin((i + 0.5F) / SIN_COUNT * RAD_FULL);
            }
            for (int i = 0; i < 360; i += 90) {
                table[(int) (i * DEG_TO_INDEX) & SIN_MASK] = (float) Math.sin(i * DEG_TO_RAD);
            }
        }
    }
}
