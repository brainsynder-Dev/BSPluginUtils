package org.bsdevelopment.pluginutils.text;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Provides advanced string manipulation utilities.
 *
 * <p>This class includes methods for padding, replacing, and extracting substrings,
 * as well as reversing and scrambling text.
 *
 * <p><b>Examples:</b>
 * <pre>
 * // Get a padded string with centered text
 * String padded = AdvString.getPaddedString("Hello", '*', 20, AdvString.AlignText.CENTER);
 *
 * // Replace the last occurrence of a substring
 * String replaced = AdvString.replaceLast("cat", "dog", "The cat chased the cat");
 *
 * // Check if a string contains a character exactly 3 times
 * boolean result = AdvString.contains("hello", 'l', 2);
 *
 * // Extract text after a specified substring
 * String after = AdvString.after("Hello", "Hello World");
 *
 * // Reverse a string
 * String reversed = AdvString.reverse("Hello");
 *
 * // Scramble a string
 * String scrambled = AdvString.scramble("Hello World");
 * </pre>
 */
public class AdvString {

    /**
     * Returns a padded string with the original text aligned according to the given parameter.
     *
     * <p>This method calculates the required padding on both sides of the text (or unevenly,
     * if aligned left or right) based on the maxPadding parameter, and then returns the padded string.
     *
     * <p><b>Example:</b>
     * <pre>
     * String padded = AdvString.getPaddedString("Hello", '*', 20, AdvString.AlignText.CENTER);
     * </pre>
     *
     * @param text
     *         the text to pad; must not be null
     * @param paddingChar
     *         the character used for padding
     * @param maxPadding
     *         the maximum total width for padding
     * @param alignText
     *         the alignment (LEFT, RIGHT, CENTER)
     *
     * @return the padded string
     * @throws NullPointerException
     *         if text is null
     */
    public static String getPaddedString(String text, char paddingChar, int maxPadding, AdvString.AlignText alignText) {
        if (text == null)
            throw new NullPointerException("Can not add padding in null String!");

        int length = text.length();
        int padding = (maxPadding - length) / 2; // decide left and right padding

        if (padding <= 0)
            return text; // return actual String if padding is less than or equal to 0

        String empty = "", hash = "#"; // hash is used as a placeholder

        // extra character in case of String with even length
        int extra = (length % 2 == 0) ? 1 : 0;

        String leftPadding = "%" + padding + "s";
        String rightPadding = "%" + (padding - extra) + "s";

        // Will align the text to the selected side
        switch (alignText) {
            case LEFT:
                leftPadding = "%s";
                rightPadding = "%" + (padding + (padding - extra)) + "s";
                break;
            case RIGHT:
                rightPadding = "%s";
                leftPadding = "%" + (padding + (padding - extra)) + "s";
                break;
        }

        String strFormat = leftPadding + "%s" + rightPadding;
        String formattedString = String.format(strFormat, empty, hash, empty);

        // Replace space with paddingChar and hash with text
        String paddedString = formattedString.replace(' ', paddingChar).replace(hash, text);
        return paddedString;
    }

    /**
     * Replaces the last occurrence of the target substring with the replacement in the haystack.
     *
     * <p><b>Example:</b>
     * <pre>
     * String result = AdvString.replaceLast("cat", "dog", "The cat chased the cat");
     * </pre>
     *
     * @param target
     *         the substring to replace
     * @param replacement
     *         the replacement string
     * @param haystack
     *         the original text
     *
     * @return the text after replacement, or the original text if target is not found
     */
    public static String replaceLast(String target, String replacement, String haystack) {
        int pos = haystack.lastIndexOf(target);

        if (pos > -1)
            return haystack.substring(0, pos) + replacement + haystack.substring(pos + target.length());
        else
            return haystack;
    }

    /**
     * Checks if the given string contains the specified character exactly count times.
     *
     * <p>If count is set to -1, it uses the default {@code String.contains} method.
     *
     * <p><b>Example:</b>
     * <pre>
     * boolean result = AdvString.contains("hello", 'l', 2);
     * </pre>
     *
     * @param string
     *         the text being checked
     * @param character
     *         the character to check for
     * @param count
     *         the required occurrence count; -1 for default check
     *
     * @return true if the character appears exactly count times, false otherwise
     */
    public static boolean contains(String string, char character, int count) {
        if (count == -1)
            return string.contains(String.valueOf(character));

        int i = 0;
        for (char c : string.toCharArray()) {
            if (c == character)
                i++;
        }

        return (i == count);
    }

    /**
     * Retrieves the text that comes after the first occurrence of the needle in the haystack.
     *
     * <p><b>Example:</b>
     * <pre>
     * String result = AdvString.after("Hello", "Hello World");
     * // result: " World"
     * </pre>
     *
     * @param needle
     *         the substring to search for
     * @param haystack
     *         the text being scanned
     *
     * @return the substring after the needle
     */
    public static String after(String needle, String haystack) {
        return haystack.substring(haystack.indexOf(needle) + needle.length());
    }

