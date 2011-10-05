package com.tortel.bt;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class RunnerService extends Service {
	//Keep a reference to the important classes
	private BluetoothChatService service;
	
	public void onCreate(){
		setService(BluetoothChatService.getInstance());
	}
	
	
	public void onStart(Intent intent, int num){
		setService(BluetoothChatService.getInstance());
	}
	
	
	
	public BluetoothChatService getService() {
		return service;
	}


	public void setService(BluetoothChatService service) {
		this.service = service;
	}


	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
