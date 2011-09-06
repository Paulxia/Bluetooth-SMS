package com.tortel.bt;

public class Contact {
	public String name;
	public String number;
	public int threadId;
	public String key;
	public boolean hasUnread;
	
	public Contact(String name, String number){
		this.name = name;
		this.number = number;
		hasUnread = false;
	}
	
	public String toString(){
		//String toRet = "Contact: ";
		//toRet += name+" "+number;
		return name;
	}
}
