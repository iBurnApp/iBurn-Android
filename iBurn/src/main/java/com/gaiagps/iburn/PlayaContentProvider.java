package com.gaiagps.iburn;

import java.util.Arrays;
import java.util.HashSet;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import com.gaiagps.iburn.database.ArtTable;
import com.gaiagps.iburn.database.CampTable;
import com.gaiagps.iburn.database.DBWrapper;
import com.gaiagps.iburn.database.EventTable;

public class PlayaContentProvider extends ContentProvider {

		// database
		private DBWrapper database;

		// Used for the UriMacher
		private static final int CAMPS = 1; 		// Query all
		private static final int CAMP_ID = 2; 		// Query single entry by id
		private static final int CAMP_SEARCH = 3; 	// Search title by string
		private static final int EVENTS = 4;
		private static final int EVENT_ID = 5;
		private static final int EVENT_SEARCH = 9;
		private static final int ART = 7;
		private static final int ART_ID = 8;
		private static final int ART_SEARCH = 10;
        private static final int ALL = 11;

		private static final String AUTHORITY = "com.gaiagps.iburn.playacontentprovider";

		private static final String CAMP_BASE_PATH = "camp";
		private static final String EVENT_BASE_PATH = "event";
		private static final String ART_BASE_PATH = "art";
        private static final String ALL_BASE_PATH = "all";
		
