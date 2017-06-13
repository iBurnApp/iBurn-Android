package com.gaiagps.iburn.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.widget.TextView
import com.gaiagps.iburn.R
import com.gaiagps.iburn.database.*

/**
 * Created by dbro on 6/13/17.
 */
class PlayaSearchItemAdapter(context: Context, listener: AdapterListener) :
        SectionedPlayaItemAdapter(context, listener) {

    var sectionedItems: DataProvider.SectionedPlayaItems? = null
        set(value) {
            field = value
            items = sectionedItems?.data
        }

    override fun onBindHeaderViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        setLinearSlimParameters(viewHolder, position)

        val firstSectionItem = getDataPositionForPosition(position + 1)
        val item = items?.get(firstSectionItem)
        var headerText: String? = null
        if (item is Camp) {
            headerText = context.getString(R.string.camps_tab)
        } else if (item is Art) {
            headerText = context.getString(R.string.art_tab)
        } else if (item is Event) {
            headerText = context.getString(R.string.events_tab)
        } else if (item is UserPoi) {
            headerText = "YOUR MAP MARKERS" // TODO : Resourceify
        }

        // SectionedPlayaItemAdapter sets header layout to a single TextView
        (viewHolder.itemView as TextView).text = headerText
    }

    override fun createHeaderPositionsForItems(items: List<PlayaItem>): Set<Int> {
        val set = HashSet<Int>()
        var headerCount = 0
        sectionedItems?.ranges?.forEach { range ->
            set.add(range.first + headerCount++)
        }
        return set
    }

}