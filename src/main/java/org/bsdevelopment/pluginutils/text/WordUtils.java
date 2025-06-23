package org.bsdevelopment.pluginutils.text;

/**
 * A collection of utility methods for manipulating and formatting words.
 *
 * <p>This class provides methods to capitalize, uncapitalize, swap the case,
 * and extract initials from strings.
 *
 * <p><b>Examples:</b>
 * <pre>
 * // Capitalize first letter of each word (using default whitespace delimiters)
 * String result = WordUtils.capitalize("hello world");
 * // result: "Hello World"
 *
 * // Fully capitalize (convert rest of letters to lower case first)
 * String result2 = WordUtils.capitalizeFully("hELLO wORLD");
 * // result2: "Hello World"
 *
 * // Uncapitalize first letter of each word
 * String result3 = WordUtils.uncapitalize("Hello World");
 * // result3: "hello world"
 *
 * // Swap case of each character
 * String result4 = WordUtils.swapCase("Hello World");
 * // result4: "hELLO wORLD"
 *
 * // Get initials of each word
 * String result5 = WordUtils.initials("Hello World");
 * // result5: "HW"
 * </pre>
 */
public final class WordUtils {

    // Private constructor to prevent instantiation.
    private WordUtils() {
        throw new UnsupportedOperationException("WordUtils is a utility class and cannot be instantiated");
    }

    /**
     * Capitalizes the first letter of each word in the given string using default delimiters (whitespace).
     *
     * @param str
     *         the input string to capitalize; may be null
     *
     * @return a new string with each word capitalized, or the original string if null or empty.
     *
     * <p><b>Example:</b>
     * <pre>
     * String result = WordUtils.capitalize("hello world");
     * // result: "Hello World"
     * </pre>
     */
    public static String capitalize(String str) {
        return capitalize(str, null);
    }

    /**
     * Converts the input string to lower case and then capitalizes the first letter of each word
     * using default delimiters (whitespace).
     *
     * @param str
     *         the input string to convert and capitalize; may be null
     *
     * @return a new string fully capitalized (only first letters in upper case), or the original string if null or empty.
     *
     * <p><b>Example:</b>
     * <pre>
     * String result = WordUtils.capitalizeFully("hELLO wORLD");
     * // result: "Hello World"
     * </pre>
     */
    public static String capitalizeFully(String str) {
        return capitalizeFully(str, null);
    }

    /**
     * Uncapitalizes the first letter of each word in the given string using default delimiters (whitespace).
     *
     * @param str
     *         the input string to uncapitalize; may be null
     *
     * @return a new string with each word uncapitalized, or the original string if null or empty.
     *
     * <p><b>Example:</b>
     * <pre>
     * String result = WordUtils.uncapitalize("Hello World");
     * // result: "hello world"
     * </pre>
     */
    public static String uncapitalize(String str) {
        return uncapitalize(str, null);
    }

    /**
     * Swaps the case of each character in the given string.
     * <ul>
     *   <li>Uppercase characters are converted to lowercase.
     *   <li>Lowercase characters are converted to uppercase. If the character follows whitespace,
     *       it is converted to title case.
     * </ul>
     *
     * @param str
     *         the input string to swap case; may be null
     *
     * @return a new string with swapped case, or the original string if null or empty.
     *
     * <p><b>Example:</b>
     * <pre>
     * String result = WordUtils.swapCase("Hello World");
     * // result: "hELLO wORLD"
     * </pre>
     */
    public static String swapCase(String str) {
        if (str == null || str.isEmpty()) return str;

        char[] chars = str.toCharArray();

        StringBuilder sb = new StringBuilder(chars.length);
        boolean whitespace = true;

        for (char ch : chars) {
            char tmp;

            if (Character.isUpperCase(ch) || Character.isTitleCase(ch))
                tmp = Character.toLowerCase(ch);
            else if (Character.isLowerCase(ch))
                tmp = whitespace ? Character.toTitleCase(ch) : Character.toUpperCase(ch);
            else
                tmp = ch;

            sb.append(tmp);

            whitespace = Character.isWhitespace(ch);
        }

        return sb.toString();
    }

    /**
     * Extracts the initials (first character of each word) from the given string using default delimiters (whitespace).
     *
     * @param str
     *         the input string from which to extract initials; may be null
     *
     * @return a string containing the initials, or the original string if null or empty.
     *
     * <p><b>Example:</b>
     * <pre>
     * String result = WordUtils.initials("Hello World");
     * // result: "HW"
     * </pre>
     */
    public static String initials(String str) {
        return initials(str, null);
    }

    // --- PRIVATE METHODS --- //

    private static String capitalize(String str, char[] delimiters) {
        if (str == null || str.isEmpty()) return str;
        if (delimiters != null && delimiters.length == 0) return str;

        char[] chars = str.toCharArray();
        StringBuilder sb = new StringBuilder(chars.length);
        boolean capitalizeNext = true;

        for (char ch : chars) {
            if (isDelimiter(ch, delimiters)) {
                sb.append(ch);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toTitleCase(ch));
                capitalizeNext = false;
            } else {
                sb.append(ch);
            }
        }

        return sb.toString();
    }

    private static String capitalizeFully(String str, char[] delimiters) {
        if (str == null || str.isEmpty()) return str;
        if (delimiters != null && delimiters.length == 0) return str;

        return capitalize(str.toLowerCase(), delimiters);
    }

    private static String uncapitalize(String str, char[] delimiters) {
        if (str == null || str.isEmpty()) return str;
        if (delimiters != null && delimiters.length == 0) return str;

        char[] chars = str.toCharArray();
        StringBuilder sb = new StringBuilder(chars.length);
        boolean uncapitalizeNext = true;

        for (char ch : chars) {
            if (isDelimiter(ch, delimiters)) {
                sb.append(ch);
                uncapitalizeNext = true;
            } else if (uncapitalizeNext) {
                sb.append(Character.toLowerCase(ch));
                uncapitalizeNext = false;
            } else {
                sb.append(ch);
            }
        }

        return sb.toString();
    }

    private static String initials(String str, char[] delimiters) {
        if (str == null || str.isEmpty()) return str;
        if (delimiters != null && delimiters.length == 0) return "";

        char[] chars = str.toCharArray();
        StringBuilder sb = new StringBuilder(chars.length / 2 + 1);
        boolean lastWasGap = true;

        for (char ch : chars) {
            if (isDelimiter(ch, delimiters))
                lastWasGap = true;
            else if (lastWasGap) {
                sb.append(ch);
                lastWasGap = false;
            }
        }

        return sb.toString();
    }

    private static boolean isDelimiter(char ch, char[] delimiters) {
        if (delimiters == null) return Character.isWhitespace(ch);

        for (char d : delimiters) {
            if (ch == d) return true;
        }

        return false;
    }
}
