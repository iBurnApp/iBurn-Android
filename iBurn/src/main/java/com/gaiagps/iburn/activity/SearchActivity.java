package com.gaiagps.iburn.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.adapters.AdapterListener;
import com.gaiagps.iburn.adapters.DividerItemDecoration;
import com.gaiagps.iburn.adapters.PlayaSearchResponseCursorAdapter;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.PlayaDatabase;
import com.squareup.sqlbrite.SqlBrite;
import com.tonicartos.superslim.LayoutManager;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

public class SearchActivity extends AppCompatActivity implements AdapterListener {

    private PlayaSearchResponseCursorAdapter adapter;
    private Subscription searchSubscription;

    @InjectView(R.id.results)
    RecyclerView resultList;

    @InjectView(R.id.search)
    EditText searchEntry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ButterKnife.inject(this);

        adapter = new PlayaSearchResponseCursorAdapter(this, null, this);

        resultList.setLayoutManager(new LayoutManager(this));
        resultList.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
        resultList.setAdapter(adapter);

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
        if (searchSubscription != null && !searchSubscription.isUnsubscribed())
            searchSubscription.unsubscribe();

        searchSubscription = DataProvider.getInstance(this)
                .flatMap(dataProvider -> dataProvider.observeNameQuery(query, adapter.getRequiredProjection()))
                .map(SqlBrite.Query::run)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(adapter::changeCursor);

    }

    @Override
    public void onItemSelected(int modelId, Constants.PlayaItemType type) {
        // Launch detail activity?
        Intent i = new Intent(this, PlayaItemViewActivity.class);
        i.putExtra("model_id", modelId);
        i.putExtra("model_type", type);
        startActivity(i);
    }

    @Override
    public void onItemFavoriteButtonSelected(int modelId, Constants.PlayaItemType type) {
        final String modelTable;
        switch (type) {
            case CAMP:
                modelTable = PlayaDatabase.CAMPS;
                break;
            case ART:
                modelTable = PlayaDatabase.ART;
                break;
            case EVENT:
                modelTable = PlayaDatabase.EVENTS;
                break;

            default:
                throw new IllegalArgumentException("Invalid type " + type);
        }

        DataProvider.getInstance(this)
                .subscribe(dataProvider -> {
                    dataProvider.toggleFavorite(modelTable, modelId);
                }, throwable -> Timber.e(throwable, "failed to toggle favorite"));
    }

    public void onBackButtonClick(View view) {
        this.finish();
    }
}
