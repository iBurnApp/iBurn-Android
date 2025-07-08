package com.gaiagps.iburn.adapters

import android.widget.SectionIndexer
import com.gaiagps.iburn.database.PlayaItem
import com.gaiagps.iburn.database.PlayaItemWithUserData

/**
 * Created by dbro on 6/14/17.
 */
abstract class PlayaItemSectionIndxer(var items: List<PlayaItemWithUserData>? = null) : SectionIndexer