package com.gaiagps.iburn.adapters;

import android.database.MatrixCursor;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SmallTest
public class EventSectionedCursorAdapterTest extends AndroidTestCase {

    @Test
    public void testSomething() {

        EventSectionedCursorAdapter adapter = new EventSectionedCursorAdapter(getContext(), prepareMockCursor(), false, (modelId, type) -> {
            // ignore clicks
        });

        List<Integer> expectedHeaderPositions = new ArrayList<Integer>() {{ add(0); add(4); add(6); }};

        assertEquals(adapter.headerPositions, expectedHeaderPositions);

        assertEquals(adapter.getItemCount(), 8);

        assertTrue(adapter.isHeaderPosition(0));
        assertTrue(adapter.isHeaderPosition(4));
        assertTrue(adapter.isHeaderPosition(6));

        assertFalse(adapter.isHeaderPosition(1));
        assertFalse(adapter.isHeaderPosition(2));
        assertFalse(adapter.isHeaderPosition(3));
        assertFalse(adapter.isHeaderPosition(5));
        assertFalse(adapter.isHeaderPosition(7));

        // Items at position 1-3 are under the first header at position 0
        // Note getHeaderPositionForPosition operates only on non-header positions
        assertEquals(adapter.getHeaderPositionForPosition(1), 0);
        assertEquals(adapter.getHeaderPositionForPosition(2), 0);
        assertEquals(adapter.getHeaderPositionForPosition(3), 0);

        assertEquals(adapter.getHeaderPositionForPosition(5), 4);
        assertEquals(adapter.getHeaderPositionForPosition(7), 6);

        // Note getCursorPositionForPosition operates only on non-header positions
        assertEquals(adapter.getCursorPositionForPosition(1), 0);
        assertEquals(adapter.getCursorPositionForPosition(2), 1);
        assertEquals(adapter.getCursorPositionForPosition(3), 2);
        assertEquals(adapter.getCursorPositionForPosition(5), 3);
        assertEquals(adapter.getCursorPositionForPosition(7), 4);

        Set<Long> itemIds = new HashSet<>(adapter.getItemCount());
        for (int idx = 0; idx < adapter.getItemCount(); idx++) {
            if (!itemIds.add(adapter.getItemId(idx))) throw new AssertionError("Duplicate id " + adapter.getItemId(idx) + " at position " + idx + " previous: " + itemIds);
        }
    }

    private MatrixCursor prepareMockCursor() {

        MatrixCursor matrixCursor = new MatrixCursor(EventSectionedCursorAdapter.Projection);

        // 21:00:00 Header
        matrixCursor.addRow(new String[] {"1", "Test Event", "2015-09-01 21:00:00", "Mon 9/1 9:00 PM", "2015-09-01 23:00:00", "Mon 9/1 11:00 PM","0", "0", "0", "0", "cere"});
        matrixCursor.addRow(new String[] {"2", "Test Event2", "2015-09-01 21:00:00", "Mon 9/1 9:00 PM", "2015-09-01 22:30:00", "Mon 9/1 10:30 PM","0", "0", "0", "0", "cere"});
        matrixCursor.addRow(new String[] {"3", "Test Event3", "2015-09-01 21:00:00", "Mon 9/1 9:00 PM", "2015-09-01 23:30:00", "Mon 9/1 11:30 PM","0", "0", "0", "0", "cere"});
        // 23:30 Header
        matrixCursor.addRow(new String[] {"4", "Test Event4", "2015-09-01 23:30:00", "Mon 9/1 11:30 PM", "2015-09-01 23:45:00", "Mon 9/1 11:45 PM","0", "0", "0", "0", "cere"});
        // 0:00 Header
        matrixCursor.addRow(new String[] {"5", "Test Event5", "2015-09-02 0:00:00", "Tues 9/2 12:00 AM", "2015-09-02 01:00:00", "Tues 9/2 1:00 AM","0", "0", "0", "0", "cere"});

        return matrixCursor;
    }
}