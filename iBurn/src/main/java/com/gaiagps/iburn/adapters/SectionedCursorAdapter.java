package com.gaiagps.iburn.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.tonicartos.superslim.LayoutManager;
import com.tonicartos.superslim.LinearSLM;

import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public abstract class SectionedCursorAdapter<T extends PlayaItemCursorAdapter.ViewHolder> extends PlayaItemCursorAdapter<T> {

    protected static final int VIEW_TYPE_HEADER = 0x01;

    protected static final int VIEW_TYPE_CONTENT = 0x00;

    List<Integer> headerPositions;

    public SectionedCursorAdapter(Context context, Cursor c, AdapterListener listener) {
        super(context, c, listener);
        initializeWithNewCursor(c);
    }

    @Override
    public void onBindViewHolder(T viewHolder, int position) {
        if (!mDataValid) {
            throw new IllegalStateException("this should only be called when the cursor is valid");
        }
        if (isHeaderPosition(position)) {
            mCursor.moveToPosition(getCursorPositionForPosition(position+1)); // the next element informed this header
            onBindViewHolderHeader(viewHolder, mCursor, position);
            return;
        }
        else if (!mCursor.moveToPosition(getCursorPositionForPosition(position))) {
            Timber.e("couldn't move cursor to position " + position);
            // I've observed a crash report here when we threw an exception. Think it's preferable to
            // just not bind the view under the assumption that this is spurious behavior
            // caused by the recyclerview being fast scroller (Not sure this is possible?) before data bound.
            return;
        }
        onBindViewHolder(viewHolder, mCursor, position);
    }

    protected abstract void onBindViewHolder(T viewHolder, Cursor item, int position);

    protected abstract void onBindViewHolderHeader(T viewHolder, Cursor firstSectionItem, int position);

    /**
     * Convenience method for setting Linear SLM layout properties on a ViewHolder view.
     * This should be called for each call to {@link #onBindViewHolder(RecyclerView.ViewHolder, Cursor, int)} and
     * {@link #onBindViewHolderHeader(RecyclerView.ViewHolder, Cursor, int)}
     *
     * Use this method if you don't require anything other than standard linear section managers.
     * Override or set properties manually if you require other behavior
     */
    protected void setLinearSlmParameters(T viewHolder, int position) {

        final LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) viewHolder.itemView.getLayoutParams();
        params.setSlm(LinearSLM.ID);
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.setFirstPosition(getHeaderPositionForPosition(position));

        viewHolder.itemView.setLayoutParams(params);
    }

    @Override
    public abstract String[] getRequiredProjection();

    @Override
    public int getItemViewType(int position) {
        // TODO : Get header positions from query. count(s_date < +30m), count(s_date < +2hr)
        return isHeaderPosition(position) ? VIEW_TYPE_HEADER : VIEW_TYPE_CONTENT;
    }

    @Override
    public int getItemCount() {
        int superCount = super.getItemCount();
        if (superCount != 0)
            return superCount + (headerPositions == null ? 0 : headerPositions.size());
        return superCount;
    }

    @Override
    public long getItemId(int position) {
        if (isHeaderPosition(position)) {
            return getHeaderId(position);
        }
        return super.getItemId(getCursorPositionForPosition(position));
    }

    public void changeCursor(Cursor cursor) {
        Observable.just(cursor)
                .subscribeOn(Schedulers.computation())
                .map(this::initializeWithNewCursor)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> {
                    super.changeCursor(cursor);
                });
    }

    public Cursor swapCursor(Cursor newCursor) {
        initializeWithNewCursor(newCursor);

        return super.swapCursor(newCursor);
    }

    boolean isHeaderPosition(int position) {
        return headerPositions != null && headerPositions.contains(position);
    }

    private long getHeaderId(int position) {
        // return something unlikely to conflict with database ids
        return Long.MAX_VALUE - headerPositions.indexOf(position);
    }

    private boolean initializeWithNewCursor(Cursor newCursor) {
        if (newCursor != null && newCursor.getCount() > 0) {
            _createHeadersForCursor(newCursor);
        }
        return true;
    }

    protected abstract List<Integer> createHeadersForCursor(Cursor cursor);

    private void _createHeadersForCursor(Cursor cursor) {
        cursor.moveToFirst();
        headerPositions = createHeadersForCursor(cursor);
        cursor.moveToFirst();
    }

    /**
     * @return the position of the header for the corresponding item position.
     * The value will be less than or equal to position.
     */
    int getHeaderPositionForPosition(int position) {
        // TODO : Do a binary search? IF -1 return last header index?
        int headerIdx = getHeaderIndexForPosition(position);
        return headerIdx == -1 ? position : headerPositions.get(headerIdx);
    }

    /**
     * @return the index of the header for the current position, or -1 if none found
     */
    int getHeaderIndexForPosition(int position) {
        // TODO : Do a binary search?
        for (int idx = headerPositions.size() - 1; idx >= 0; idx--) {
            if (headerPositions.get(idx) <= position)
                return idx;
        }
        return -1;
    }

    /**
     * @return the cursor position for the corresponding item position. Compensate for the presence of headers
     * e.g: Position 1 is cursor position 0, because position 0 is always the first header
     */
    int getCursorPositionForPosition(int position) {
        return position - (getHeaderIndexForPosition(position) + 1);
    }
}
