package com.gaiagps.iburn;

/**
 * Represents an item that can respond to Search queries
 * Created by davidbrodsky on 8/1/14.
 */
public interface Searchable {

    /**
     * The object should adjust itself based on the given query. If query is of 0 length
     * or null, the implementor should treat the query as having been cancelled.
     */
    void onSearchQueryRequested(String query);
}
