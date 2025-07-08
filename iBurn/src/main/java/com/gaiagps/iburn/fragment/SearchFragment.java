package com.gaiagps.iburn.fragment;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.gaiagps.iburn.IntentUtil;
import com.gaiagps.iburn.adapters.AdapterListener;
import com.gaiagps.iburn.adapters.DividerItemDecoration;
import com.gaiagps.iburn.adapters.MultiTypePlayaItemAdapter;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.PlayaItem;
import com.gaiagps.iburn.database.PlayaItemWithUserData;
import com.gaiagps.iburn.databinding.ActivitySearchBinding;
import com.tonicartos.superslim.LayoutManager;

import java.util.Locale;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class SearchFragment extends Fragment implements AdapterListener {

    private MultiTypePlayaItemAdapter adapter;
    private Disposable searchSubscription;

    private ActivitySearchBinding binding;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ActivitySearchBinding.inflate(getLayoutInflater());

        Context activityContext = getContext();
        adapter = new MultiTypePlayaItemAdapter(activityContext, this);

        RecyclerView resultList = binding.results;
        resultList.setLayoutManager(new LayoutManager(activityContext));
        resultList.addItemDecoration(new DividerItemDecoration(activityContext, DividerItemDecoration.VERTICAL_LIST));
        resultList.setAdapter(adapter);

        binding.search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                dispatchSearchQuery(s.toString());
            }
        });

        binding.search.setOnEditorActionListener((view, actionId, event) -> {
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
            ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(binding.search.getWindowToken(), 0);
            return true;
        });

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        binding.search.requestFocus();
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(binding.search, InputMethodManager.SHOW_IMPLICIT);
    }

    /**
     * Dispatch a search query to the current Fragment in the FragmentPagerAdapter
     */
    private void dispatchSearchQuery(String query) {
        if (searchSubscription != null && !searchSubscription.isDisposed())
            searchSubscription.dispose();

        searchSubscription = DataProvider.Companion.getInstance(getContext().getApplicationContext())
                .flatMap(dataProvider -> dataProvider.observeFtsQuery(query).toObservable()) // TODO : rm toObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sectionedPlayaItems -> {
                    binding.resultsSummary.setText(describeResults(sectionedPlayaItems));
                    adapter.setSectionedItems(sectionedPlayaItems);
                });

    }

    private String describeResults(DataProvider.SectionedPlayaItems searchResults) {
        return String.format(Locale.US, "%d results",
                searchResults.getData().size());
    }

    @Override
    public void onItemSelected(PlayaItemWithUserData item) {
        IntentUtil.viewItemDetail(getActivity(), item.getItem());
    }

    @Override
    public void onItemFavoriteButtonSelected(PlayaItem item) {
        DataProvider.Companion.getInstance(getContext().getApplicationContext())
                .observeOn(Schedulers.io())
                .subscribe(dataProvider -> {
                    dataProvider.toggleFavorite(item);
                }, throwable -> Timber.e(throwable, "failed to toggle favorite"));
    }
}
