/* 
 * Copyright (C) 2008 Torgny Bjers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xorcode.andtweet;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.xorcode.andtweet.data.AndTweetDatabase;
import com.xorcode.andtweet.data.AndTweetDatabase.DirectMessages;
import com.xorcode.andtweet.data.AndTweetDatabase.Tweets;
import com.xorcode.andtweet.data.AndTweetDatabase.Users;

/**
 * Database provider for the AndTweetDatabase database.
 * 
 * @author torgny.bjers
 */
public class AndTweetProvider extends ContentProvider {

	private static final String TAG = "AndTweetProvider";

	private static final String DATABASE_NAME = "andtweet.db";
	private static final int DATABASE_VERSION = 6;
	private static final String TWEETS_TABLE_NAME = "tweets";
	private static final String USERS_TABLE_NAME = "users";
	private static final String DIRECTMESSAGES_TABLE_NAME = "directmessages";

	private static final UriMatcher sUriMatcher;
	private DatabaseHelper mOpenHelper;

	private static HashMap<String, String> sTweetsProjectionMap;
	private static HashMap<String, String> sUsersProjectionMap;
	private static HashMap<String, String> sDirectMessagesProjectionMap;

	private static final int TWEETS = 1;
	private static final int TWEET_ID = 2;
	private static final int USERS = 3;
	private static final int USER_ID = 4;
	private static final int DIRECTMESSAGES = 5;
	private static final int DIRECTMESSAGE_ID = 6;
	private static final int TWEET_SEARCH = 7;

	/**
	 * Database helper for AndTweetProvider.
	 * 
	 * @author torgny.bjers
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.d(TAG, "Creating tables");
			db.execSQL("CREATE TABLE " + TWEETS_TABLE_NAME + " ("
					+ Tweets._ID + " INTEGER PRIMARY KEY," 
					+ Tweets.AUTHOR_ID + " TEXT," 
					+ Tweets.MESSAGE + " TEXT," 
					+ Tweets.SOURCE + " TEXT,"
					+ Tweets.IN_REPLY_TO_STATUS_ID + " INTEGER,"
					+ Tweets.IN_REPLY_TO_AUTHOR_ID + " TEXT,"
					+ Tweets.SENT_DATE + " INTEGER," 
					+ Tweets.CREATED_DATE + " INTEGER"
					+ ");");

			db.execSQL("CREATE TABLE " + DIRECTMESSAGES_TABLE_NAME + " ("
					+ DirectMessages._ID + " INTEGER PRIMARY KEY," 
					+ DirectMessages.AUTHOR_ID + " TEXT," 
					+ DirectMessages.MESSAGE + " TEXT," 
					+ DirectMessages.SENT_DATE + " INTEGER,"
					+ DirectMessages.CREATED_DATE + " INTEGER"
					+ ");");

			db.execSQL("CREATE TABLE " + USERS_TABLE_NAME + " ("
					+ Users._ID + " INTEGER PRIMARY KEY," 
					+ Users.AUTHOR_ID + " TEXT," 
					+ Users.CREATED_DATE + " INTEGER," 
					+ Users.MODIFIED_DATE + " INTEGER"
					+ ");");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
	}

	/**
	 * @see android.content.ContentProvider#onCreate()
	 */
	@Override
	public boolean onCreate() {
		mOpenHelper = new DatabaseHelper(getContext());
		return (mOpenHelper == null) ? false : true;
	}

	/**
	 * Get type of the Uri to make sure we use the right table
	 * 
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 */
	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
		case TWEETS:
		case TWEET_SEARCH:
			return Tweets.CONTENT_TYPE;

		case TWEET_ID:
			return Tweets.CONTENT_ITEM_TYPE;

		case DIRECTMESSAGES:
			return DirectMessages.CONTENT_TYPE;

		case DIRECTMESSAGE_ID:
			return DirectMessages.CONTENT_ITEM_TYPE;

		case USERS:
			return Users.CONTENT_TYPE;

