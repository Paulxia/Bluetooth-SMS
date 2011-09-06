package com.tortel.bt;

/**
 * Container class for a message and related data
 * 
 * @author Scott Warner
 */
public class TextMessage {
	//Types
	public static final int UNREAD=1;
	public static final int READ=1;
	public static final int SENT=1;
	public static final int RECEIVED=2;
	
	
	private String message;
	private String number;
	private long time;
	private String name;
	private boolean read;
	private int type;
	
	/**
	 * Creates a TextMessage object based off the data
	 * @param number the phone number
	 * @param message the message body
	 * @param time the time (In milis)
	 */
	public TextMessage(String number, long time, String message, int type){
		this.message = message;
		this.number = number;
		this.time = time;
		this.type = type;
		read = false;
	}
	
	/**
	 * Returns the mesasge body
	 * @return body
	 */
	public String getMessage(){
		return message;
	}
	
	/**
	 * Returns the phone number from the message
	 * @return nuber
	 */
	public String getPhoneNumber(){
		return number;
	}
	
	/**
	 * Sets the name for the person that sent the message
	 * @param name
	 */
	public void setName(String name){
		this.name = name;
	}
	
	/**
	 * Retuns the type of the message
	 * @return either SENT or RECEIVED
	 */
	public int type(){
		return type;
	}
	
	/**
	 * Returns the name of the person that sent the message, or the phone number if its unknown
	 * @return name
	 */
	public String getName(){
		if(type == SENT)
			return "Me";
		if(name == null || name.equals(""))
			return number;
		else
			return name;
	}
	
	/**
	 * Returns the time the message was sent.
	 * @return the time in milliseconds
	 */
	public long getTime(){
		return time;
	}
	
	/**
	 * Marks the TextMessage as read
	 */
	public void read(){
		read = true;
	}
	
	/**
	 * True is read, false if not.
	 * @return
	 */
	public boolean isRead(){
		return read;
	}
	
	/**
	 * Returns a byte array suitable for sending over bluetooth
	 * @return the array
	 */
	public byte[] bytes(){
		String toRet = number+":"+time+":"+message;
		return toRet.getBytes();
	}
	
	public String toString(){
		return this.getName() +":\n"+message;
	}
	
}
