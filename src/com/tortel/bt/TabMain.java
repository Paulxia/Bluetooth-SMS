package com.tortel.bt;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.telephony.PhoneNumberUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * TODO:
 * New icon
 * Make the service an actual service, and bind to it etc...
 * Add contact icons
 * Better Tablet notifications
 * Deleting threads
 * ActionBar background image
 * Fix embarrased smiley
 * Conversation size limit
 * Clear unread on select
 * Save messages as draft when switching threads
 * Maybe enable syncing all texts?
 * Different colors for sent/received messages?
 * Prefs!
 * 
 * Phone interface, maybe
 * Add some prefs
 * Clean up code
 * 
 * 
 * Done:
 * Current conversation a different color
 * Make conversations with unread messages a different color
 * Fixed send button
 * Smilies
 * 
 * @author swarner2
 *
 */
public class TabMain extends ListActivity {
    // Debugging
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    
    private static final int PICK_CONTACT = 3;

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private EditText mSendToText;
    private Button mSendButton;
    

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ConversationFiller conversationFiller;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;
    
    //Handler
    private Handler mHandler;
	
	private static final int DIALOG_QUIT=1;
	
	//Assorted Vars
	private ThreadFiller threads;
	//private ActionBar aBar;
	private Button newConv;
	private int threadId;
	
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		//Make sure phone doesnt go to this activity
		Type.TYPE = Type.TAB;
	
		setContentView(R.layout.tab_layout);
		
		Type.TYPE = Type.TAB;
		
		if(BluetoothChatService.getInstance() == null){
			//Create variables
			mHandler = new MessageHandler();
			BluetoothChatService.createInstance(this, mHandler);
		}
		
		
		threads = new ThreadFiller(this);
		this.setListAdapter(threads);
		
		ActionBar aBar = getActionBar();
		aBar.setDisplayShowTitleEnabled(true);
		if( mConnectedDeviceName == null )
				aBar.setSubtitle(R.string.not_connected);
		else
			aBar.setSubtitle("Connected to "+mConnectedDeviceName);
		//TODO: Fancy image background for the actionbar
		aBar.setBackgroundDrawable(getResources().getDrawable(R.color.actionbar ));
     	mOutEditText = (EditText) findViewById(R.id.edit_text_out);
     	mSendButton = (Button) findViewById(R.id.button_send);
     	mSendToText = (EditText) findViewById(R.id.sendToEditText);
     	mSendToText.setVisibility(View.GONE);
     	
     	mConversationView = (ListView) findViewById(R.id.messages);
     	
		threadId = -1;
		
