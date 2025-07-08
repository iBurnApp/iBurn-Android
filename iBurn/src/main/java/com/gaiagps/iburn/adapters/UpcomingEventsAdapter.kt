package com.gaiagps.iburn.adapters

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import com.gaiagps.iburn.CurrentDateProvider
import com.gaiagps.iburn.DateUtil
import com.gaiagps.iburn.api.typeadapter.PlayaDateTypeAdapter
import com.gaiagps.iburn.database.*
import java.util.Locale

/**
 * Created by dbro on 6/13/17.
 */
class UpcomingEventsAdapter(context: Context, listener: AdapterListener) :
        SectionedPlayaItemAdapter(context, listener) {

    override fun createHeaderPositionsForItems(items: List<PlayaItemWithUserData>): Set<Int> {
        val set = HashSet<Int>()
        var headerCount = 0
        var lastStartTime = ""
        items.forEachIndexed { index, playaItem ->
            val thisStartTime = (playaItem as Event).startTime
            requireNotNull(thisStartTime) { "Event start time cannot be null" }
            if (thisStartTime != lastStartTime) {
                set.add(index + headerCount++)
            }
            lastStartTime = thisStartTime
        }
        return set
    }

    override fun onBindHeaderViewHolder(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
        setLinearSlimParameters(viewHolder, position)

        val firstSectionItem = getDataPositionForPosition(position + 1)
        val item = items?.get(firstSectionItem) as Event

        val headerTitle = DateUtil.getStartDateString(
                    apiDateFormat.parse(item.startTime),
                    CurrentDateProvider.getCurrentDate()).uppercase(Locale.getDefault())

        // SectionedPlayaItemAdapter sets header layout to a single TextView
        (viewHolder.itemView as TextView).text = headerTitle
    }
}