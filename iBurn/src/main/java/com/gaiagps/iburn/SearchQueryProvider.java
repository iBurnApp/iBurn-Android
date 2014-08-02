package com.gaiagps.iburn;

/**
 * Represents an item that can provide an outstanding
 * query to a newly created Searchable object.
 * Created by davidbrodsky on 8/2/14.
 */
public interface SearchQueryProvider {

    public String getCurrentQuery();
}