		//Get the button, set up a listener
		newConv = (Button) findViewById(R.id.newThread);
		newConv.setOnClickListener(new NewConversationListener());
		
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
	}
	
    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.d("++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
    }
	
	/**
	 * Close the app
	 */
	public void onDestroy(){
		super.onDestroy();
	}
	
	/**
	 * Resume
	 */
	public void onResume(){
		super.onResume();
		//Make sure phone doesnt go to this activity
		if(Type.TYPE == Type.PHONE ){
			Intent goToPhone = new Intent(this, BluetoothSms.class);
			startActivity(goToPhone);
		} else {
			threads = new ThreadFiller(this);
			this.setListAdapter(threads);
		}
	}
	
	/**
	 * Thread has been selected
	 */
	public void onListItemClick(ListView parent, View v, int position, long id){
		//Hide the phone number, and fill it from the list
		mSendToText.setVisibility(View.GONE);
		Contact tmp = (Contact) parent.getItemAtPosition(position);
		mSendToText.setText( tmp.number );
		threadId = tmp.threadId;
		//Redraw the lists
		Utils.markRead(threadId, this);
		tmp.hasUnread = false;
		setListAdapter(threads);
		conversationFiller = new ConversationFiller(this);
		mConversationView.setAdapter(conversationFiller);
	}
	
	/**
	 * Sets up all the variables for everything to work correctly
	 */
    private void setupChat() {
        Log.d("setupChat()");
        
        // Initialize the compose field with a listener for the return key
        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        mOutEditText.setOnEditorActionListener(mWriteListener);
        
        // Initialize the send button with a listener that for click events
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	//Format the message and send it
            	initMessage();
            }
        });
	        
        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = BluetoothChatService.getInstance();
        
        //Start the RunnerService, to keep this in memory
        Intent service = new Intent(this, RunnerService.class);
        startService(service);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }
	
    private void ensureDiscoverable() {
        if(D) Log.d("ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener =
        new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
            	initMessage();
            }
            if(D) Log.i("END onEditorAction");
            return true;
        }
    };
	
    /**
     * Sends the message
     */
    private void initMessage(){
    	if(mSendToText.getText() != null && !mSendToText.getText().toString().trim().equals("")
    			&& mOutEditText.getText() != null && !mOutEditText.getText().toString().trim().equals("")){
    		String message=null;
    		if(mSendToText.getVisibility() == View.GONE)
    			message = mSendToText.getText().toString() +
    				":"+ mOutEditText.getText().toString();
    		else
    			message = mSendToText.getText().toString()+
    				":"+ mOutEditText.getText().toString();
            mSendToText.setVisibility(View.GONE);
            sendMessage(message);
    	}
    }
    
	/**
	 * Creates option menu
	 */
	public boolean onCreateOptionsMenu(Menu menu){
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		return true;
	}
	
	/**
	 * After the connection dialog
	 */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d("onActivityResult " + resultCode);
        if(data == null){
        	Toast.makeText(this, "Null data!", 2000);
        	return;
        }
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                if(address == null){
                	Toast.makeText(this, "Null device!", 2000).show();
                	return;
                }
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mChatService.connect(device);
            }
            break;
        case PICK_CONTACT:
            Cursor cursor = null;  
            String phoneNumber = "";
            List<String> allNumbers = new ArrayList<String>();
            int phoneIdx = 0;
            try {  
                Uri result = data.getData();  
                String id = result.getLastPathSegment();  
                cursor = getContentResolver().query(Phone.CONTENT_URI, null, Phone.CONTACT_ID + "=?", new String[] { id }, null);  
                phoneIdx = cursor.getColumnIndex(Phone.DATA);
                if (cursor.moveToFirst()) {
                    while (cursor.isAfterLast() == false) {
                        phoneNumber = cursor.getString(phoneIdx);
                        allNumbers.add(phoneNumber);
                        cursor.moveToNext();
                    }
                } else {
                    //no results actions
                }  
            } catch (Exception e) {  
               //error actions
            } finally {  
                if (cursor != null) {  
                    cursor.close();
                }

                final CharSequence[] items = allNumbers.toArray(new String[allNumbers.size()]);
                AlertDialog.Builder builder = new AlertDialog.Builder(TabMain.this);
                builder.setTitle("Choose a number");
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        String selectedNumber = items[item].toString();
                        selectedNumber = selectedNumber.replace("-", "");
                        mSendToText.setText( selectedNumber );
                    }
                });
                AlertDialog alert = builder.create();
                if(allNumbers.size() > 1) {
                    alert.show();
                } else {
                    String selectedNumber = phoneNumber.toString();
                    //selectedNumber = selectedNumber.replace("-", "");
                    mSendToText.setText(selectedNumber);
                }

                if (phoneNumber.length() == 0) {  
                    //no numbers found actions  
                }  
            }  
            break; 
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupChat();
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d("BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
	
    /**
     * Handle options menu selections
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.scan:
            // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
        case R.id.discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
        case R.id.prefs:
        	Intent prefsIntent = new Intent(this, Settings.class);
        	startActivity(prefsIntent);
        	return true;
        }
        return false;
    }
	
    /**
     * Quit confirmation dialog listener
     */
    @Override
    public void onBackPressed (){   
    	showDialog(DIALOG_QUIT);
    }
    
    /**
     * Dialog creation
     */
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch(id) {
        case DIALOG_QUIT:
            AlertDialog.Builder builder;
            LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.quit_dialog,
                            (ViewGroup) findViewById(R.id.layout_root));
            TextView text = (TextView) layout.findViewById(R.id.text);
            text.setText("This will end all communication.");

            builder = new AlertDialog.Builder(this);
            builder.setView(layout);
            builder.setTitle("Are you sure you want to quit?");
            builder.setCancelable(false);
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            	public void onClick(DialogInterface dialog, int id) {
            		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(TabMain.this);
                    if(prefs.getBoolean("clear", false) )
                    	Utils.clearMessages(TabMain.this);
                    
                    TabMain.this.finish();
                    }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            	public void onClick(DialogInterface dialog, int id) {
            		dialog.cancel();
            	}
            });
            dialog = builder.create();
            break;
        default:
            dialog = null;
        }
        return dialog;
    }
	
	/**
	 * Class to listen for the New Conversation button being selected
	 */
    class NewConversationListener implements View.OnClickListener{
		/**
         * Button event method
         */
    	public void onClick(View view) {
    		mSendToText.setText("");
    		mSendToText.setVisibility(View.VISIBLE);
    		
    		//Open the contact picker
    		Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
    		startActivityForResult(contactPickerIntent, PICK_CONTACT);
    	}
    }
	
    /**
     * Fills the list of open threads
     */
	class ThreadFiller extends BaseAdapter {
		private ArrayList<Contact> contacts;
		private LayoutInflater inflater;
		
		public ThreadFiller(Context context) {
			contacts = new ArrayList<Contact>();
			inflater=getLayoutInflater();
			getdata();
		}
		
		public void getdata(){
			contacts = Utils.getThreads(TabMain.this);
		}
		
		/**
		 * Actual method that fills the list
		 */
		public View getView(int position, View convertView, ViewGroup parent) {
			View row=inflater.inflate(R.layout.thread_element, parent, false);
			Contact cur = getItem(position);
			TextView contactName=(TextView)row.findViewById(R.id.contact_name);
			contactName.setText(cur.name);
			TextView contactNumber = (TextView) row.findViewById(R.id.display_number);
			contactNumber.setText( PhoneNumberUtils.formatNumber(cur.number) );
			if(cur.hasUnread){
				//TODO:Colors go here. Using hex values
				//Gold color for unread
				contactName.setTextColor( getResources().getColor(R.color.unread) );
				contactNumber.setTextColor( getResources().getColor(R.color.unread) );
			}
			if(cur.threadId == threadId){
				//Set the current conversation to a different color
				contactName.setTextColor(getResources().getColor(R.color.selected));
				contactNumber.setTextColor(getResources().getColor(R.color.selected));
			}
			//TODO: Use badge or not?
			//QuickContactBadge badge = (QuickContactBadge) row.findViewById(R.id.contact_badge);
			//badge.assignContactUri( Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, getItem(position).key) );
			//badge.assignContactFromPhone( getItem(position).number , true);
			//badge.setImageResource(R.drawable.app_icon);
			return(row);
		}
		
		
		public int getCount() {
			return contacts.size();
		}
		
		public Contact getItem(int i) {
			return contacts.get(i % getCount());
		}
		public long getItemId(int i) {
			return i;
		}
	}
	
    /**
     * Fills the conversation list
     */
	class ConversationFiller extends BaseAdapter {
		private ArrayList<TextMessage> messages;
		private LayoutInflater inflater;
		
		public ConversationFiller(Context context) {
			messages = new ArrayList<TextMessage>();
			inflater=getLayoutInflater();
			getdata();
		}
		
		public synchronized void getdata(){
			messages = Utils.getMessages(threadId, TabMain.this);
		}
		
		/**
		 * Actual method that fills the list
		 */
		public View getView(int position, View convertView, ViewGroup parent) {
			SmileyParser smile = SmileyParser.getInstance();
			if(smile == null){
				SmileyParser.init(TabMain.this);
				smile = SmileyParser.getInstance();
			}
			View row = inflater.inflate(R.layout.message_part, parent, false);
			TextMessage cur = this.getItem(position);
			TextView message = (TextView) row.findViewById(R.id.message_body);
			message.setText( smile.addSmileySpans( cur.toString() ) );
			TextView time = (TextView) row.findViewById(R.id.message_time);
			//Need to subtract the offset to make it the right time
			Date date = new Date(cur.getTime() - TimeZone.getDefault().getOffset(cur.getTime()));
			SimpleDateFormat df = new SimpleDateFormat("hh:mma MMM dd");
			time.setText( df.format(date) );
			return row;
		}
		
		
		public int getCount() {
			return messages.size();
		}
		
		public TextMessage getItem(int i) {
			return messages.get(i % getCount());
		}
		public long getItemId(int i) {
			return i;
		}
	}
	
    // The Handler that gets information back from the BluetoothChatService
    private class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	ActionBar actionBar = TabMain.this.getActionBar();
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i("MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
            		//tab, use ActionBar
            		actionBar.setSubtitle("Connected to "+mConnectedDeviceName);
                    break;
                case BluetoothChatService.STATE_CONNECTING:
            		//Tab
            		actionBar.setSubtitle(R.string.title_connecting);
                    break;
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:
                	mConnectedDeviceName = null;
            		//Tab
            		actionBar.setSubtitle(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
            	//TODO: This is sending a message manually. This should be removed, or changed so it can send a text
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                //mConversationArrayAdapter.add(writeMessage);
                //TODO: Confirm this works
                Utils.sentMessage(writeMessage, TabMain.this);
                //Refresh the conversation and the threads
                conversationFiller = new ConversationFiller(getBaseContext());
                mConversationView.setAdapter(conversationFiller);
                threads = new ThreadFiller(getBaseContext());
                TabMain.this.setListAdapter(threads);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                //TODO: Grab the message from here, and format it as needed.
            	//Just take it and display it
            	Log.v("Tab got message. Displaying.");
            	TextMessage mess = Utils.decodeMessage(readMessage, TabMain.this);
            	//Make a notification for it :)
            	NotificationManager noteMan = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            	Notification note = new Notification(R.drawable.app_icon,mess.toString(), mess.getTime());
            	note.flags |= Notification.FLAG_AUTO_CANCEL;
            	
            	//Give it intents
            	PendingIntent pi = PendingIntent.getActivity(TabMain.this, 0, new Intent(TabMain.this, BluetoothSms.class),0);
            	note.setLatestEventInfo(TabMain.this, mess.getName(), mess.getMessage(), pi);
            	
            	//push the notification
            	noteMan.notify(1210, note);
            	
            	//mConversationArrayAdapter.add(mess.toString());
            	
            	//refresh the conversation
            	conversationFiller = new ConversationFiller(getBaseContext());
            	mConversationView.setAdapter(conversationFiller);
            	//Refresh the threads
                threads = new ThreadFiller(getBaseContext());
                TabMain.this.setListAdapter(threads);
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
}
