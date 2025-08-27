package com.gaiagps.iburn.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gaiagps.iburn.CurrentDateProvider
import com.gaiagps.iburn.R
import com.gaiagps.iburn.adapters.DividerItemDecoration
import com.gaiagps.iburn.adapters.PlayaItemAdapter
import com.gaiagps.iburn.adapters.UpcomingEventsAdapter
import com.gaiagps.iburn.database.DataProvider
import com.tonicartos.superslim.LayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

class ExploreListViewFragment : PlayaListViewFragment() {
    companion object { fun newInstance() = ExploreListViewFragment() }

    override fun createAdapter(): PlayaItemAdapter = UpcomingEventsAdapter(requireContext().applicationContext, this)

    override fun startObserving() {
        val now = CurrentDateProvider.getCurrentDate()
        val endCal = Calendar.getInstance().apply { time = now; add(Calendar.HOUR, 7) }
        val end = endCal.time
        val provider = DataProvider.getInstance(requireActivity().applicationContext)
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            provider.observeEventBetweenDates(now, end).collect { events ->
                Timber.d("Data onNext %d items", events.size)
                onDataChanged(events)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_playa_list_view, container, false)
        mEmptyText = v.findViewById<TextView>(android.R.id.empty)
        mRecyclerView = v.findViewById(android.R.id.list)
        mRecyclerView?.layoutManager = LinearLayoutManager(activity)
        mRecyclerView?.layoutManager = LayoutManager(activity)
        mRecyclerView?.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL_LIST))
        return v
    }

    override fun getEmptyText(): String = getString(R.string.no_now_items)
}

