package com.gaiagps.iburn.fragment

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gaiagps.iburn.IntentUtil
import com.gaiagps.iburn.R
import com.gaiagps.iburn.adapters.AdapterListener
import com.gaiagps.iburn.adapters.DividerItemDecoration
import com.gaiagps.iburn.adapters.MultiTypePlayaItemAdapter
import com.gaiagps.iburn.adapters.PlayaItemAdapter
import com.gaiagps.iburn.database.DataProvider
import com.gaiagps.iburn.database.PlayaItem
import com.gaiagps.iburn.database.PlayaItemWithUserData
import com.gaiagps.iburn.database.SectionedPlayaItems
import com.tonicartos.superslim.LayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

abstract class PlayaListViewFragment : Fragment(), AdapterListener {
    companion object { const val ARG_SCROLL_POS = "spos" }

    protected lateinit var adapter: PlayaItemAdapter
        private set
    protected var mRecyclerView: RecyclerView? = null
    protected var mEmptyText: TextView? = null

    private var lastScrollPos: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            lastScrollPos = savedInstanceState.getInt(ARG_SCROLL_POS, 0)
            Timber.d("%s onCreate with scroll ps %d", javaClass.simpleName, lastScrollPos)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        adapter = createAdapter()
        mRecyclerView?.adapter = adapter
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        lastScrollPos = getScrollPosition()
        outState.putInt(ARG_SCROLL_POS, lastScrollPos)
        Timber.d("%s onSaveInstanceState with scroll pos %d", javaClass.simpleName, lastScrollPos)
    }

    override fun onStart() {
        super.onStart()
        startObserving()
    }

    override fun onStop() {
        super.onStop()
        stopObserving()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::adapter.isInitialized) {
            adapter.cleanup()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Timber.d("%s onCreateView", javaClass.simpleName)
        val v = inflater.inflate(R.layout.fragment_playa_list_view, container, false)
        mEmptyText = v.findViewById(android.R.id.empty)
        mRecyclerView = v.findViewById(android.R.id.list)
        mRecyclerView?.layoutManager = LinearLayoutManager(activity)
        mRecyclerView?.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL_LIST))
        return v
    }

    protected fun onDataChanged(newData: List<PlayaItemWithUserData>?) {
        if (newData == null) {
            Timber.w("Got null data onDataChanged")
            return
        }
        prepareForNewData(newData)
        adapter.items = newData
        restoreScrollPosition()
    }

    protected fun onDataChanged(newData: SectionedPlayaItems?) {
        if (newData == null) {
            Timber.w("Got null data onDataChanged")
            return
        }
        prepareForNewData(newData.data)
        if (adapter is MultiTypePlayaItemAdapter) {
            (adapter as MultiTypePlayaItemAdapter).sectionedItems = newData
        } else {
            Timber.w("Sectioned data provided but adapter does not seem to support sections")
            adapter.items = newData.data
        }
        restoreScrollPosition()
    }

    private fun prepareForNewData(newData: List<out PlayaItemWithUserData>) {
        lastScrollPos = getScrollPosition()
        Timber.d("%s onDataChanged Had %d items. Now %d items", javaClass.simpleName, adapter.itemCount, newData.size)
        val adapterWasEmpty = adapter.itemCount == 0
        if (adapterWasEmpty && newData.isNotEmpty()) {
            val fadeAnimation = AlphaAnimation(0f, 1f)
            fadeAnimation.duration = 250
            fadeAnimation.startOffset = 100
            fadeAnimation.fillAfter = true
            fadeAnimation.isFillEnabled = true
            mRecyclerView?.startAnimation(fadeAnimation)
        }
        setListShown(newData.isNotEmpty())
    }

    private fun restoreScrollPosition() {
        Timber.d("%s Scrolling to prior scroll position %d", javaClass.simpleName, lastScrollPos)
        mRecyclerView?.scrollToPosition(lastScrollPos)
    }

    protected open fun createAdapter(): PlayaItemAdapter = PlayaItemAdapter(requireContext(), this)

    protected abstract fun startObserving()
    protected open fun stopObserving() {}

    open fun getEmptyText(): String = getString(R.string.no_items_found)

    fun setListShown(doShow: Boolean) {
        if (doShow) {
            mRecyclerView?.visibility = View.VISIBLE
            mEmptyText?.visibility = View.GONE
        } else {
            mRecyclerView?.visibility = View.INVISIBLE
            mEmptyText?.text = getEmptyText()
            mEmptyText?.visibility = View.VISIBLE
        }
    }

    fun setListShownNoAnimation(doShow: Boolean) { setListShown(doShow) }

    override fun onItemSelected(item: PlayaItemWithUserData) {
        IntentUtil.viewItemDetail(activity as Activity, item.item)
    }

    override fun onItemFavoriteButtonSelected(item: PlayaItem) {
        Timber.d("onItemFavoriteButtonSelected for %s", item.playaId)
        // Simple background update
        GlobalScope.launch(Dispatchers.IO) {
            DataProvider.getInstance(requireActivity().applicationContext).toggleFavorite(item)
        }
    }

    private fun getScrollPosition(): Int {
        mRecyclerView?.let { rv ->
            var scrollPos = 0
            when (val lm = rv.layoutManager) {
                is LayoutManager -> scrollPos = lm.findFirstCompletelyVisibleItemPosition()
                is LinearLayoutManager -> scrollPos = lm.findFirstCompletelyVisibleItemPosition()
            }
            Timber.d("%s onSaveInstanceState scrollPos %d", javaClass.simpleName, scrollPos)
            return scrollPos
        }
        return 0
    }
}

