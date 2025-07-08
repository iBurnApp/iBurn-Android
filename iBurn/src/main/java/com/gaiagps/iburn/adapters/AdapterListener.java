package com.gaiagps.iburn.adapters;

import com.gaiagps.iburn.database.PlayaItem;
import com.gaiagps.iburn.database.PlayaItemWithUserData;

/**
 * Created by davidbrodsky on 6/15/15.
 */
public interface AdapterListener {
    void onItemSelected(PlayaItemWithUserData item);
    void onItemFavoriteButtonSelected(PlayaItem item);

}
