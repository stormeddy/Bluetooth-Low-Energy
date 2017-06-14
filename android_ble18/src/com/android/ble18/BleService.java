package com.android.ble18;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.Loader.ForceLoadContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BleService extends Service implements BluetoothAdapter.LeScanCallback {
	public static final String TAG = "BleService";
	public static final String TAG1 = "BleService1";
	static final int MSG_REGISTER = 1;
	static final int MSG_UNREGISTER = 2;
	static final int MSG_START_SCAN = 3;
	static final int MSG_STATE_CHANGED = 4;
	static final int MSG_DEVICE_FOUND = 5;
	static final int MSG_DEVICE_CONNECT = 6;
	static final int MSG_DEVICE_DISCONNECT = 7;
	static final int MSG_DEVICE_DATA = 8;
	public static int  temp[][]=new int[DisplayViewActivity.AP][DisplayViewActivity.ANUMBER];
	
	
	

	private static final long SCAN_PERIOD = 3000;

	public static final String KEY_MAC_ADDRESSES = "KEY_MAC_ADDRESSES";

	private static final String DEVICE_NAME = "SensorTag";
	public static final UUID UUID_COORDINATES_SERVICE = UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB");
	public static final UUID UUID_COORDINATES_DATA = UUID.fromString("0000FFF5-0000-1000-8000-00805F9B34FB");
	private static final UUID UUID_HUMIDITY_CONF = UUID.fromString("f000aa22-0451-4000-b000-000000000000");
	private static final UUID UUID_CCC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
	private static final byte[] ENABLE_SENSOR = {0x01};

	private static final Queue<Object> sWriteQueue = new ConcurrentLinkedQueue<Object>();
	private static boolean sIsWriting = false;

	private final IncomingHandler mHandler;
	private final Messenger mMessenger;
	private final List<Messenger> mClients = new LinkedList<Messenger>();
	final static Map<String, BluetoothDevice> mDevices = new HashMap<String, BluetoothDevice>();
	public static BluetoothGatt[] mGatt = {null,null,null,null,null,null};
	public  int mIndex=0;

	public enum State {
		UNKNOWN,
		IDLE,
		SCANNING,
		BLUETOOTH_OFF,
		CONNECTING,
		CONNECTED,
		DISCONNECTING
	}

	private BluetoothAdapter mBluetooth = null;
	private State mState = State.UNKNOWN;

	BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			super.onConnectionStateChange(gatt, status, newState);
			Log.v(TAG, "Connection State Changed: " + (newState == BluetoothProfile.STATE_CONNECTED ? "Connected" : "Disconnected"));
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				setState(State.CONNECTED);
	           /* mGatt[mIndex].readRemoteRssi();

	            // Attempts to discover services after successful connection.
	            Log.i(TAG, "Attempting to start service discovery:" +
	                    mGatt[mIndex].discoverServices());*/
				gatt.discoverServices();
			} else {
				setState(State.IDLE);
			}
		}
		
	public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
		 if (status == BluetoothGatt.GATT_SUCCESS) 
			  Log.e(TAG1,"onCharRead "+gatt.getDevice().getName()
					   +" read "
					   +characteristic.getUuid().toString()
					   +"->"
					   +bytesToHexString(characteristic.getValue())+"end");
			 
	}
	
	   public String bytesToHexString(byte[] src){  
	       StringBuilder stringBuilder = new StringBuilder("");  
	       if (src == null || src.length <= 0) {  
	           return null;  
	       }  
	       for (int i = 0; i < src.length; i++) {  
	           int v = src[i] & 0xFF;  
	           String hv = Integer.toHexString(v);  
	           if (hv.length() < 2) {  
	               stringBuilder.append(0);  
	           }  
	           stringBuilder.append(hv+" ");  
	       }  
	       return stringBuilder.toString();  
	   }  
		
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
			if (status==BluetoothGatt.GATT_SUCCESS) {
				String gattString=gatt.getDevice().getAddress();
	/*			try {
					FileOutputStream fos = new FileOutputStream(MessureActivity.fileout, true);
					fos.write(String.valueOf(gattString.substring(gattString.length()-2)+" "+rssi+"\n").getBytes());
					fos.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}catch (IOException e) {
					// TODO: handle exception
				}*/
				Log.d(TAG, String.format("BluetoothGatt ReadRssi[%d]"+gattString, rssi));
				int i;
				for (i = 0; i < DeviceListFragment.mDevices.length; i++) {
					if (gatt.getDevice().getAddress().equals(DeviceListFragment.mDevices[i])) {
						break;
					}
				}
					for (int j = 1; j < DisplayViewActivity.ANUMBER; j++) {
						temp[i][j-1]=temp[i][j];						
					}
					temp[i][DisplayViewActivity.ANUMBER-1]=rssi;
				
				
			}
		}
		
		
		
		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			Log.v(TAG, "onServicesDiscovered: " + status);
			if (status == BluetoothGatt.GATT_SUCCESS) {
				subscribe(gatt);
			}
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			Log.v(TAG, "onCharacteristicWrite: " + status);
			sIsWriting = false;
			nextWrite();
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			Log.v(TAG, "onDescriptorWrite: " + status);
			sIsWriting = false;
			nextWrite();
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			Log.v(TAG, "onCharacteristicChanged: " + characteristic.getUuid());
			if (characteristic.getUuid().equals(UUID_COORDINATES_DATA)) {
				int t = shortUnsignedAtOffset(characteristic, 0);
				int h = shortUnsignedAtOffset(characteristic, 2);
				t = t - (t % 4);
				h = h - (h % 4);

				float humidity = (-6f) + 125f * (h / 65535f);
				float temperature = -46.85f + 175.72f / 65536f * (float) t;
				Log.d(TAG, "Value: " + humidity + ":" + temperature);
				Message msg = Message.obtain(null, MSG_DEVICE_DATA);
				msg.arg1 = (int) (temperature * 100);
				msg.arg2 = (int) (humidity * 100);
				sendMessage(msg);
			}
		}
	};

	public BleService() {
		mHandler = new IncomingHandler(this);
		mMessenger = new Messenger(mHandler);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

	private static class IncomingHandler extends Handler {
		private final WeakReference<BleService> mService;

		public IncomingHandler(BleService service) {
			mService = new WeakReference<BleService>(service);
		}

		@Override
		public void handleMessage(Message msg) {
			BleService service = mService.get();
			if (service != null) {
				switch (msg.what) {
					case MSG_REGISTER:
						service.mClients.add(msg.replyTo);
						Log.d(TAG, "Registered");
						break;
					case MSG_UNREGISTER:
						service.mClients.remove(msg.replyTo);
						if (service.mState == State.CONNECTED && service.mGatt[0] != null) {
							service.mGatt[0].disconnect();
						}
						Log.d(TAG, "Unegistered");
						break;
					case MSG_START_SCAN:
						service.startScan();
						Log.d(TAG, "Start Scan");
						break;
					case MSG_DEVICE_CONNECT:
						service.connect((String) msg.obj);
						break;
					case MSG_DEVICE_DISCONNECT:
						if (service.mState == State.CONNECTED && service.mGatt[0] != null) {
							service.mGatt[0].disconnect();
						}
						break;
					default:
						super.handleMessage(msg);
				}
			}
		}
	}

	private void startScan() {
		mDevices.clear();
		setState(State.SCANNING);
		if (mBluetooth == null) {
			BluetoothManager bluetoothMgr = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
			mBluetooth = bluetoothMgr.getAdapter();
		}
		if (mBluetooth == null || !mBluetooth.isEnabled()) {
			setState(State.BLUETOOTH_OFF);
		} else {
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					if (mState == State.SCANNING) {
						mBluetooth.stopLeScan(BleService.this);
						setState(State.IDLE);
					}
				}
			}, SCAN_PERIOD);
			mBluetooth.startLeScan(this);
		}
	}

	@Override
	public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
		if (device != null && !mDevices.containsValue(device) && device.getName() != null && device.getName().equals("SimpleBLEPeripheral") ) {
			mDevices.put(device.getAddress(), device);
			Message msg = Message.obtain(null, MSG_DEVICE_FOUND);
			if (msg != null) {
				Bundle bundle = new Bundle();
				String[] addresses = mDevices.keySet().toArray(new String[mDevices.size()]);
				bundle.putStringArray(KEY_MAC_ADDRESSES, addresses);
				msg.setData(bundle);
				sendMessage(msg);
			}
			Log.d(TAG, "Added " + device.getName() + ": " + device.getAddress());
		}
	}

	public void connect(final String macAddress) {
		String index = "mindex";
		Log.i(index, DeviceListFragment.mDevices.length+"");
		for (int i = 0; i < DisplayViewActivity.AP; i++) {
			for (int j = 0; j < DisplayViewActivity.ANUMBER; j++) {
				temp[i][j]=0;
			}
		}
		for ( mIndex = 0; mIndex < DeviceListFragment.mDevices.length; mIndex++) {
			Thread thread=new MyThread(DeviceListFragment.mDevices[mIndex]);
			thread.start();
		}
	}
	
	class MyThread extends Thread {
		private String device=null;
		
		public MyThread(String device){
			this.device=device;
		}
		
		@Override
		public void run() {
		// TODO Auto-generated method stub
			
			BluetoothDevice device = mDevices.get(this.device);
			if (device != null) {
				while (mGatt[mIndex]==null) {
					mGatt[mIndex] = device.connectGatt(BleService.this, true, mGattCallback);		
				}
				
			}
		}
	}
	
	

	private void subscribe(BluetoothGatt gatt) {
		BluetoothGattService coordinatesService = gatt.getService(UUID_COORDINATES_SERVICE);
        gatt.readRemoteRssi();

        gatt.discoverServices();
        

		
		if (coordinatesService != null) {
			BluetoothGattCharacteristic coordinatesCharacteristic = coordinatesService.getCharacteristic(UUID_COORDINATES_DATA);
            gatt.readCharacteristic(coordinatesCharacteristic);
		/*	BluetoothGattCharacteristic humidityConf = coordinatesService.getCharacteristic(UUID_HUMIDITY_CONF);
			if (coordinatesCharacteristic != null && humidityConf != null) {
				BluetoothGattDescriptor config = coordinatesCharacteristic.getDescriptor(UUID_CCC);
				if (config != null) {
					gatt.setCharacteristicNotification(coordinatesCharacteristic, true);
					humidityConf.setValue(ENABLE_SENSOR);
					write(humidityConf);
					config.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
					write(config);
				}
			}*/
		}
	}

	private synchronized void write(Object o) {
		if (sWriteQueue.isEmpty() && !sIsWriting) {
			doWrite(o);
		} else {
			sWriteQueue.add(o);
		}
	}

	private synchronized void nextWrite() {
		if (!sWriteQueue.isEmpty() && !sIsWriting) {
			doWrite(sWriteQueue.poll());
		}
	}

	private synchronized void doWrite(Object o) {
		if (o instanceof BluetoothGattCharacteristic) {
			sIsWriting = true;
			mGatt[mIndex].writeCharacteristic((BluetoothGattCharacteristic) o);
		} else if (o instanceof BluetoothGattDescriptor) {
			sIsWriting = true;
			mGatt[mIndex].writeDescriptor((BluetoothGattDescriptor) o);
		} else {
			nextWrite();
		}
	}

	private void setState(State newState) {
		if (mState != newState) {
			mState = newState;
			Message msg = getStateMessage();
			if (msg != null) {
				sendMessage(msg);
			}
		}
	}

	private Message getStateMessage() {
		Message msg = Message.obtain(null, MSG_STATE_CHANGED);
		if (msg != null) {
			msg.arg1 = mState.ordinal();
		}
		return msg;
	}

	private void sendMessage(Message msg) {
		for (int i = mClients.size() - 1; i >= 0; i--) {
			Messenger messenger = mClients.get(i);
			if (!sendMessage(messenger, msg)) {
				mClients.remove(messenger);
			}
		}
	}

	private boolean sendMessage(Messenger messenger, Message msg) {
		boolean success = true;
		try {
			messenger.send(msg);
		} catch (RemoteException e) {
			Log.w(TAG, "Lost connection to client", e);
			success = false;
		}
		return success;
	}

	private static Integer shortUnsignedAtOffset(BluetoothGattCharacteristic characteristic, int offset) {
		Integer lowerByte = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
		Integer upperByte = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 1);

		return (upperByte << 8) + lowerByte;
	}
}

