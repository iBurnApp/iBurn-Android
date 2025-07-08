package com.gaiagps.iburn.adapters

import com.gaiagps.iburn.database.PlayaItem
import com.gaiagps.iburn.database.PlayaItemWithUserData
import java.util.Locale

/**
 * Created by dbro on 8/18/15.
 */

class AlphabeticalSectionIndexer(items: List<PlayaItemWithUserData>? = null) : PlayaItemSectionIndxer(items) {

    val sections: Array<String> = arrayOf("!", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z") as Array<String>

    override fun getSections(): Array<Any> {
        return sections as Array<Any>
    }

    override fun getSectionForPosition(position: Int): Int {
        // Requesting the lastIndex of an empty array yields -1, which will produce ArrayIndexOutOfBoundsExceptions
        if (items?.isEmpty() ?: true) return 0
        
        if (position == items?.size) return sections.lastIndex

        val name = items?.get(position)?.item?.name
        return if (name == null) {
            sections.lastIndex
        } else {
            getSectionIndexForName(name)
        }
    }

    override fun getPositionForSection(section: Int): Int {
        // not needed
        return 0
    }

    private fun getSectionIndexForName(title: String): Int {
        return Math.max(0, sections.indexOf(title.first().toString().uppercase(Locale.getDefault())))
    }
}