package com.tortel.bt;

import java.util.ArrayList;
import java.util.TimeZone;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.PhoneNumberUtils;
//TODO: Log shit.
import android.telephony.SmsManager;

/**
 * @author Scott Warner
 */
public class Utils {
	/**
	 * Useful consts
	 */
	public static final Uri SMS_CONTENT_URI = Uri.parse("content://sms");
	public static final Uri SMS_INBOX_CONTENT_URI = Uri.withAppendedPath(SMS_CONTENT_URI, "inbox");
	public static final String SMS_MIME_TYPE = "vnd.android-dir/mms-sms";
	
	public static final Uri MMS_CONTENT_URI = Uri.parse("content://mms");
	public static final Uri MMS_INBOX_CONTENT_URI = Uri.withAppendedPath(MMS_CONTENT_URI, "inbox");
	
	//private static final String UNREAD_CONDITION = "read=0";
	
	/**
	 * Bluetooth static data
	 */
	public static final java.util.UUID UUID = java.util.UUID.fromString("23e8e005-979d-407b-b992-d357cd15f696"); //My UUID
	//public static final java.util.UUID UUID = java.util.UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66"); //Bluetooh Chat UUID
	public static final String BTNAME = "BluetoothSms";
	
	/**
	 * Looks up a contact in the phones contact database
	 * @param number the phone number
	 * @param context context
	 * @return a contact object with a contact, or properly formatted phone number
	 */
	public static Contact lookupContactByNumber(String number, Context context){
		ContentResolver resolver = context.getContentResolver();
		String displayName = null;
		
		
		//Need to look up the number in contacts
		Uri lookupUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
		//contact_id
		Cursor c = resolver.query(lookupUri, new String[] {PhoneLookup.LOOKUP_KEY,PhoneLookup.DISPLAY_NAME}, null, null, null);
		try {
		    c.moveToFirst();
		    displayName = c.getString(1);
		} catch(Exception e){
			displayName = null;
		}finally {
		    c.close();
		}
		
		//Not found, format the number
		if(displayName == null)
			displayName = PhoneNumberUtils.formatNumber(number);
		Contact contact = new Contact(displayName,number);
		Log.v(contact.toString());
		return contact;
	}
	
	/**
	 * Looks up a contact in the phones contact database, and returns the lookup key for that contact
	 * @param number the phone number
	 * @param context context
	 * @return the lookup key for the contact, or null if not found
	 */
	public static String getContactKey(String number, Context context){
		ContentResolver resolver = context.getContentResolver();
		String key = null;
		
		
		//Need to look up the number in contacts
		Uri lookupUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
		//contact_id
		Cursor c = resolver.query(lookupUri, new String[] {PhoneLookup.DISPLAY_NAME}, null, null, null);
		try {
		    c.moveToFirst();
		    key = c.getString(0);
		} catch(Exception e){
			key = null;
		}finally {
		    c.close();
		}
		
		//Return it
		return key;
	}
	
	public static void sendMessage(String message, Context context){
		/**
		 * Message format
		 * PhoneNumber:Message
		 * 
		 */
		message = message.trim();
		Log.v("Sending message: "+message);
		String parts[] =  message.split(":", 2);
		SmsManager sms = SmsManager.getDefault();
		ArrayList<String> tmp = new ArrayList<String>(1);
		tmp.addAll(sms.divideMessage(parts[1]));
		Uri sentBox = Uri.parse("content://sms/sent");
		//From here: http://stackoverflow.com/questions/642076/how-to-save-sms-to-inbox-in-android
		for(String current: tmp){
			//Write the messages into the outbox
			ContentValues values = new ContentValues();
			values.put("address", parts[0]);
			values.put("body",current);
			context.getContentResolver().insert(sentBox, values);
		}
		sms.sendMultipartTextMessage(parts[0], null, tmp, null, null);
	}
	
	/**
	 * Marks a thread as read
	 * @param threadId the thread id to mark read
	 * @param context context
	 */
	public static void markRead(int threadId, Context context){
		DB db = new DB(context);
		db.open();
		db.markRead(threadId);
		db.close();
	}
	
