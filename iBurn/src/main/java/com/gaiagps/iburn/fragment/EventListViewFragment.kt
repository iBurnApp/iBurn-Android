package com.gaiagps.iburn.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gaiagps.iburn.R
import com.gaiagps.iburn.adapters.AdapterUtils
import com.gaiagps.iburn.adapters.DividerItemDecoration
import com.gaiagps.iburn.database.DataProvider
import com.gaiagps.iburn.view.EventListHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

class EventListViewFragment : PlayaListViewFragment(), EventListHeader.PlayaListViewHeaderReceiver {
    companion object { fun newInstance() = EventListViewFragment() }

    private var selectedDay: String = AdapterUtils.getCurrentOrFirstDayAbbreviation()
    private var selectedTypes: ArrayList<String>? = null
    private var includeExpired: Boolean = false

    override fun startObserving() {
        val provider = DataProvider.getInstance(requireActivity().applicationContext)
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            provider.observeEventsOnDayOfTypes(selectedDay, selectedTypes, includeExpired)
                .collect { events ->
                    Timber.d("Data onNext %d items", events.size)
                    onDataChanged(events)
                }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_event_list_view, container, false)
        mEmptyText = v.findViewById<TextView>(android.R.id.empty)
        mRecyclerView = v.findViewById<RecyclerView>(android.R.id.list)
        mRecyclerView?.layoutManager = LinearLayoutManager(activity)
        mRecyclerView?.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL_LIST))
        v.findViewById<EventListHeader>(R.id.header).setReceiver(this)
        return v
    }

    override fun onSelectionChanged(day: String, types: ArrayList<String>?, expired: Boolean) {
        selectedDay = day
        selectedTypes = types
        includeExpired = expired
        startObserving()
    }
}
