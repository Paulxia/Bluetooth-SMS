package com.tortel.bt;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

/**
 * Database helper class
 * @author scott
 *
 */
public class DBHelper extends SQLiteOpenHelper {
	
	/**
	 * Long string for create messages table query
	 */
	private static final String CREATE_TABLE_MESSAGES="create table "+DBConstants.TABLE_NAME_MESSAGES+" ("+
	DBConstants.MESSAGE_ID+" integer primary key autoincrement, "+
	DBConstants.MESSAGE_BODY+" text not null, "+
	DBConstants.MESSAGE_THREAD+" int, "+
	DBConstants.MESSAGE_READ+" int, "+
	DBConstants.MESSAGE_TYPE+" int, "+
	DBConstants.MESSAGE_TIME+" long);";
	
	/**
	 * Long string to create thread table
	 */
	private static final String CREATE_TABLE_THREADS="create table "+DBConstants.TABLE_NAME_THREADS+" ("+
	DBConstants.THREAD_ID+" integer primary key autoincrement, "+
	DBConstants.THREAD_PHONE+" text not null, "+
	DBConstants.THREAD_KEY+" text not null, "+
	DBConstants.THREAD_UNREAD+" int, "+
	DBConstants.THREAD_TIME+" long);";
	
	/**
	 * Constructor
	 * @param context
	 * @param name
	 * @param factory
	 * @param version
	 */
	public DBHelper(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
	}

	/**
	 * Create the table
	 */
	public void onCreate(SQLiteDatabase db) {
		Log.v("Creating all the tables");
		try{
			db.execSQL(CREATE_TABLE_MESSAGES);
			db.execSQL(CREATE_TABLE_THREADS);
		}catch(SQLiteException ex)
		{
			Log.e("open exception caught",ex);
			
		}
	}

	/**
	 * Database upgrade. Drop it all, idk what else
	 */
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w("Upgrading from version "+oldVersion +" to "+newVersion+", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS "+DBConstants.TABLE_NAME_MESSAGES);
		db.execSQL("DROP TABLE IF EXISTS "+DBConstants.TABLE_NAME_THREADS);
		onCreate(db);
	}
}
