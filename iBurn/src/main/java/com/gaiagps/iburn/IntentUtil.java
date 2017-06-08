package com.gaiagps.iburn;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.gaiagps.iburn.activity.PlayaItemViewActivity;
import com.gaiagps.iburn.database.PlayaItem;

/**
 * Created by dbro on 8/29/16.
 */
public class IntentUtil {

    public static void viewItemDetail(@NonNull Activity host, PlayaItem item) {
        // Launch detail activity?
//        if (type == Constants.PlayaItemType.POI) {
//            Toast.makeText(host.getApplicationContext(), "MAYBE NEXT YEAR\n       ¯\\_(ツ)_/¯", Toast.LENGTH_SHORT).show();
//        } else {
        Intent i = new Intent(host, PlayaItemViewActivity.class);
        i.putExtra(PlayaItemViewActivity.EXTRA_PLAYA_ITEM, item);
        host.startActivity(i);
//        }
    }
}
