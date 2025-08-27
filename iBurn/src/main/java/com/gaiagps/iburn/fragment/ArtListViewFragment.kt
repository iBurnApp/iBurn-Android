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
import com.gaiagps.iburn.adapters.DividerItemDecoration
import com.gaiagps.iburn.database.DataProvider
import com.gaiagps.iburn.database.PlayaItemWithUserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import timber.log.Timber

class ArtListViewFragment : PlayaListViewFragment(), com.gaiagps.iburn.view.ArtListHeader.Listener {

    companion object { fun newInstance() = ArtListViewFragment() }

    private var showAudioTourOnly: Boolean = false

    override fun startObserving() {
        val provider = DataProvider.getInstance(requireActivity().applicationContext)
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            val flow = if (showAudioTourOnly) provider.observeArtWithAudioTour() else provider.observeArt()
            flow.collect { art ->
                Timber.d("Got Art")
                 onDataChanged(art)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_art_list_view, container, false)
        mEmptyText = v.findViewById<TextView>(android.R.id.empty)
        mRecyclerView = v.findViewById<RecyclerView>(android.R.id.list)
        mRecyclerView?.layoutManager = LinearLayoutManager(activity)
        mRecyclerView?.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL_LIST))
        v.findViewById<com.gaiagps.iburn.view.ArtListHeader>(R.id.header).setListener(this)
        return v
    }

    override fun onSelectionChanged(showAudioTourOnly: Boolean) {
        this.showAudioTourOnly = showAudioTourOnly
        // Will restart observation on next start; for immediate refresh, you can trigger re-subscribe
        startObserving()
    }
}

