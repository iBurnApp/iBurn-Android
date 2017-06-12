package com.gaiagps.iburn.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.gaiagps.iburn.IntentUtil;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.adapters.AdapterListener;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.PlayaItem;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

//import butterknife.BindView;
//import butterknife.ButterKnife;

public class SearchActivity extends AppCompatActivity implements AdapterListener {

    //    private PlayaSearchResponseCursorAdapter adapter;
    private Disposable searchSubscription;

    @BindView(R.id.results)
    RecyclerView resultList;

    @BindView(R.id.search)
    EditText searchEntry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ButterKnife.bind(this);

        resultList = (RecyclerView) findViewById(R.id.results);
        searchEntry = (EditText) findViewById(R.id.search);

//        adapter = new PlayaSearchResponseCursorAdapter(this, null, this);
//
//        resultList.setLayoutManager(new LayoutManager(this));
//        resultList.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
//        resultList.setAdapter(adapter);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        searchEntry.addTextChangedListener(new TextWatcher() {
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

        searchEntry.setOnEditorActionListener((view, actionId, event) -> {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(searchEntry.getWindowToken(), 0);
            return true;
        });

    }

    /**
     * Dispatch a search query to the current Fragment in the FragmentPagerAdapter
     */
    private void dispatchSearchQuery(String query) {
        if (searchSubscription != null && !searchSubscription.isDisposed())
            searchSubscription.dispose();

        searchSubscription = DataProvider.Companion.getInstance(getApplicationContext())
                .flatMap(dataProvider -> dataProvider.observeNameQuery(query).toObservable()) // TODO : rm toObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(playaItems -> {
                    // TODO : Send to adapter
                });

    }

    @Override
    public void onItemSelected(PlayaItem item) {
        IntentUtil.viewItemDetail(this, item);
    }

    @Override
    public void onItemFavoriteButtonSelected(PlayaItem item) {
        DataProvider.Companion.getInstance(getApplicationContext())
                .subscribe(dataProvider -> {
                    dataProvider.toggleFavorite(item);
                }, throwable -> Timber.e(throwable, "failed to toggle favorite"));
    }

    public void onBackButtonClick(View view) {
        this.finish();
    }
}
