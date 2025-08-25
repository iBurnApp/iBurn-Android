package com.gaiagps.iburn.fragment

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.gaiagps.iburn.database.DataProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

class CampListViewFragment : PlayaListViewFragment() {
    companion object { fun newInstance() = CampListViewFragment() }

    override fun startObserving() {
        val provider = DataProvider.getInstance(requireActivity().applicationContext)
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            provider.observeCamps().collect { camps ->
                Timber.d("Got camps")
                onDataChanged(camps)
            }
        }
    }
}

