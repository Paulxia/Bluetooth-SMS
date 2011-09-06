package com.tortel.bt;

/**
 * Class to store database related constants
 * @author Scott Warner
 */
public class DBConstants {
	public static final String DATABASE_NAME="btsms";
	public static final int DATABASE_VERSION=1;
	
	/**
	 * Table 1 - Messages table
	 */
	public static final String TABLE_NAME_MESSAGES="messages";
	//Short Version
	public static final String MESSAGES = TABLE_NAME_MESSAGES;
	//Thread ID
	public static final String MESSAGE_THREAD="threadid";
	public static final String MESSAGE_TIME="time";
	public static final String MESSAGE_BODY="body";
	//Sent/Received
	public static final String MESSAGE_TYPE="type";
	public static final String MESSAGE_READ="read";
	public static final String MESSAGE_ID="_id";
	
	/**
	 * Table 2 - Thread table
	 */
	public static final String TABLE_NAME_THREADS="threads";
	//Short version
	public static final String THREADS = TABLE_NAME_THREADS;
	//Use the id label from the DB as the thread id
	public static final String THREAD_ID="_id";
	public static final String THREAD_KEY="key";
	public static final String THREAD_UNREAD="unread";
	public static final String THREAD_PHONE="phone";
	public static final String THREAD_TIME="time";
}
