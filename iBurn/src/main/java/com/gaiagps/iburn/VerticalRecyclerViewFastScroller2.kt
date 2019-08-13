package com.gaiagps.iburn

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.AttributeSet
import xyz.danoz.recyclerviewfastscroller.calculation.VerticalScrollBoundsProvider
import xyz.danoz.recyclerviewfastscroller.calculation.position.VerticalScreenPositionCalculator
import xyz.danoz.recyclerviewfastscroller.calculation.progress.TouchableScrollProgressCalculator
import xyz.danoz.recyclerviewfastscroller.calculation.progress.VerticalLinearLayoutManagerScrollProgressCalculator
import xyz.danoz.recyclerviewfastscroller.calculation.progress.VerticalScrollProgressCalculator
import xyz.danoz.recyclerviewfastscroller.vertical.VerticalRecyclerViewFastScroller

/**
 * Modified fast scroller that uses a modified scroll progress calculator.
 *
 * For whatever reason large art cells in recyclerview would occasionally result in
 * findLastCompletelyVisibleItemPosition returning -1, which would cause the fast scroller
 * to incorrectly flicker between the top of the list and its correct position during a fling.
 *
 * Also, the previous fastscroller could scroll backwards during a fling if cells have
 * unequal height. This scroller uses a simpler logic that may be less accurate but is definitely
 * more visually pleasing.
 * Created by dbro on 8/22/17.
 */
public class VerticalRecyclerViewFastScroller2(context: Context, attributeSet: AttributeSet?, defStyleAttributes: Int) : VerticalRecyclerViewFastScroller(context, attributeSet, defStyleAttributes) {

    constructor(context: Context, attributeSet: AttributeSet) : this(context, attributeSet, 0)
    constructor(context: Context) : this(context, null, 0)

    private var mScrollProgressCalculator: VerticalScrollProgressCalculator? = null
    private var mScreenPositionCalculator: VerticalScreenPositionCalculator? = null

    override fun onCreateScrollProgressCalculator() {
        val boundsProvider = VerticalScrollBoundsProvider(this.mBar.y, this.mBar.y + this.mBar.height.toFloat() - this.mHandle.height.toFloat())
        this.mScrollProgressCalculator = VerticalLinearLayoutManagerScrollProgressCalculator2(boundsProvider)
        this.mScreenPositionCalculator = VerticalScreenPositionCalculator(boundsProvider)
    }


    class VerticalLinearLayoutManagerScrollProgressCalculator2(verticalScrollBoundsProvider: VerticalScrollBoundsProvider) : VerticalLinearLayoutManagerScrollProgressCalculator(verticalScrollBoundsProvider) {
        override fun calculateScrollProgress(recyclerView: androidx.recyclerview.widget.RecyclerView?): Float {
            if (recyclerView == null) return 0f

            val layoutManager = recyclerView.layoutManager as androidx.recyclerview.widget.LinearLayoutManager
            val thisItem = layoutManager.findLastVisibleItemPosition()
            val itemCount = layoutManager.itemCount

            if (itemCount == 0) return 0f

            return (thisItem.toFloat() / itemCount)
        }
    }

    override fun getScrollProgressCalculator(): TouchableScrollProgressCalculator? {
        return mScrollProgressCalculator
    }

    override fun moveHandleToPosition(scrollProgress: Float) {
        if (this.mScreenPositionCalculator != null) {
            this.mHandle.y = this.mScreenPositionCalculator!!.getYPositionFromScrollProgress(scrollProgress)
        }
    }
}