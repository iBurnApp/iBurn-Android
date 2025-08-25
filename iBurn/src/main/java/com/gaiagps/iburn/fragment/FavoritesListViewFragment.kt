package com.gaiagps.iburn.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gaiagps.iburn.R
import com.gaiagps.iburn.adapters.DividerItemDecoration
import com.gaiagps.iburn.adapters.MultiTypePlayaItemAdapter
import com.gaiagps.iburn.adapters.PlayaItemAdapter
import com.gaiagps.iburn.database.DataProvider
import com.tonicartos.superslim.LayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

class FavoritesListViewFragment : PlayaListViewFragment() {
    override fun createAdapter(): PlayaItemAdapter = MultiTypePlayaItemAdapter(requireContext().applicationContext, this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_playa_list_view, container, false)
        mEmptyText = v.findViewById(android.R.id.empty)
        mRecyclerView = v.findViewById(android.R.id.list)
        mRecyclerView?.layoutManager = LinearLayoutManager(activity)
        mRecyclerView?.layoutManager = LayoutManager(activity)
        mRecyclerView?.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL_LIST))
        return v
    }

    override fun startObserving() {
        val provider = DataProvider.getInstance(requireActivity().applicationContext)
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            provider.observeFavorites().collect { items ->
                Timber.d("Loaded favorites: %d", items.data.size)
                onDataChanged(items)
            }
        }
    }

    override fun getEmptyText(): String = getString(R.string.mark_some_favorites)
}

