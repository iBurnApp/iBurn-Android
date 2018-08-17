package com.gaiagps.iburn.adapters

import com.gaiagps.iburn.api.typeadapter.PlayaDateTypeAdapter
import com.gaiagps.iburn.database.PlayaItem
import com.gaiagps.iburn.database.Event
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


/**
 * Created by dbro on 6/14/17.
 */
class EventStartTimeSectionIndexer(items: List<PlayaItem>? = null) : PlayaItemSectionIndxer(items) {

    private val apiDateFormat = PlayaDateTypeAdapter.buildIso8601Format()
    private val humanDateFormat = SimpleDateFormat("E h a", Locale.US)

    private var sections: ArrayList<String>? = null
    private var sectionPositions: ArrayList<Int>? = null

    override fun getSections(): Array<Any> {
        if (sections == null && items != null) {

            val newSections = ArrayList<String>()
            val newSectionPositions = ArrayList<Int>()

            var lastSection = ""
            items?.forEachIndexed { index, event ->
                val thisSection = getSectionStringForEvent(event as Event)
                if (thisSection != lastSection) {
                    newSections.add(thisSection)
                    newSectionPositions.add(index)
                }
                lastSection = thisSection
            }

            sections = newSections
            sectionPositions = newSectionPositions
        } else if (items == null) {
            return emptyArray()
        }

        return sections?.toArray() as Array<Any>
    }

    override fun getSectionForPosition(position: Int): Int {
        // Requesting the lastIndex of an empty array yields -1, which will produce ArrayIndexOutOfBoundsExceptions
        if (sectionPositions?.isEmpty() ?: true) return 0

        sectionPositions?.forEachIndexed { index, sectionPosition ->

            if (sectionPosition > position) return index - 1
        }
        return sectionPositions?.lastIndex ?: 0
    }

    override fun getPositionForSection(section: Int): Int {
        // not needed
        return 0
    }

    private fun getSectionStringForEvent(event: Event): String {
        if (event.allDay) {
            return "All ${event.startTimePretty}"
        } else {
            try {
                return humanDateFormat.format(apiDateFormat.parse(event.startTime))
            } catch (e: ParseException) {
                return event.startTimePretty
            }
        }
    }
}
