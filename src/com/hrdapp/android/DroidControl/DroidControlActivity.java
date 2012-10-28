package com.hrdapp.android.DroidControl;

import java.lang.ref.WeakReference;

import com.hrdapp.android.DroidControl.R.id;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;


public class DroidControlActivity extends Activity {

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

    // Layout Views
    private TextView mTitle;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
//    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
//    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothDroidControlService mCmdSendService = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
    
        SeekBar seekBar = (SeekBar) findViewById(id.seekBar1);
        seekBar.setMax(100);
        seekBar.setProgress(50);
        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            // トラッキング開始時に呼び出されます
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.v("onStartTrackingTouch()",
                    String.valueOf(seekBar.getProgress()));
            }
            // トラッキング中に呼び出されます
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
                Log.v("onProgressChanged()",
                    String.valueOf(progress) + ", " + String.valueOf(fromTouch));
                
                sendMessage(String.format("s%02d", progress/10));
            }
            // トラッキング終了時に呼び出されます
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.v("onStopTrackingTouch()",
                    String.valueOf(seekBar.getProgress()));
            }
        });
        CheckBox checkBox1 = (CheckBox) findViewById(id.led1);
        // チェックボックスのチェック状態を設定します
        checkBox1.setChecked(false);
        // チェックボックスがクリックされた時に呼び出されるコールバックリスナーを登録します
        checkBox1.setOnClickListener(new View.OnClickListener() {
            
            // チェックボックスがクリックされた時に呼び出されます
            public void onClick(View v) {
                CheckBox checkBox = (CheckBox) v;
                // チェックボックスのチェック状態を取得します
                boolean checked = checkBox.isChecked();
                if(checked)
                {
                	sendMessage("l11");
                }else{
                	sendMessage("l10");
                }
            }
        });
        CheckBox checkBox2 = (CheckBox) findViewById(id.led2);
        // チェックボックスのチェック状態を設定します
        checkBox2.setChecked(false);
        // チェックボックスがクリックされた時に呼び出されるコールバックリスナーを登録します
        checkBox2.setOnClickListener(new View.OnClickListener() {
            
            // チェックボックスがクリックされた時に呼び出されます
            public void onClick(View v) {
                CheckBox checkBox = (CheckBox) v;
                // チェックボックスのチェック状態を取得します
                boolean checked = checkBox.isChecked();
                if(checked)
                {
                	sendMessage("l21");
                }else{
                	sendMessage("l20");
                }
            }
        });

        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);
        
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

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mCmdSendService == null) setupBT();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mCmdSendService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mCmdSendService.getState() == BluetoothDroidControlService.STATE_NONE) {
              // Start the Bluetooth chat services
            	mCmdSendService.start();
            }
        }
    }

    private void setupBT() {

        // Initialize the BluetoothChatService to perform bluetooth connections
    	mCmdSendService = new BluetoothDroidControlService(this, mHandler);

    }

    @Override
    public synchronized void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mCmdSendService != null) mCmdSendService.stop();
    }
    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mCmdSendService.getState() != BluetoothDroidControlService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mCmdSendService.write(send);
        }
    }

    static class BtHandler extends Handler {
        private final WeakReference<DroidControlActivity> mActivity; 

        BtHandler(DroidControlActivity activity) {
        	mActivity = new WeakReference<DroidControlActivity>(activity);
        }
        @Override
        public void handleMessage(Message msg)
        {
        	DroidControlActivity activity = mActivity.get();
             if (activity != null) {
            	 activity.handleMessage(msg);
             }
        }
    }

    private final Handler mHandler = new BtHandler(this);

    public void handleMessage(Message msg) {
        switch (msg.what) {
        case MESSAGE_STATE_CHANGE:
            switch (msg.arg1) {
            case BluetoothDroidControlService.STATE_CONNECTED:
                mTitle.setText(R.string.title_connected_to);
                mTitle.append(mConnectedDeviceName);
                break;
            case BluetoothDroidControlService.STATE_CONNECTING:
                mTitle.setText(R.string.title_connecting);
                break;
            case BluetoothDroidControlService.STATE_LISTEN:
            case BluetoothDroidControlService.STATE_NONE:
                mTitle.setText(R.string.title_not_connected);
                break;
            }
            break;
        case MESSAGE_WRITE:
//            byte[] writeBuf = (byte[]) msg.obj;
            // construct a string from the buffer
//            String writeMessage = new String(writeBuf);
//            mConversationArrayAdapter.add("Me:  " + writeMessage);
            break;
        case MESSAGE_READ:
//            byte[] readBuf = (byte[]) msg.obj;
            // construct a string from the valid bytes in the buffer
//            String readMessage = new String(readBuf, 0, msg.arg1);
//            mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mCmdSendService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupBT();
            } else {
                // User did not enable Bluetooth or an error occured
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.connect:
            // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
        case R.id.disconnect:
            // Stop the Bluetooth chat services
            if (mCmdSendService != null) mCmdSendService.stop();
        	return true;
        }
        return false;
    }

}