	/**
	 * Returns an ArrayList of TextMessages representing all the messages in a thread
	 * @param threadId
	 * @param cont
	 * @return
	 */
	public static ArrayList<TextMessage> getMessages(int threadId, Context cont){
		if(threadId < 0)
			return new ArrayList<TextMessage>();
		DB db = new DB(cont);
		db.open();
		Cursor c = db.getMessagesByThread(threadId);
		ArrayList<TextMessage> thread = new ArrayList<TextMessage>();
		//Set up the vars for creating the TextMessage objects
		String number = db.getPhoneNumber(threadId);
		String name = Utils.lookupContactByNumber(number, cont).name;
		int bodyCol = c.getColumnIndex(DBConstants.MESSAGE_BODY);
		int readCol = c.getColumnIndex(DBConstants.MESSAGE_READ);
		int typeCol = c.getColumnIndex(DBConstants.MESSAGE_TYPE);
		int timeCol = c.getColumnIndex(DBConstants.MESSAGE_TIME);
		while(c.moveToNext()){
			//Loop through everything, and pull the information
			TextMessage cur = new TextMessage(number, c.getLong(timeCol), c.getString(bodyCol), c.getInt(typeCol));
			cur.setName(name);
			if(c.getInt(readCol) == TextMessage.READ)
				cur.read();
			thread.add(cur);
		}
		db.close();
		thread.trimToSize();
		return thread;
	}
	
	
	/**
	 * Returns a TextMessage object from a message received over bluetooth
	 * @param message
	 * @return
	 */
	public static TextMessage decodeMessage(String message, Context context){
		/**
		 * Message format
		 * Number:Time:Message
		 * 
		 * Use String.split(":",3) to split the string. Number is the size of the final array
		 */
		if(message == null || message.equals(""))
			return null;
		String parts[] = message.split(":", 3); 
		TextMessage toRet = new TextMessage( parts[0],
				Long.parseLong(parts[1]),
				parts[2], TextMessage.RECEIVED);
		toRet.setName( Utils.lookupContactByNumber(parts[0], context).name );
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		int max = Integer.parseInt( prefs.getString("convMax", "100") );
		if(max < 25)
			max = 25;
		if(max > 500)
			max = 500;
		//Insert into the database
		DB db = new DB(context);
		db.open();
		db.insertMessage(toRet.getMessage(), toRet.getPhoneNumber(), toRet.getTime(), toRet.type(),max);
		db.close();
		return toRet;
	}
	
	/**
	 * This strips out all non-digit characters from a phone number
	 * @param number
	 * @return
	 */
	public static String parseNumber(String number){
		if(number == null || number.equals(""))
			return null;
		String newNumber = "";
		char cur;
		for(int i=0; i < number.length(); i++){
			cur = number.charAt(i);
			if( Character.isDigit(cur) )
				newNumber += cur;
		}
		return newNumber;
	}
	
	public static ArrayList<Contact> getThreads(Context context){
		DB db = new DB(context);
		db.open();
		Cursor c = db.getThreads();
		ArrayList<Contact> contacts = new ArrayList<Contact>();
        if(c.moveToFirst()){
        	int idCol = c.getColumnIndex(DBConstants.THREAD_ID);
        	int keyCol = c.getColumnIndex(DBConstants.THREAD_KEY);
        	int numberCol = c.getColumnIndex(DBConstants.THREAD_PHONE);
        	int readCol = c.getColumnIndex(DBConstants.THREAD_UNREAD);
        	int read =0;
        	do{
        		//Get the info, fill the wrapper
        		String number = c.getString(numberCol);
        		Contact temp = Utils.lookupContactByNumber(number, context);
        		temp.threadId = c.getInt(idCol);
        		temp.key = c.getString(keyCol);
        		read = c.getInt(readCol);
        		if(read ==1)
        			temp.hasUnread = true;
        		contacts.add(temp);
        	}while(c.moveToNext());
        	
        }
        contacts.trimToSize();
        db.close();
        contacts.trimToSize();
        return contacts;
	}
	
	/**
	 * Saves a sent message
	 * @param message the message, including number
	 * @param context context
	 */
	public static void sentMessage(String message, Context context){
		//Need to strip the 1111111111: part off the body
		//Adjust time back to GMT
		DB db = new DB(context);
		db.open();
		long time = System.currentTimeMillis();
		String parts[] = message.split(":", 2);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		int max = Integer.parseInt( prefs.getString("convMax", "100") );
		if(max < 25)
			max = 25;
		if(max > 500)
			max = 500;
		db.insertMessage(parts[1], parts[0], time + TimeZone.getDefault().getOffset(time), TextMessage.SENT,max);
		db.close();
	}
	
	public static void clearMessages(Context context){
		DB db = new DB(context);
		db.open();
		db.clear();
		db.close();
	}
	
}
