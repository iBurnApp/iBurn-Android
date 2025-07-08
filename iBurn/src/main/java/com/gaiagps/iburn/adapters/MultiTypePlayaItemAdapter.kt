package com.gaiagps.iburn.adapters

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import com.gaiagps.iburn.R
import com.gaiagps.iburn.database.*

/**
 * Created by dbro on 6/13/17.
 */
class MultiTypePlayaItemAdapter(context: Context, listener: AdapterListener) :
        SectionedPlayaItemAdapter(context, listener) {

    var sectionedItems: DataProvider.SectionedPlayaItems? = null
        set(value) {
            field = value
            items = sectionedItems?.data
        }

    override fun onBindHeaderViewHolder(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
        setLinearSlimParameters(viewHolder, position)

        val firstSectionItem = getDataPositionForPosition(position + 1)
        val item = items?.get(firstSectionItem)?.item
        var headerText: String? = null
        when (item) {
            is Camp -> {
                headerText = context.getString(R.string.camps_tab)
            }

            is Art -> {
                headerText = context.getString(R.string.art_tab)
            }

            is Event -> {
                headerText = context.getString(R.string.events_tab)
            }

            is UserPoi -> {
                headerText = "YOUR MAP MARKERS" // TODO : Resourceify
            }
        }

        // SectionedPlayaItemAdapter sets header layout to a single TextView
        (viewHolder.itemView as TextView).text = headerText
    }

    override fun createHeaderPositionsForItems(items: List<PlayaItemWithUserData>): Set<Int> {
        val set = HashSet<Int>()
        var headerCount = 0
        sectionedItems?.ranges?.forEach { range ->
            set.add(range.first + headerCount++)
        }
        return set
    }

}