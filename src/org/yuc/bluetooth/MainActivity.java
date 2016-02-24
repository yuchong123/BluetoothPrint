package org.yuc.bluetooth;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Toast;

/**
 * Android蓝牙图片打印
 * @author 小土豆YUC
 * 2015年12月15日
 */
public class MainActivity extends Activity {

    // Message types sent from the BluetoothService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    
    // Key names received from the BluetoothService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the services
    public static BluetoothService mService = null;
	private EditText print_et;
	private RadioGroup radioGroup1;
	private RadioGroup radioGroup2;
//	private RadioGroup radioGroup3;
    
    
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "您的设备不支持蓝牙", 0).show();
//			finish();
			return;
		}
		
		
		
		
		
		
		print_et = (EditText) findViewById(R.id.print_et);
		print_connect_btn = (Button) findViewById(R.id.print_connect_btn);
		radioGroup1 = (RadioGroup) findViewById(R.id.radioGroup1);
		radioGroup2 = (RadioGroup) findViewById(R.id.radioGroup2);
//		radioGroup3 = (RadioGroup) findViewById(R.id.radioGroup3);
		
		radioGroup1.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				switch (checkedId) {
				case R.id.radio0:
					mService.printLeft();
					break;
				case R.id.radio1:
					mService.printCenter();
					break;
				case R.id.radio2:
					mService.printRight();
					break;
				}
			}
		});
		
		radioGroup2.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				switch (checkedId) {
				case R.id.radio0:
					mService.printSize(0);
					break;
				case R.id.radio1:
					mService.printSize(1);
					break;
				case R.id.radio2:
					mService.printSize(2);
					break;
				}
			}
		});
		
//		radioGroup3.setOnCheckedChangeListener(new OnCheckedChangeListener() {
//			
//			@Override
//			public void onCheckedChanged(RadioGroup group, int checkedId) {
//				switch (checkedId) {
//				case R.id.radio0:
//					mService.print(11);
//					break;
//				case R.id.radio1:
//					mService.print(12);
//					break;
//				}
//			}
//		});
	}
	
	
    @Override
    public void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {
        	//打开蓝牙
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
        if (mService==null) {
        	mService = new BluetoothService(this, mHandler);
        }
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.option_menu, menu);
		return true;
	}
	
	
	public void onClick(View v){
		switch (v.getId()) {
		case R.id.print_btn:
			sendMessage(print_et.getText().toString()+"\n");
			break;
		case R.id.print_img_btn:
        	sendMessage("\n");
        	sendMessage("\n");
        	
        	BufferedInputStream bis = null;
			try {
				bis = new BufferedInputStream(getAssets()
					      .open("android.jpg"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			Bitmap bitmap = BitmapFactory.decodeStream(bis);
			
        	sendMessage(bitmap);
        	
        	sendMessage(" \n");
        	sendMessage(" \n");
        	sendMessage(" \n");
			break;
		case R.id.print_connect_btn:
			openOptionsMenu();
			break;
		case R.id.print_out_btn:
			sendMessage(" \n");
			break;
		}
	}
	
	
    /**
     * 打印
     * @param message
     */
	private void sendMessage(String message) {
		// Check that we're actually connected before trying anything
		if (mService.getState() != BluetoothService.STATE_CONNECTED) {
			Toast.makeText(this, "蓝牙没有连接", Toast.LENGTH_SHORT).show();
			return;
		}

		// Check that there's actually something to send
		if (message.length() > 0) {
			// Get the message bytes and tell the BluetoothService to write
			byte[] send;
			try {
				send = message.getBytes("GB2312");
			} catch (UnsupportedEncodingException e) {
				send = message.getBytes();
			}

			mService.write(send);
		}
	}
	
	
	private void sendMessage(Bitmap bitmap) {
		// Check that we're actually connected before trying anything
		if (mService.getState() != BluetoothService.STATE_CONNECTED) {
			Toast.makeText(this, "蓝牙没有连接", Toast.LENGTH_SHORT).show();
			return;
		}
		// 发送打印图片前导指令
		byte[] start = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1B,
				0x40, 0x1B, 0x33, 0x00 };
		mService.write(start);
		
		/**获取打印图片的数据**/
//		byte[] send = getReadBitMapBytes(bitmap);
		
		mService.printCenter();
		byte[] draw2PxPoint = PicFromPrintUtils.draw2PxPoint(bitmap);
		
		mService.write(draw2PxPoint);
		// 发送结束指令
		byte[] end = { 0x1d, 0x4c, 0x1f, 0x00 };
		mService.write(end);
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
                mService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
//                setupChat();
            	Toast.makeText(this, "蓝牙已打开", 0);
            } else {
            	Toast.makeText(this, "蓝牙没有打开", 0);
                finish();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.scan:
            // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
        case R.id.disconnect:
            // disconnect
        	mService.stop();
            return true;
        }
        return false;
    }

    
    // The Handler that gets information back from the BluetoothService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BluetoothService.STATE_CONNECTED:
                	print_connect_btn.setText("已连接:");
                	print_connect_btn.append(mConnectedDeviceName);
                    break;
                case BluetoothService.STATE_CONNECTING:
                	print_connect_btn.setText("正在连接...");
                    break;
                case BluetoothService.STATE_LISTEN:
                case BluetoothService.STATE_NONE:
                	print_connect_btn.setText("无连接");
                    break;
                }
                break;
            case MESSAGE_WRITE:
                //byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                //String writeMessage = new String(writeBuf);
                break;
            case MESSAGE_READ:
                //byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                //String readMessage = new String(readBuf, 0, msg.arg1);
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "连接至"
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
	private Button print_connect_btn;
}
