package com.gaiagps.iburn.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.adapters.AdapterItemSelectedListener;
import com.gaiagps.iburn.adapters.PlayaSearchResponseCursorAdapter;
import com.gaiagps.iburn.database.DataProvider;
import com.squareup.sqlbrite.SqlBrite;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Subscription;
import rx.functions.Action1;

public class SearchActivity extends AppCompatActivity implements AdapterItemSelectedListener {

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

        resultList.setLayoutManager(new LinearLayoutManager(this));
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

        searchSubscription = DataProvider.getInstance(this).observeQuery(query, adapter.getRequiredProjection())
                .subscribe(query1 -> {
                    adapter.changeCursor(query1.run());
                });

    }

    @Override
    public void onItemSelected(int modelId, Constants.PlayaItemType type) {
        // Launch detail activity?
        Intent i = new Intent(this, PlayaItemViewActivity.class);
        i.putExtra("model_id", modelId);
        i.putExtra("model_type", type);
        startActivity(i);
    }

    public void onBackButtonClick(View view) {
        this.finish();
    }
}
