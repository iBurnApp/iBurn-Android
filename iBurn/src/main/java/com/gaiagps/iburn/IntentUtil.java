package com.gaiagps.iburn;

import static com.gaiagps.iburn.activity.PlayaItemViewActivity.EXTRA_PLAYA_ITEM_ART;
import static com.gaiagps.iburn.activity.PlayaItemViewActivity.EXTRA_PLAYA_ITEM_CAMP;
import static com.gaiagps.iburn.activity.PlayaItemViewActivity.EXTRA_PLAYA_ITEM_EVENT;

import android.app.Activity;
import android.content.Intent;
import androidx.annotation.NonNull;

import com.gaiagps.iburn.activity.PlayaItemViewActivity;
import com.gaiagps.iburn.database.Art;
import com.gaiagps.iburn.database.Camp;
import com.gaiagps.iburn.database.Event;
import com.gaiagps.iburn.database.PlayaItem;

/**
 * Created by dbro on 8/29/16.
 */
public class IntentUtil {

    public static void viewItemDetail(@NonNull Activity host, PlayaItem item) {
        Intent i = getViewItemDetailIntent(host, item);
        host.startActivity(i);
    }

    public static Intent getViewItemDetailIntent(@NonNull Activity host, PlayaItem item) {
        Intent i = new Intent(host, PlayaItemViewActivity.class);
        int id = item.id;
        i.putExtra(PlayaItemViewActivity.EXTRA_PLAYA_ITEM_ID, id);
        if (item instanceof Camp) {
            i.putExtra(PlayaItemViewActivity.EXTRA_PLAYA_ITEM_TYPE, EXTRA_PLAYA_ITEM_CAMP);
        } else if (item instanceof Art) {
            i.putExtra(PlayaItemViewActivity.EXTRA_PLAYA_ITEM_TYPE, EXTRA_PLAYA_ITEM_ART);
        } else if (item instanceof Event) {
            i.putExtra(PlayaItemViewActivity.EXTRA_PLAYA_ITEM_TYPE, EXTRA_PLAYA_ITEM_EVENT);
        }
        return i;
    }
}