		case USER_ID:
			return Users.CONTENT_ITEM_TYPE;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	/**
	 * Delete a record from the database.
	 * 
	 * @see android.content.ContentProvider#delete(android.net.Uri,
	 *      java.lang.String, java.lang.String[])
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case TWEETS:
			count = db.delete(TWEETS_TABLE_NAME, selection, selectionArgs);
			break;

		case TWEET_ID:
			String tweetId = uri.getPathSegments().get(1);
			count = db.delete(TWEETS_TABLE_NAME, Tweets._ID + "=" + tweetId
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
					selectionArgs);
			break;

		case DIRECTMESSAGES:
			count = db.delete(DIRECTMESSAGES_TABLE_NAME, selection, selectionArgs);
			break;

		case DIRECTMESSAGE_ID:
			String messageId = uri.getPathSegments().get(1);
			count = db.delete(DIRECTMESSAGES_TABLE_NAME, DirectMessages._ID + "=" + messageId
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
					selectionArgs);
			break;

		case USERS:
			count = db.delete(USERS_TABLE_NAME, selection, selectionArgs);
			break;

		case USER_ID:
			String userId = uri.getPathSegments().get(1);
			count = db.delete(USERS_TABLE_NAME, Users._ID + "=" + userId
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
					selectionArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	/**
	 * Insert a new record into the database.
	 * 
	 * @see android.content.ContentProvider#insert(android.net.Uri,
	 *      android.content.ContentValues)
	 */
	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {

		ContentValues values;
		long rowId;
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"), Locale.US);
		Long now = Long.valueOf(cal.getTimeInMillis());
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		String table;
		String nullColumnHack;
		Uri contentUri;

		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		switch (sUriMatcher.match(uri)) {
		case TWEETS:
			table = TWEETS_TABLE_NAME;
			nullColumnHack = Tweets.MESSAGE;
			contentUri = Tweets.CONTENT_URI;
			if (values.containsKey(Tweets.CREATED_DATE) == false) values.put(Tweets.CREATED_DATE, now);
			if (values.containsKey(Tweets.SENT_DATE) == false) values.put(Tweets.SENT_DATE, now);
			if (values.containsKey(Tweets.AUTHOR_ID) == false) values.put(Tweets.AUTHOR_ID, "");
			if (values.containsKey(Tweets.MESSAGE) == false) values.put(Tweets.MESSAGE, "");
			if (values.containsKey(Tweets.SOURCE) == false) values.put(Tweets.SOURCE, "");
			if (values.containsKey(Tweets.IN_REPLY_TO_AUTHOR_ID) == false) values.put(Tweets.IN_REPLY_TO_AUTHOR_ID, "");
			break;

		case DIRECTMESSAGES:
			table = DIRECTMESSAGES_TABLE_NAME;
			nullColumnHack = DirectMessages.MESSAGE;
			contentUri = DirectMessages.CONTENT_URI;
			if (values.containsKey(DirectMessages.CREATED_DATE) == false) values.put(DirectMessages.CREATED_DATE, now);
			if (values.containsKey(DirectMessages.SENT_DATE) == false) values.put(DirectMessages.SENT_DATE, now);
			if (values.containsKey(DirectMessages.AUTHOR_ID) == false) values.put(DirectMessages.AUTHOR_ID, "");
			if (values.containsKey(DirectMessages.MESSAGE) == false) values.put(DirectMessages.MESSAGE, "");
			break;

		case USERS:
			table = USERS_TABLE_NAME;
			nullColumnHack = Users.AUTHOR_ID;
			contentUri = Users.CONTENT_URI;
			if (values.containsKey(Users.MODIFIED_DATE) == false) values.put(Users.MODIFIED_DATE, now);
			if (values.containsKey(Users.CREATED_DATE) == false) values.put(Users.CREATED_DATE, now);
			if (values.containsKey(Users.AUTHOR_ID) == false) values.put(Users.AUTHOR_ID, "");
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		rowId = db.insert(table, nullColumnHack, values);
		if (rowId > 0) {
			Uri newUri = ContentUris.withAppendedId(contentUri, rowId);
			return newUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	/**
	 * Get a cursor to the database
	 * 
	 * @see android.content.ContentProvider#query(android.net.Uri,
	 *      java.lang.String[], java.lang.String, java.lang.String[],
	 *      java.lang.String)
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		switch (sUriMatcher.match(uri)) {
		case TWEETS:
			qb.setTables(TWEETS_TABLE_NAME);
			qb.setProjectionMap(sTweetsProjectionMap);
			break;

		case TWEET_ID:
			qb.setTables(TWEETS_TABLE_NAME);
			qb.setProjectionMap(sTweetsProjectionMap);
			qb.appendWhere(Tweets._ID + "=" + uri.getPathSegments().get(1));
			break;

		case TWEET_SEARCH:
			qb.setTables(TWEETS_TABLE_NAME);
			qb.setProjectionMap(sTweetsProjectionMap);
			qb.appendWhere(Tweets.AUTHOR_ID + " LIKE ? OR " + Tweets.MESSAGE + " LIKE ?");
			selectionArgs = new String[2];
			selectionArgs[0] = selectionArgs[1] = "%" + uri.getLastPathSegment() + "%";
			break;

		case DIRECTMESSAGES:
			qb.setTables(DIRECTMESSAGES_TABLE_NAME);
			qb.setProjectionMap(sDirectMessagesProjectionMap);
			break;

		case DIRECTMESSAGE_ID:
			qb.setTables(DIRECTMESSAGES_TABLE_NAME);
			qb.setProjectionMap(sDirectMessagesProjectionMap);
			qb.appendWhere(DirectMessages._ID + "=" + uri.getPathSegments().get(1));
			break;

		case USERS:
			qb.setTables(USERS_TABLE_NAME);
			qb.setProjectionMap(sUsersProjectionMap);
			break;

		case USER_ID:
			qb.setTables(USERS_TABLE_NAME);
			qb.setProjectionMap(sUsersProjectionMap);
			qb.appendWhere(Users._ID + "=" + uri.getPathSegments().get(1));
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// If no sort order is specified use the default
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			switch (sUriMatcher.match(uri)) {
			case TWEETS:
			case TWEET_ID:
				orderBy = Tweets.DEFAULT_SORT_ORDER;
				break;

			case DIRECTMESSAGES:
			case DIRECTMESSAGE_ID:
				orderBy = DirectMessages.DEFAULT_SORT_ORDER;
				break;

			case USERS:
			case USER_ID:
				orderBy = Users.DEFAULT_SORT_ORDER;
				break;

			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
			}
		} else {
			orderBy = sortOrder;
		}

		// Get the database and run the query
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

		// Tell the cursor what Uri to watch, so it knows when its source data changes
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	/**
	 * Update a record in the database
	 * 
	 * @see android.content.ContentProvider#update(android.net.Uri,
	 *      android.content.ContentValues, java.lang.String, java.lang.String[])
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case TWEETS:
			count = db.update(TWEETS_TABLE_NAME, values, selection, selectionArgs);
			break;

		case TWEET_ID:
			String noteId = uri.getPathSegments().get(1);
			count = db.update(TWEETS_TABLE_NAME, values, Tweets._ID + "=" + noteId
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
					selectionArgs);
			break;

		case DIRECTMESSAGES:
			count = db.update(DIRECTMESSAGES_TABLE_NAME, values, selection, selectionArgs);
			break;

		case DIRECTMESSAGE_ID:
			String messageId = uri.getPathSegments().get(1);
			count = db.update(DIRECTMESSAGES_TABLE_NAME, values,
					DirectMessages._ID + "=" + messageId
							+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
					selectionArgs);
			break;

		case USERS:
			count = db.update(USERS_TABLE_NAME, values, selection, selectionArgs);
			break;

		case USER_ID:
			String userId = uri.getPathSegments().get(1);
			count = db.update(USERS_TABLE_NAME, values, Users._ID + "=" + userId
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
					selectionArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		return count;
	}

	// Static Definitions for UriMatcher and Projection Maps
	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

		sUriMatcher.addURI(AndTweetDatabase.AUTHORITY, TWEETS_TABLE_NAME, TWEETS);
		sUriMatcher.addURI(AndTweetDatabase.AUTHORITY, TWEETS_TABLE_NAME + "/#", TWEET_ID);
		sUriMatcher.addURI(AndTweetDatabase.AUTHORITY, TWEETS_TABLE_NAME + "/search/*", TWEET_SEARCH);

		sUriMatcher.addURI(AndTweetDatabase.AUTHORITY, USERS_TABLE_NAME, USERS);
		sUriMatcher.addURI(AndTweetDatabase.AUTHORITY, USERS_TABLE_NAME + "/#", USER_ID);

		sUriMatcher.addURI(AndTweetDatabase.AUTHORITY, DIRECTMESSAGES_TABLE_NAME, DIRECTMESSAGES);
		sUriMatcher.addURI(AndTweetDatabase.AUTHORITY, DIRECTMESSAGES_TABLE_NAME + "/#", DIRECTMESSAGE_ID);

		sTweetsProjectionMap = new HashMap<String, String>();
		sTweetsProjectionMap.put(Tweets._ID, Tweets._ID);
		sTweetsProjectionMap.put(Tweets.AUTHOR_ID, Tweets.AUTHOR_ID);
		sTweetsProjectionMap.put(Tweets.MESSAGE, Tweets.MESSAGE);
		sTweetsProjectionMap.put(Tweets.SOURCE, Tweets.SOURCE);
		sTweetsProjectionMap.put(Tweets.IN_REPLY_TO_STATUS_ID, Tweets.IN_REPLY_TO_STATUS_ID);
		sTweetsProjectionMap.put(Tweets.IN_REPLY_TO_AUTHOR_ID, Tweets.IN_REPLY_TO_AUTHOR_ID);
		sTweetsProjectionMap.put(Tweets.SENT_DATE, Tweets.SENT_DATE);
		sTweetsProjectionMap.put(Tweets.CREATED_DATE, Tweets.CREATED_DATE);

		sDirectMessagesProjectionMap = new HashMap<String, String>();
		sDirectMessagesProjectionMap.put(DirectMessages._ID, DirectMessages._ID);
		sDirectMessagesProjectionMap.put(DirectMessages.AUTHOR_ID, DirectMessages.AUTHOR_ID);
		sDirectMessagesProjectionMap.put(DirectMessages.MESSAGE, DirectMessages.MESSAGE);
		sDirectMessagesProjectionMap.put(DirectMessages.SENT_DATE, DirectMessages.SENT_DATE);
		sDirectMessagesProjectionMap.put(DirectMessages.CREATED_DATE, DirectMessages.CREATED_DATE);

		sUsersProjectionMap = new HashMap<String, String>();
		sUsersProjectionMap.put(Users._ID, Users._ID);
		sUsersProjectionMap.put(Users.AUTHOR_ID, Users.AUTHOR_ID);
		sUsersProjectionMap.put(Users.CREATED_DATE, Users.CREATED_DATE);
		sUsersProjectionMap.put(Users.MODIFIED_DATE, Users.MODIFIED_DATE);
	}
}
