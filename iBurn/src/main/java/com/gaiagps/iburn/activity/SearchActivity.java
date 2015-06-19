package com.gaiagps.iburn.activity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.adapters.AdapterItemSelectedListener;
import com.gaiagps.iburn.adapters.PlayaSearchResponseCursorAdapter;
import com.gaiagps.iburn.database.PlayaContentProvider;
import com.gaiagps.iburn.database.PlayaItemTable;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import timber.log.Timber;

public class SearchActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, AdapterItemSelectedListener {

    static final String[] PROJECTION = new String[] {
            PlayaItemTable.id,
            PlayaItemTable.name,
            PlayaItemTable.favorite,
            PlayaItemTable.latitude,
            PlayaItemTable.longitude
    };

    private String query;
    private PlayaSearchResponseCursorAdapter adapter;
    private boolean didInit;

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

//        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//        imm.showSoftInput(searchEntry, InputMethodManager.SHOW_FORCED);
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

        searchEntry.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchEntry.getWindowToken(), 0);
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Dispatch a search query to the current Fragment in the FragmentPagerAdapter
     */
    private void dispatchSearchQuery(String query) {
        this.query = query;
        if (didInit)
            restartLoader();
        else {
            initLoader();
            didInit = true;
        }
    }

    public void restartLoader() {
        Timber.d("restarting loader");
        getSupportLoaderManager().restartLoader(0, null, this);
    }

    public void initLoader() {
        Timber.d("init loader");
        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        adapter.swapCursor(null);
    }

    ArrayList<String> selectionArgs = new ArrayList<>();

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // sample only has one Loader, so we don't care about the ID.
        // First, pick the base URI to use depending on whether we are
        // currently filtering.

        StringBuilder selection = new StringBuilder();
        selectionArgs.clear();

//        if (getShouldLimitSearchToFavorites()) {
//            appendSelection(selection, getFavoriteSelection(), "1");
//        }

        if (!TextUtils.isEmpty(query)) {
            appendSelection(selection, PlayaItemTable.name + " LIKE ?", "%" + query + "%");
        }

//        if (mCurrentSort == PlayaListViewHeader.PlayaListViewHeaderReceiver.SORT.DISTANCE) {
//            appendSelection(selection, PlayaItemTable.latitude + " != ?", "0");
//            appendSelection(selection, PlayaItemTable.longitude + " != ?", "0");
//        }

        addCursorLoaderSelectionArgs(selection, selectionArgs);

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        Timber.d("Creating loader with selection: " + selection.toString() + selectionArgs);
        return new CursorLoader(this,
                PlayaContentProvider.Camps.CAMPS,
                PROJECTION,
                selection.toString(),
                selectionArgs.toArray(new String[selectionArgs.size()]),
                PlayaItemTable.name + " ASC"); // TODO : Sort by distance?
    }

    protected void addCursorLoaderSelectionArgs(StringBuilder selection, ArrayList<String> selectionArgs) {
        // childclasses can add selections here
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        adapter.swapCursor(data);
        if (data == null) {
            Timber.e("cursor is null onLoadFinished");
        }
    }

    @Override
    public void onItemSelected(int modelId, Constants.PlayaItemType type) {
        // Launch detail activity?
        Intent i = new Intent(this, PlayaItemViewActivity.class);
        i.putExtra("model_id", modelId);
        i.putExtra("playa_item", type);
        startActivity(i);
    }

    protected void appendSelection(StringBuilder builder, String selection, String value) {
        if (builder.length() > 0)
            builder.append(" AND ");
        builder.append(selection);
        selectionArgs.add(value);
    }

    public void onBackButtonClick(View view) {
        this.finish();
    }
}
