package com.gaiagps.iburn.adapters;

import com.gaiagps.iburn.Constants;

/**
 * Created by davidbrodsky on 6/15/15.
 */
public interface AdapterListener {
    void onItemSelected(int modelId, Constants.PlayaItemType type);
    void onItemFavoriteButtonSelected(int modelId, Constants.PlayaItemType type);

}
