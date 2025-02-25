package org.bsdevelopment.pluginutils.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A paginated list that splits its contents into pages of a fixed size limit.
 *
 * <p>This class extends {@link ArrayList} but provides additional methods
 * to manage and retrieve items by page index. Each page has a maximum number of items
 * equal to the provided {@code contentLimit}.
 *
 * <p><b>Example Usage:</b>
 * <pre>
 * // Create a pager that can hold 5 items per page
 * ListPager&lt;String&gt; pager = new ListPager&lt;&gt;(5, "A", "B", "C", "D", "E", "F");
 *
 * // Get the total number of pages
 * int pages = pager.totalPages(); // 2 in this example
 *
 * // Check if a page index is valid
 * boolean page2Exists = pager.exists(2); // true if page 2 is valid
 *
 * // Fetch the items on page 1
 * List&lt;String&gt; page1 = pager.getPage(1); // returns ["A", "B", "C", "D", "E"]
 * </pre>
 *
 * @param <T> the type of elements in this list
 */
public class ListPager<T> extends ArrayList<T> {

    /**
     * The maximum number of items allowed on one page.
     */
    private final int contentLimit;

    /**
     * Constructs a pager with the specified page size limit, but no initial contents.
     *
     * @param contentLimit number of items per page
     */
    public ListPager(int contentLimit) {
        this(contentLimit, new ArrayList<>());
    }

    /**
     * Constructs a pager with the specified page size limit and initial elements.
     *
     * @param contentLimit number of items per page
     * @param objects      varargs array of items to be added
     */
    public ListPager(int contentLimit, T... objects) {
        this(contentLimit, Arrays.asList(objects));
    }

    /**
     * Constructs a pager with the specified page size limit and initial elements from a collection.
     *
     * @param contentLimit number of items per page
     * @param objects      the collection of items to be added initially
     */
    public ListPager(int contentLimit, List<T> objects) {
        this.contentLimit = contentLimit;
        addAll(objects);
    }

    /**
     * Retrieves the maximum number of items allowed in each page.
     *
     * @return the page size limit
     */
    public int getContentLimit() {
        return contentLimit;
    }

    /**
     * Determines the total number of pages required to hold all elements,
     * given the current list size and {@link #contentLimit}.
     *
     * @return the total number of pages
     */
    public int totalPages() {
        return (int) Math.ceil((double) size() / contentLimit);
    }

    /**
     * Checks if a page with the given index exists in the pager. Page numbering starts at 1.
     *
     * @param page the 1-based page index
     * @return true if the page exists, otherwise false
     */
    public boolean exists(int page) {
        var zeroBased = page - 1;
        return zeroBased >= 0 && zeroBased < totalPages();
    }

    /**
     * Retrieves the elements of the specified 1-based page index.
     *
     * @param page the 1-based page index
     * @return a list of items on that page
     * @throws IndexOutOfBoundsException if the page is out of range
     */
    public List<T> getPage(int page) {
        var zeroBased = page - 1;
        if (zeroBased < 0 || zeroBased >= totalPages()) {
            throw new IndexOutOfBoundsException("Index: " + zeroBased + ", Size: " + totalPages());
        }

        var objects = new ArrayList<T>();
        var min = zeroBased * contentLimit;
        var max = min + contentLimit;
        if (max > size()) max = size();

        for (var i = min; i < max; i++) {
            objects.add(get(i));
        }
        return objects;
    }
}
