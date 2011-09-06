package com.tortel.bt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

public class DB {
	private SQLiteDatabase db;
	private final Context context;
	private final DBHelper dbhelper;
	
	/**
	 * Constructor
	 * @param c Context
	 */
	public DB(Context c){
		context = c;
		dbhelper = new DBHelper(context, DBConstants.DATABASE_NAME, null, DBConstants.DATABASE_VERSION);
	}
	
	/**
	 * Close the database
	 */
	public synchronized void close(){
		db.close();
	}
	
	/**
	 * Opens the database connection
	 * @throws SQLiteException oh shit!
	 */
	public synchronized void open() throws SQLiteException {
		try{
			db = dbhelper.getWritableDatabase();
		}catch(SQLiteException ex)
		{
			Log.v("Open exception caught"+ex.getMessage());
			db = dbhelper.getReadableDatabase();
		}
	}
	
	/**
	 * Deletes a contact from the database with the given name.
	 * If nto found, no action is taken
	 * @param name the contact name
	 */
	public synchronized void deleteThread(int threadId){
		db.delete(DBConstants.THREADS, DBConstants.THREAD_ID+"=\""+threadId+"\"", null);
		db.delete(DBConstants.MESSAGES, DBConstants.MESSAGE_THREAD+"=\""+threadId+"\"", null);
	}
	
	/**
	 * Clears the database
	 */
	public void clear(){
		db.delete(DBConstants.THREADS, null, null);
		db.delete(DBConstants.MESSAGES, null, null);
	}
	
	/**
	 * Inserts a contact into the database
	 * @param name contact name
	 * @param number contact number
	 * @param icon contact icon
	 * @return -1 if bad things happen
	 */
	public synchronized long insertMessage(String message, String phoneNumber, long time, int type, int trim){
		if(message == null || message.equals("") ||
				phoneNumber == null || phoneNumber.equals(""))
			return -1;
		try{
			//phone numbers are only 10 digits
			if(phoneNumber.length() > 10)
				phoneNumber = Utils.parseNumber(phoneNumber);
			int id = getThreadId(phoneNumber);
			ContentValues newMessage = new ContentValues();
			newMessage.put(DBConstants.MESSAGE_BODY, message);
			newMessage.put(DBConstants.MESSAGE_THREAD, id);
			newMessage.put(DBConstants.MESSAGE_TIME, time);
			newMessage.put(DBConstants.MESSAGE_TYPE, type);
			if(type == TextMessage.SENT)
				Log.v(markRead(id) + " rows marked as read");
			else
				Log.v(markUnread(id) + " rows marked as unread.");
			if(trim > 0)
				trimThread(id,trim);
			
			return db.insert(DBConstants.MESSAGES, null, newMessage);
		}catch(SQLiteException e)
		{
			Log.e("open exception caught",e);
			return -1;
		}	
	}
	
	public synchronized int trimThread(int threadId, int max){
		Cursor c = getMessagesByThread(threadId);
		if(c.getCount() > max){
			Log.v("Trimming conversation "+threadId);
			c.moveToFirst();
			String values[] = { c.getInt(c.getColumnIndex(DBConstants.MESSAGE_ID)) +"" };
			return db.delete(DBConstants.MESSAGES, DBConstants.MESSAGE_ID+" =?", values);
		}
		return 0;
	}
	
	public synchronized int trimThread(String number, int max){
		int threadId = getThreadId(number);
		Cursor c = getMessagesByThread(threadId);
		if(c.getCount() > max){
			Log.v("Trimming conversation "+threadId);
			c.moveToFirst();
			String values[] = { c.getInt(c.getColumnIndex(DBConstants.MESSAGE_ID)) +"" };
			return db.delete(DBConstants.MESSAGES, DBConstants.MESSAGE_ID+" =?", values);
		}
		return 0;
	}
	
	/**
	 * Marks a thread as having no unread messages
	 * @param threadId the thread id to mark read
	 * @return number of rows changed
	 */
	public synchronized int markRead(int threadId){
		//Mark sender as having unread messages
		ContentValues markUnread = new ContentValues();
		markUnread.put(DBConstants.THREAD_UNREAD,0);
		String where = DBConstants.THREAD_ID+"=?";
		String values[] = { threadId+"" };
		//Mark sender as unread
		return db.update(DBConstants.THREADS, markUnread, where, values);
	}
	
	/**
	 * Marks a thread as having unread messages
	 * @param threadId the thread id to mark unread
	 * @return number of rows changed
	 */
	public synchronized int markUnread(int threadId){
		//Mark sender as having unread messages
		ContentValues markUnread = new ContentValues();
		markUnread.put(DBConstants.THREAD_UNREAD,1);
		String where = DBConstants.THREAD_ID+"=?";
		String values[] = { threadId+"" };
		//Mark sender as unread
		return db.update(DBConstants.THREADS, markUnread, where, values);
	}
	
	/**
	 * Gets a contact from the database. If the contact is found, the threadId
	 * returned is -1
	 * @param number the number to lookup
	 * @return Contact object if found, null if not
	 */
	public synchronized int getThreadId(String number){
		Log.v("getThreadID for "+number);
		int tmp = tryThreadId(number);
		if(tmp != -1){
			Log.v("Got threadId "+tmp);
			return tmp;
		} else {
			Log.v("Creating threadId");
			//Not in the database, need to make one.
			ContentValues newThread = new ContentValues();
			newThread.put(DBConstants.THREAD_PHONE, number);
			//Lookup key
			newThread.put(DBConstants.THREAD_KEY, Utils.getContactKey(number, context));
			//Save it
			tmp =(int) db.insert(DBConstants.THREADS, null, newThread);
			Log.v("Created id "+tmp);
			return tmp;
		}
	}
	
	private synchronized int tryThreadId(String number){
		if(number == null || number.equals(""))
			return -1;
		try {
			Cursor c = db.query(DBConstants.THREADS,
					new String[] { DBConstants.THREAD_ID },
					DBConstants.THREAD_PHONE + " = '" + number + "'", null, null,
					null, null);
			if (c.moveToNext()) {
				int temp = c.getInt(0);
				Log.v("Looked up thread id for " + number + ", got " + temp);
				c.close();
				return temp;
			}
			c.close();
		} catch (Exception e) {
			Log.v("Exception cought while gettting icon: "+e);
		}
		return -1;
	}
	
	/**
	 * Returns a cursor for all contacts
	 * @return memos
	 */
	public synchronized Cursor getThreads(){
		Cursor c =  db.query(DBConstants.THREADS, null, null, null, null, null, null);
		return c;
	}
	
	/**
	 * Returns the phone number for a given thread
	 * @param threadId thread ID
	 * @return phone number
	 */
	public synchronized String getPhoneNumber(int threadId){
		Cursor c = db.query(DBConstants.THREADS, new String[] {DBConstants.THREAD_PHONE },
				DBConstants.THREAD_ID+" = "+threadId, null, null, null, null);
		if(!c.moveToNext())
			return null;
		return c.getString(0);
	}
	
	/**
	 * Returns all the messages in the given thread, or null if the thread is not found.
	 * @param thread thread to look up
	 * @return messages or null
	 */
	public synchronized Cursor getMessagesByThread(int thread){
		Cursor c = db.query(DBConstants.MESSAGES, null, 
				DBConstants.MESSAGE_THREAD+" = "+thread, null, null, null,
				DBConstants.MESSAGE_TIME+" ASC");
		return c;
	}
}
