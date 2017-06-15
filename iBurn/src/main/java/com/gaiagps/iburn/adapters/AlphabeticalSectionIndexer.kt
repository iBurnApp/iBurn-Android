package com.gaiagps.iburn.adapters

import com.gaiagps.iburn.database.PlayaItem

/**
 * Created by dbro on 8/18/15.
 */

class AlphabeticalSectionIndexer(items: List<PlayaItem>? = null) : PlayaItemSectionIndxer(items) {

    val sections: Array<String> = arrayOf("!", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z") as Array<String>

    override fun getSections(): Array<Any> {
        return sections as Array<Any>
    }

    override fun getSectionForPosition(position: Int): Int {
        if (position == items?.size) return sections.lastIndex

        val name = items?.get(position)?.name
        if (name == null) {
            return sections.lastIndex
        } else {
            return getSectionIndexForName(name)
        }
    }

    override fun getPositionForSection(section: Int): Int {
        // not needed
        return 0
    }

    private fun getSectionIndexForName(title: String): Int {
        return Math.max(0, sections.indexOf(title.first().toString().toUpperCase()))
    }
}