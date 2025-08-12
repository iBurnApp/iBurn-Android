package com.gaiagps.iburn

import android.app.Activity
import android.content.Intent
import com.gaiagps.iburn.activity.PlayaItemViewActivity
import com.gaiagps.iburn.database.Art
import com.gaiagps.iburn.database.Camp
import com.gaiagps.iburn.database.Event
import com.gaiagps.iburn.database.PlayaItem
import com.gaiagps.iburn.database.PlayaItemWithUserData

/**
 * Created by dbro on 8/29/16.
 */
object IntentUtil {
    @JvmStatic
    fun viewItemDetail(host: Activity, item: PlayaItem) {
        val i = getViewItemDetailIntent(host, item)
        host.startActivity(i)
    }

    fun getViewItemDetailIntent(host: Activity, item: PlayaItem): Intent {
        val i = Intent(host, PlayaItemViewActivity::class.java)
        val id = item.id
        i.putExtra(PlayaItemViewActivity.EXTRA_PLAYA_ITEM_ID, id)
        if (item is Camp) {
            i.putExtra(
                PlayaItemViewActivity.EXTRA_PLAYA_ITEM_TYPE,
                PlayaItemViewActivity.EXTRA_PLAYA_ITEM_CAMP
            )
        } else if (item is Art) {
            i.putExtra(
                PlayaItemViewActivity.EXTRA_PLAYA_ITEM_TYPE,
                PlayaItemViewActivity.EXTRA_PLAYA_ITEM_ART
            )
        } else if (item is Event) {
            i.putExtra(
                PlayaItemViewActivity.EXTRA_PLAYA_ITEM_TYPE,
                PlayaItemViewActivity.EXTRA_PLAYA_ITEM_EVENT
            )
        }
        return i
    }

//    fun loadPlayaItemFromIntent(i: Intent, cb: (PlayaItemWithUserData) -> Unit) {
//
//    }

}