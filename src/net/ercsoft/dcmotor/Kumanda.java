package net.ercsoft.dcmotor;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;
import net.ercsoft.dcmotor.R;

public class Kumanda extends Activity {

		private static final String TAG = "ArduinoAccessory";
		private static final int direction_SEND = 1;
		private static final int CLEAR_DIRECTION= 0;
	 
		private static final String ACTION_USB_PERMISSION = "net.ercsoft.dcmotor.USB_PERMISSION";
	 
		private UsbManager mUsbManager;
		private PendingIntent mPermissionIntent;
		private boolean mPermissionRequestPending;
		private Button ileriBtn,solBtn,sagBtn,geriBtn,durBtn;
	 
		UsbAccessory mAccessory;
		ParcelFileDescriptor mFileDescriptor;
		FileInputStream mInputStream;
		FileOutputStream mOutputStream;
	 
		private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (ACTION_USB_PERMISSION.equals(action)) {
					synchronized (this) {
						UsbAccessory accessory = UsbManager.getAccessory(intent);
						if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
							openAccessory(accessory);
						} else {
							Log.d(TAG, "permission denied for accessory " + accessory);
						}
						mPermissionRequestPending = false;
					}
				} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
					UsbAccessory accessory = UsbManager.getAccessory(intent);
					if (accessory != null && accessory.equals(mAccessory)) {
						closeAccessory();
					}
				}
			}
		};
	 
	 
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
	 
			mUsbManager = UsbManager.getInstance(this);
			mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
			IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
			filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
			registerReceiver(mUsbReceiver, filter);
	 
			if (getLastNonConfigurationInstance() != null) {
				mAccessory = (UsbAccessory) getLastNonConfigurationInstance();
				openAccessory(mAccessory);
			}
	 
			setContentView(R.layout.activity_text);
			ileriBtn = (Button) findViewById(R.id.ileri);
			ileriBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					sendDirection("w");
				}
			});
			geriBtn = (Button) findViewById(R.id.geri);
			geriBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					sendDirection("s");
				}
			});
			solBtn = (Button) findViewById(R.id.sol);
			solBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					sendDirection("a");
				}
			});
			sagBtn = (Button) findViewById(R.id.sag);
			sagBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					sendDirection("d");

				}
			});
			durBtn = (Button) findViewById(R.id.dur);
			durBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					clearDirection();
				}
			});

			
		}
	 
		@Override
		public Object onRetainNonConfigurationInstance() {
			if (mAccessory != null) {
				return mAccessory;
			} else {
				return super.onRetainNonConfigurationInstance();
			}
		}
	 
		@Override
		public void onResume() {
			super.onResume();
	 
			if (mInputStream != null && mOutputStream != null) {
				return;
			}
	 
			UsbAccessory[] accessories = mUsbManager.getAccessoryList();
			UsbAccessory accessory = (accessories == null ? null : accessories[0]);
			if (accessory != null) {
				if (mUsbManager.hasPermission(accessory)) {
					openAccessory(accessory);
				} else {
					synchronized (mUsbReceiver) {
						if (!mPermissionRequestPending) {
							mUsbManager.requestPermission(accessory,mPermissionIntent);
							mPermissionRequestPending = true;
						}
					}
				}
			} else {
				Log.d(TAG, "mAccessory is null");
			}
		}
	 
		@Override
		public void onPause() {
			super.onPause();
			closeAccessory();
		}
	 
		@Override
		public void onDestroy() {
			unregisterReceiver(mUsbReceiver);
			super.onDestroy();
		}
	 
		private void openAccessory(UsbAccessory accessory) {
			mFileDescriptor = mUsbManager.openAccessory(accessory);
			if (mFileDescriptor != null) {
				mAccessory = accessory;
				FileDescriptor fd = mFileDescriptor.getFileDescriptor();
				mInputStream = new FileInputStream(fd);
				mOutputStream = new FileOutputStream(fd);
				if(mOutputStream!=null) {
					ileriBtn.setVisibility(View.VISIBLE);
					geriBtn.setVisibility(View.VISIBLE);
					solBtn.setVisibility(View.VISIBLE);
					sagBtn.setVisibility(View.VISIBLE);
					durBtn.setVisibility(View.VISIBLE);

				}
				Log.d(TAG, "accessory opened");
			} else {
				Log.d(TAG, "accessory open fail");
			}
		}
	 
	 
		private void closeAccessory() {
			try {
				if (mFileDescriptor != null) {
					mFileDescriptor.close();
				}
			} catch (IOException e) {
			} finally {
				mFileDescriptor = null;
				mAccessory = null;
			}
		}
	 
		public void sendDirection(String yon){
			if (mOutputStream != null) {
				try {
					int directionSize = yon.length();
					byte buffer[] = new byte[2+directionSize];
					buffer[0] = direction_SEND;
					buffer[1] = (byte) directionSize;
					byte[] textdirection = yon.getBytes();
					for (int i = 0; i < directionSize; i++) {
						buffer[2+i] = textdirection[i];
					}
					mOutputStream.write(buffer);
				} catch (IOException e) {
					Log.e(TAG, "write failed", e);
				}
			}
		}
		public void clearDirection(){
			if (mOutputStream != null) {
				try {
					byte buffer[] = new byte[1];
					buffer[0] = CLEAR_DIRECTION;
					mOutputStream.write(buffer);
				} catch (IOException e) {
					Log.e(TAG, "write failed", e);
				}
			}
		}

}