    /**
     * Retrieves the text that comes after the last occurrence of the needle in the haystack.
     *
     * <p><b>Example:</b>
     * <pre>
     * String result = AdvString.afterLast("World", "Hello World, World!");
     * // result: "!"
     * </pre>
     *
     * @param needle
     *         the substring to search for
     * @param haystack
     *         the text being scanned
     *
     * @return the substring after the last occurrence of the needle
     */
    public static String afterLast(String needle, String haystack) {
        return haystack.substring(reversePos(needle, haystack) + needle.length());
    }

    /**
     * Retrieves the text that comes before the first occurrence of the needle in the haystack.
     *
     * <p><b>Example:</b>
     * <pre>
     * String result = AdvString.before("World", "Hello World");
     * // result: "Hello "
     * </pre>
     *
     * @param needle
     *         the substring to search for
     * @param haystack
     *         the text being scanned
     *
     * @return the substring before the needle
     */
    public static String before(String needle, String haystack) {
        return haystack.substring(0, haystack.indexOf(needle));
    }

    /**
     * Retrieves the text that comes before the last occurrence of the needle in the haystack.
     *
     * <p><b>Example:</b>
     * <pre>
     * String result = AdvString.beforeLast("Hello", "Hello World, Hello");
     * // result: "Hello World, "
     * </pre>
     *
     * @param needle
     *         the substring to search for
     * @param haystack
     *         the text being scanned
     *
     * @return the substring before the last occurrence of the needle
     */
    public static String beforeLast(String needle, String haystack) {
        return haystack.substring(0, reversePos(needle, haystack));
    }

    /**
     * Retrieves the text between the first occurrence of 'first' and 'last' in the haystack.
     *
     * <p><b>Example:</b>
     * <pre>
     * String result = AdvString.between("[", "]", "Value [42] is here");
     * // result: "42"
     * </pre>
     *
     * @param first
     *         the starting delimiter
     * @param last
     *         the ending delimiter
     * @param haystack
     *         the text to scan
     *
     * @return the text between the delimiters
     */
    public static String between(String first, String last, String haystack) {
        return before(last, after(first, haystack));
    }

    /**
     * Retrieves the text between the last occurrence of 'first' and 'last' in the haystack.
     *
     * <p><b>Example:</b>
     * <pre>
     * String result = AdvString.betweenLast("<", ">", "Value <old> and <new>");
     * // result: "new"
     * </pre>
     *
     * @param first
     *         the starting delimiter
     * @param last
     *         the ending delimiter
     * @param haystack
     *         the text to scan
     *
     * @return the text between the last occurrences of the delimiters
     */
    public static String betweenLast(String first, String last, String haystack) {
        return afterLast(first, beforeLast(last, haystack));
    }

    /**
     * Finds the position of the needle in the haystack, searching from the end.
     *
     * <p><b>Example:</b>
     * <pre>
     * int pos = AdvString.reversePos("World", "Hello World");
     * </pre>
     *
     * @param needle
     *         the substring to search for
     * @param haystack
     *         the text being scanned
     *
     * @return the position of the needle when searching from the end
     */
    public static int reversePos(String needle, String haystack) {
        int pos = reverse(haystack).indexOf(reverse(needle));
        return haystack.length() - pos - needle.length();
    }

    /**
     * Reverses the input text.
     *
     * <p><b>Example:</b>
     * <pre>
     * String reversed = AdvString.reverse("Hello");
     * // reversed: "olleH"
     * </pre>
     *
     * @param input
     *         the text to reverse
     *
     * @return the reversed text
     */
    public static String reverse(String input) {
        var chars = input.toCharArray();

        var characters = new ArrayList<Character>();
        for (char c : chars)
            characters.add(c);

        Collections.reverse(characters);

        var iterator = characters.listIterator();
        var builder = new StringBuilder();

        while (iterator.hasNext())
            builder.append(iterator.next());

        return builder.toString();
    }

    /**
     * Randomly scrambles the input text.
     *
     * <p><b>Example:</b>
     * <pre>
     * String scrambled = AdvString.scramble("Hello World");
     * </pre>
     *
     * @param input
     *         the text to scramble
     *
     * @return the scrambled text
     */
    public static String scramble(String input) {
        var out = new StringBuilder();

        for (String part : input.split(" ")) {
            var characters = new ArrayList<Character>();

            for (char c : part.toCharArray())
                characters.add(c);

            var output = new StringBuilder(part.length());

            while (characters.size() != 0) {
                int rndm = (int) (Math.random() * characters.size());
                output.append(characters.remove(rndm));
            }

            out.append(output).append(' ');
        }

        return out.toString().trim();
    }

    public enum AlignText {
        LEFT, RIGHT, CENTER
    }
}
