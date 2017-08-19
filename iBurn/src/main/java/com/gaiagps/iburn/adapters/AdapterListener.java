package com.gaiagps.iburn.adapters;

import com.gaiagps.iburn.database.PlayaItem;

/**
 * Created by davidbrodsky on 6/15/15.
 */
public interface AdapterListener {
    void onItemSelected(PlayaItem item);
    void onItemFavoriteButtonSelected(PlayaItem item);

}
