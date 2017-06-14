package com.gaiagps.iburn.adapters

import android.widget.SectionIndexer
import com.gaiagps.iburn.database.PlayaItem

/**
 * Created by dbro on 6/14/17.
 */
abstract class PlayaItemSectionIndxer(var items: List<PlayaItem>? = null) : SectionIndexer