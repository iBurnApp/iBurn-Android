package com.gaiagps.iburn.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gaiagps.iburn.R
import com.gaiagps.iburn.adapters.AlphabeticalSectionIndexer
import com.gaiagps.iburn.adapters.DividerItemDecoration
import com.gaiagps.iburn.adapters.EventStartTimeSectionIndexer
import com.gaiagps.iburn.adapters.PlayaItemAdapter
import com.gaiagps.iburn.database.DataProvider
import com.gaiagps.iburn.view.ArtListHeader
import com.gaiagps.iburn.view.BrowseListHeader
import com.gaiagps.iburn.view.EventListHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import com.gaiagps.iburn.VerticalRecyclerViewFastScroller2
import com.gaiagps.iburn.adapters.SimpleSectionTitleIndicator

class BrowseListViewFragment : PlayaListViewFragment(), EventListHeader.PlayaListViewHeaderReceiver, BrowseListHeader.BrowseSelectionListener, com.gaiagps.iburn.view.ArtListHeader.Listener {

    companion object { fun newInstance() = BrowseListViewFragment() }

    private var artListHeader: ViewGroup? = null
    private var eventListHeader: ViewGroup? = null
    private var categorySelection: BrowseListHeader.BrowseSelection = BrowseListHeader.BrowseSelection.CAMPS

    private var selectedDay: String = com.gaiagps.iburn.adapters.AdapterUtils.getCurrentOrFirstDayAbbreviation()
    private var selectedTypes: ArrayList<String>? = null
    private var includeExpired: Boolean = false
    private var eventTiming: String = "timed"

    private var showAudioTourOnly = false

    override fun startObserving() {
        val provider = DataProvider.getInstance(requireActivity().applicationContext)
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            when (categorySelection) {
                BrowseListHeader.BrowseSelection.CAMPS -> {
                    adapter.sectionIndexer = AlphabeticalSectionIndexer()
                    provider.observeCamps().collect { onDataChanged(it) }
                }
                BrowseListHeader.BrowseSelection.ART -> {
                    adapter.sectionIndexer = AlphabeticalSectionIndexer()
                    val flow = if (showAudioTourOnly) provider.observeArtWithAudioTour() else provider.observeArt()
                    flow.collect { onDataChanged(it) }
                }
                BrowseListHeader.BrowseSelection.EVENT -> {
                    adapter.sectionIndexer = EventStartTimeSectionIndexer()
                    provider.observeEventsOnDayOfTypes(selectedDay, selectedTypes, includeExpired, eventTiming)
                        .collect { onDataChanged(it) }
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_browse_list_view, container, false)
        eventListHeader = v.findViewById(R.id.eventHeader)
        artListHeader = v.findViewById(R.id.artHeader)
        mEmptyText = v.findViewById(android.R.id.empty)
        mRecyclerView = v.findViewById(android.R.id.list)
        mRecyclerView?.layoutManager = LinearLayoutManager(activity)
        mRecyclerView?.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL_LIST))
        v.findViewById<BrowseListHeader>(R.id.header).setBrowseSelectionListener(this)
        v.findViewById<EventListHeader>(R.id.eventHeader).setReceiver(this)
        v.findViewById<ArtListHeader>(R.id.artHeader).setListener(this)

        // Bind fast scroller to RecyclerView and section indicator to avoid NPEs
        v.findViewById<VerticalRecyclerViewFastScroller2>(R.id.fastScroller)?.let { scroller ->
            mRecyclerView?.let { rv ->
                scroller.setRecyclerView(rv)
            }
            v.findViewById<SimpleSectionTitleIndicator>(R.id.fastScrollerSectionIndicator)?.let { indicator ->
                scroller.setSectionIndicator(indicator)
            }
        }
        return v
    }

    override fun onSelectionChanged(day: String, types: ArrayList<String>?, expired: Boolean, timing: String) {
        selectedDay = day
        selectedTypes = types
        includeExpired = expired
        eventTiming = timing
        startObserving()
    }

    override fun onSelectionChanged(selection: BrowseListHeader.BrowseSelection) {
        when (selection) {
            BrowseListHeader.BrowseSelection.CAMPS -> {
                eventListHeader?.visibility = View.GONE
                artListHeader?.visibility = View.GONE
            }
            BrowseListHeader.BrowseSelection.ART -> {
                eventListHeader?.visibility = View.GONE
                artListHeader?.visibility = View.VISIBLE
            }
            BrowseListHeader.BrowseSelection.EVENT -> {
                eventListHeader?.visibility = View.VISIBLE
                artListHeader?.visibility = View.GONE
            }
        }
        if (categorySelection != selection) {
            categorySelection = selection
            adapter.items = ArrayList(0)
            startObserving()
        }
    }

    override fun onSelectionChanged(showAudioTourOnly: Boolean) {
        this.showAudioTourOnly = showAudioTourOnly
        adapter.items = ArrayList(0)
        startObserving()
    }
}