		public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY + "/");
		
		public static final Uri CAMP_URI = AUTHORITY_URI.buildUpon().appendPath(CAMP_BASE_PATH).build();
		public static final Uri CAMP_SEARCH_URI = AUTHORITY_URI.buildUpon().appendPath(CAMP_BASE_PATH).appendPath("search").build();
		
		public static final Uri EVENT_URI = AUTHORITY_URI.buildUpon().appendPath(EVENT_BASE_PATH).build();
		public static final Uri EVENT_SEARCH_URI = AUTHORITY_URI.buildUpon().appendPath(EVENT_BASE_PATH).appendPath("search").build();
		
		public static final Uri ART_URI = AUTHORITY_URI.buildUpon().appendPath(ART_BASE_PATH).build();
		public static final Uri ART_SEARCH_URI = AUTHORITY_URI.buildUpon().appendPath(ART_BASE_PATH).appendPath("search").build();

        public static final Uri ALL_URI = AUTHORITY_URI.buildUpon().appendPath(ALL_BASE_PATH).build();


		private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		static {
			sURIMatcher.addURI(AUTHORITY, CAMP_BASE_PATH, CAMPS);
			sURIMatcher.addURI(AUTHORITY, CAMP_BASE_PATH + "/#", CAMP_ID);
			sURIMatcher.addURI(AUTHORITY, CAMP_BASE_PATH + "/search/*", CAMP_SEARCH);
			
			sURIMatcher.addURI(AUTHORITY, EVENT_BASE_PATH, EVENTS);
			sURIMatcher.addURI(AUTHORITY, EVENT_BASE_PATH + "/#", EVENT_ID);
			sURIMatcher.addURI(AUTHORITY, EVENT_BASE_PATH + "/search/*", EVENT_SEARCH);
			
			sURIMatcher.addURI(AUTHORITY, ART_BASE_PATH, ART);
			sURIMatcher.addURI(AUTHORITY, ART_BASE_PATH + "/#", ART_ID);
			sURIMatcher.addURI(AUTHORITY, ART_BASE_PATH + "/search/*", ART_SEARCH);

            sURIMatcher.addURI(AUTHORITY, ALL_BASE_PATH, ALL);
		}

		@Override
		public boolean onCreate() {
			database = new DBWrapper(getContext());
			return false;
		}
		

		@Override
		public Cursor query(Uri uri, String[] projection, String selection,
				String[] selectionArgs, String sortOrder) {
			
			SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

			int uriType = sURIMatcher.match(uri);
			switch (uriType) {
			case CAMPS:
				queryBuilder.setTables(CampTable.TABLE_NAME);
				checkColumns(projection, CAMPS);
				break;
			case CAMP_ID:
				queryBuilder.setTables(CampTable.TABLE_NAME);
				checkColumns(projection, CAMPS);
				queryBuilder.appendWhere(CampTable.COLUMN_ID + "="
						+ uri.getLastPathSegment());
				break;
			case CAMP_SEARCH:
				queryBuilder.setTables(CampTable.TABLE_NAME);
				checkColumns(projection, CAMPS);
				queryBuilder.appendWhere(CampTable.COLUMN_NAME + " LIKE "
						+ "\"%" + uri.getLastPathSegment()+"%\"");
				break;
			case EVENTS:
				queryBuilder.setTables(EventTable.TABLE_NAME);
				checkColumns(projection, EVENTS);
				break;
			case EVENT_ID:
				queryBuilder.setTables(EventTable.TABLE_NAME);
				checkColumns(projection, EVENTS);
				queryBuilder.appendWhere(EventTable.COLUMN_ID + "="
						+ uri.getLastPathSegment());
				break;
			case EVENT_SEARCH:
				queryBuilder.setTables(EventTable.TABLE_NAME);
				checkColumns(projection, EVENTS);
				queryBuilder.appendWhere(EventTable.COLUMN_NAME + " LIKE "
						+ "\"%" + uri.getLastPathSegment()+"%\"");
				break;
			case ART:
				queryBuilder.setTables(ArtTable.TABLE_NAME);
				checkColumns(projection, ART);
				break;
			case ART_ID:
				queryBuilder.setTables(ArtTable.TABLE_NAME);
				checkColumns(projection, ART);
				queryBuilder.appendWhere(ArtTable.COLUMN_ID + "="
						+ uri.getLastPathSegment());
				break;
			case ART_SEARCH:
				queryBuilder.setTables(ArtTable.TABLE_NAME);
				checkColumns(projection, ART);
				queryBuilder.appendWhere(ArtTable.COLUMN_NAME + " LIKE "
						+ "\"%" + uri.getLastPathSegment()+"%\"");
				break;
            case ALL:
                queryBuilder.setTables(ArtTable.TABLE_NAME + ", " + CampTable.TABLE_NAME + ", " + EventTable.TABLE_NAME);
                //queryBuilder.appendWhere("(art.latitude != 0 AND art.longitude != 0) OR (camps.latitude != 0 AND camps.longitude != 0) OR (events.latitude != 0 AND events.longitude != 0)");
                checkColumns(projection, ALL);
                break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
			}

			SQLiteDatabase db = database.getWritableDatabase();
			Cursor cursor = queryBuilder.query(db, projection, selection,
					selectionArgs, null, null, sortOrder);
			// Make sure that potential listeners are getting notified
			cursor.setNotificationUri(getContext().getContentResolver(), uri);
			return cursor;
		}

		@Override
		public String getType(Uri uri) {
			return null;
		}

		@Override
		public Uri insert(Uri uri, ContentValues values) {
			int uriType = sURIMatcher.match(uri);
			SQLiteDatabase sqlDB = database.getWritableDatabase();
			long id = 0;
			Uri result = null;
			switch (uriType) {
			case CAMPS:
				id = sqlDB.insert(CampTable.TABLE_NAME, null, values);
				getContext().getContentResolver().notifyChange(uri, null);
				result = Uri.parse(CAMP_BASE_PATH + "/" + id);
				break;
			case EVENTS:
				id = sqlDB.insert(EventTable.TABLE_NAME, null, values);
				getContext().getContentResolver().notifyChange(uri, null);
				result = Uri.parse(EVENT_BASE_PATH + "/" + id);
				break;
			case ART:
				id = sqlDB.insert(ArtTable.TABLE_NAME, null, values);
				getContext().getContentResolver().notifyChange(uri, null);
				result = Uri.parse(ART_BASE_PATH + "/" + id);
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
			}
			
			return result;
			
		}

		@Override
		public int delete(Uri uri, String selection, String[] selectionArgs) {
			int uriType = sURIMatcher.match(uri);
			SQLiteDatabase sqlDB = database.getWritableDatabase();
			int rowsDeleted = 0;
			String id;
			switch (uriType) {
			case CAMPS:
				rowsDeleted = sqlDB.delete(CampTable.TABLE_NAME, selection,
						selectionArgs);
				break;
			case CAMP_ID:
				id = uri.getLastPathSegment();
				if (TextUtils.isEmpty(selection)) {
					rowsDeleted = sqlDB.delete(CampTable.TABLE_NAME,
							CampTable.COLUMN_ID + "=" + id, 
							null);
				} else {
					rowsDeleted = sqlDB.delete(CampTable.TABLE_NAME,
							CampTable.COLUMN_ID + "=" + id 
							+ " and " + selection,
							selectionArgs);
				}
				break;
			case EVENTS:
				rowsDeleted = sqlDB.delete(EventTable.TABLE_NAME, selection,
						selectionArgs);
				break;
			case EVENT_ID:
				id = uri.getLastPathSegment();
				if (TextUtils.isEmpty(selection)) {
					rowsDeleted = sqlDB.delete(EventTable.TABLE_NAME,
							CampTable.COLUMN_ID + "=" + id, 
							null);
				} else {
					rowsDeleted = sqlDB.delete(EventTable.TABLE_NAME,
							CampTable.COLUMN_ID + "=" + id 
							+ " and " + selection,
							selectionArgs);
				}
				break;
			case ART:
				rowsDeleted = sqlDB.delete(ArtTable.TABLE_NAME, selection,
						selectionArgs);
				break;
			case ART_ID:
				id = uri.getLastPathSegment();
				if (TextUtils.isEmpty(selection)) {
					rowsDeleted = sqlDB.delete(ArtTable.TABLE_NAME,
							ArtTable.COLUMN_ID + "=" + id, 
							null);
				} else {
					rowsDeleted = sqlDB.delete(ArtTable.TABLE_NAME,
							ArtTable.COLUMN_ID + "=" + id 
							+ " and " + selection,
							selectionArgs);
				}
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
			}
			getContext().getContentResolver().notifyChange(uri, null);
			return rowsDeleted;
		}

		@Override
		public int update(Uri uri, ContentValues values, String selection,
				String[] selectionArgs) {

			int uriType = sURIMatcher.match(uri);
			SQLiteDatabase sqlDB = database.getWritableDatabase();
			int rowsUpdated = 0;
			String id;
			switch (uriType) {
			case CAMPS:
				rowsUpdated = sqlDB.update(CampTable.TABLE_NAME, 
						values, 
						selection,
						selectionArgs);
				break;
			case CAMP_ID:
				id = uri.getLastPathSegment();
				if (TextUtils.isEmpty(selection)) {
					rowsUpdated = sqlDB.update(CampTable.TABLE_NAME, 
							values,
							CampTable.COLUMN_ID + "=" + id, 
							null);
				} else {
					rowsUpdated = sqlDB.update(CampTable.TABLE_NAME, 
							values,
							CampTable.COLUMN_ID + "=" + id 
							+ " and " 
							+ selection,
							selectionArgs);
				}
				break;
			case EVENTS:
				rowsUpdated = sqlDB.update(EventTable.TABLE_NAME, 
						values, 
						selection,
						selectionArgs);
				break;
			case EVENT_ID:
				id = uri.getLastPathSegment();
				if (TextUtils.isEmpty(selection)) {
					rowsUpdated = sqlDB.update(EventTable.TABLE_NAME, 
							values,
							EventTable.COLUMN_ID + "=" + id, 
							null);
				} else {
					rowsUpdated = sqlDB.update(EventTable.TABLE_NAME, 
							values,
							EventTable.COLUMN_ID + "=" + id 
							+ " and " 
							+ selection,
							selectionArgs);
				}
				break;
			case ART:
				rowsUpdated = sqlDB.update(ArtTable.TABLE_NAME, 
						values, 
						selection,
						selectionArgs);
				break;
			case ART_ID:
				id = uri.getLastPathSegment();
				if (TextUtils.isEmpty(selection)) {
					rowsUpdated = sqlDB.update(ArtTable.TABLE_NAME, 
							values,
							ArtTable.COLUMN_ID + "=" + id, 
							null);
				} else {
					rowsUpdated = sqlDB.update(ArtTable.TABLE_NAME, 
							values,
							ArtTable.COLUMN_ID + "=" + id 
							+ " and " 
							+ selection,
							selectionArgs);
				}
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
			}
			getContext().getContentResolver().notifyChange(uri, null);
			return rowsUpdated;
		}

		private void checkColumns(String[] projection, int type) {
			String[] available;
			
			if(type == CAMPS)
				available = CampTable.COLUMNS;
			else if(type == EVENTS)
				available = EventTable.COLUMNS;
			else if(type == ART)
				available = ArtTable.COLUMNS;
            else if(type == ALL)
                return; //TODO
			else
				available = new String[0];
			
			if (projection != null) {
				HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
				HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
				// Check if all columns which are requested are available
				if (!availableColumns.containsAll(requestedColumns)) {
					throw new IllegalArgumentException("Unknown columns in projection");
				}
			}
		}

}
