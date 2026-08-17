package com.gaiagps.iburn.fragment

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.gaiagps.iburn.IntentUtil
import com.gaiagps.iburn.adapters.AdapterListener
import com.gaiagps.iburn.adapters.DividerItemDecoration
import com.gaiagps.iburn.adapters.MultiTypePlayaItemAdapter
import com.gaiagps.iburn.database.DataProvider
import com.gaiagps.iburn.database.PlayaItem
import com.gaiagps.iburn.database.PlayaItemWithUserData
import com.gaiagps.iburn.database.SectionedPlayaItems
import com.gaiagps.iburn.databinding.ActivitySearchBinding
import com.tonicartos.superslim.LayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale

class SearchFragment : Fragment(), AdapterListener {

    private var adapter: MultiTypePlayaItemAdapter? = null
    private var searchJob: Job? = null
    private lateinit var binding: ActivitySearchBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = ActivitySearchBinding.inflate(layoutInflater)

        val context = requireContext()
        adapter = MultiTypePlayaItemAdapter(context, this)

        val resultList: RecyclerView = binding.results
        resultList.layoutManager = LayoutManager(context)
        resultList.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL_LIST))
        resultList.adapter = adapter

        binding.search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // No-op
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // No-op
            }

            override fun afterTextChanged(s: Editable) {
                dispatchSearchQuery(s.toString())
            }
        })

        binding.search.setOnEditorActionListener { _, _, _ ->
            requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
            (requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(binding.search.windowToken, 0)
            true
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        binding.search.requestFocus()
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.search, InputMethodManager.SHOW_IMPLICIT)
    }

    /**
     * Dispatch a search query to the current Fragment in the FragmentPagerAdapter
     */
    private fun dispatchSearchQuery(query: String) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            try {
                val provider = DataProvider.getInstance(requireContext().applicationContext)
                provider.observeFtsQuery(query).collectLatest { items ->
                    binding.resultsSummary.text = describeResults(items)
                    adapter?.sectionedItems = items
                }
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                Timber.w(t, "FTS search failed; showing no results")
                adapter?.sectionedItems =
                    SectionedPlayaItems(
                        emptyList(),
                        emptyList()
                    )
                binding.resultsSummary.text = "0 results"
            }
        }
    }

    private fun describeResults(searchResults: SectionedPlayaItems): String {
        return String.format(
            Locale.US, "%d results",
            searchResults.data.size
        )
    }

    override fun onItemSelected(item: PlayaItemWithUserData) {
        IntentUtil.viewItemDetail(requireActivity(), item.item)
    }

    override fun onItemFavoriteButtonSelected(item: PlayaItem) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                DataProvider.getInstance(requireContext().applicationContext).toggleFavorite(item)
            } catch (t: Throwable) {
                Timber.e(t, "failed to toggle favorite")
            }
        }
    }

    override fun onDestroyView() {
        searchJob?.cancel()
        searchJob = null
        adapter = null
        super.onDestroyView()
    }
}
