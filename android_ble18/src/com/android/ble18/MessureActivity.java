package com.android.ble18;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.xml.transform.Templates;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.ble18.R;

public class MessureActivity extends Activity {
	public static final int XNUMBER = 5;
	public static final int YNUMBER = 8;
	public static final int NUMBER = 4;
	public static final float XLENGTH = 1.0f;
	public static final float YLENGTH = 1.0f;
	public static final float XINITIAL = 0.0f;
	public static final float YINITIAL = 0.0f;
     String path = Environment.getExternalStorageDirectory().getPath();
     File fileout = new File(path + "/" + "database");
	static float rssiAverage[]=new float[DisplayViewActivity.AP];
	private int xi = 0;
	private int yi = 0;
	private boolean judge=false;//是否写到最后一个数据
	Quick paQuick =new Quick();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.messure);
		final EditText xEditText = (EditText) findViewById(R.id.xcoordinate);
		final EditText yEditText = (EditText) findViewById(R.id.ycoordinate);
		final TextView rssiTextView0 = (TextView) findViewById(R.id.rssi0);
		final TextView rssiTextView1 = (TextView) findViewById(R.id.rssi1);
		final TextView rssiTextView2 = (TextView) findViewById(R.id.rssi2);
		final TextView rssiTextView3 = (TextView) findViewById(R.id.rssi3);
		final TextView rssiTextView[]={rssiTextView0,rssiTextView1,rssiTextView2,rssiTextView3};
		TextView rssiText0=(TextView) findViewById(R.id.rssiText0);
		TextView rssiText1=(TextView) findViewById(R.id.rssiText1);
		TextView rssiText2=(TextView) findViewById(R.id.rssiText2);
		TextView rssiText3=(TextView) findViewById(R.id.rssiText3);
		TextView rssiText[]={rssiText0,rssiText1,rssiText2,rssiText3};
		for (int i = 0; i < DeviceListFragment.mDevices.length; i++) {
			if (DeviceListFragment.mDevices[i]!="") {
				rssiText[i].setText(DeviceListFragment.mDevices[i]);
			}
		}
		Button messureButton = (Button) findViewById(R.id.messure);
		Button nextButton = (Button) findViewById(R.id.next);
		Button completeButton = (Button) findViewById(R.id.complete);
		Button backButton=(Button)findViewById(R.id.back);
		//final TextView stringTextView=(TextView)findViewById(R.id.stringbuffer);
		xEditText.setText(String.valueOf(XINITIAL));
		yEditText.setText(String.valueOf(YINITIAL));
		
		System.out.println(path);
		final File file = new File(path + "/" + "datastorage");

		messureButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
			/*	int newTemp[][]=new int [DisplayViewActivity.AP][DisplayViewActivity.ANUMBER];
        		
        		for (int i = 0; i < DisplayViewActivity.AP; i++) {
        			for (int j = 0; j < DisplayViewActivity.ANUMBER; j++) {
        				newTemp[i][j]=BleService.temp[i][j];
        			}			
        		}
        		for (int i = 0; i < DisplayViewActivity.AP; i++) {
        			paQuick.sort(newTemp[i], 0, newTemp[i].length-1);
        		}
        		for (int i = 0; i < MessureActivity.rssiAverage.length; i++) {
        			MessureActivity.rssiAverage[i]=(newTemp[i][DisplayViewActivity.ANUMBER/2]+newTemp[i][(DisplayViewActivity.ANUMBER-1)/2])/2.0f;
        		}*/
				float rssiSum[] = new float[DisplayViewActivity.AP];
				
				for (int i = 0; i < rssiSum.length; i++) {
					rssiSum[i]=0.0f;
				}

				for (int i = 0; i < DisplayViewActivity.AP; i++) {
					for (int j = 0; j < DisplayViewActivity.ANUMBER; j++) {
						rssiSum[i]+=BleService.temp[i][j];
					}
					
				}
				for (int i = 0; i < rssiAverage.length; i++) {
					rssiAverage[i]=rssiSum[i]/DisplayViewActivity.ANUMBER;
				}
				/*rssiTextView0.setText(String.valueOf(rssiAverage[0]));
				rssiTextView1.setText(String.valueOf(rssiAverage[1]));
				rssiTextView2.setText(String.valueOf(rssiAverage[2]));
				rssiTextView3.setText(String.valueOf(rssiAverage[3]));*/
				for (int i = 0; i < rssiTextView.length; i++) {
					rssiTextView[i].setText(String.valueOf(rssiAverage[i]));
				}
			}
		});
		

		
		
		nextButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				if (!judge) {//未测到最后一个点
					try {
						FileOutputStream fos = new FileOutputStream(file, true);
						FileOutputStream fosdata = new FileOutputStream(fileout, true);
						fosdata.write(String.valueOf("x:"+(XINITIAL + xi * XLENGTH)+" ").getBytes());
						fosdata.write(String.valueOf("y:"+(YINITIAL + yi * YLENGTH)+"\n").getBytes());
						for (int i = 0; i < DisplayViewActivity.AP; i++) {
							for (int j = 0; j < DisplayViewActivity.ANUMBER; j++) {
								fosdata.write(String.valueOf(DeviceListFragment.mDevices[i].substring(DeviceListFragment.mDevices[i].length()-2)+" "+BleService.temp[i][j]+"\n").getBytes());
							}
							
						}
						
						fosdata.close();
						for (int i = 0; i < rssiAverage.length; i++) {
							fos.write((String.valueOf(rssiAverage[i]) + " ").getBytes());
						}
						
						fos.close();
					} catch (FileNotFoundException e) {
						// TODO: handle exception
						e.printStackTrace();
					} catch (IOException e) {
						// TODO: handle exception
						e.printStackTrace();
					}
				}
				if (xi < XNUMBER - 1 && yi < YNUMBER) {
					xi++;
				} else if (xi == XNUMBER - 1 && yi < YNUMBER - 1) {
					xi = 0;
					yi++;
				} else if (xi == XNUMBER - 1 && yi == YNUMBER - 1) {
					Toast.makeText(MessureActivity.this, "已经测完所有标定点",
							Toast.LENGTH_SHORT).show();
					judge=true;
				}
				xEditText.setText(String.valueOf(XINITIAL + xi * XLENGTH));
				yEditText.setText(String.valueOf(YINITIAL + yi * YLENGTH));
			}

		});
		
		backButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub	
				judge=false;//重新测XNUMBER*YNUMBER个
				Intent intent=getIntent();
				setResult(2,intent);
				for (int i = 0; i < DisplayViewActivity.AP; i++) {
					BleService.mGatt[i].disconnect();
					BleService.mGatt[i].close();
					BleService.mGatt[i]=null;
					BleService.mGatt[i].disconnect();
				}
			
				finish();				
			}
		});
		
		completeButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				StringBuffer sBuffer=new StringBuffer();				
				String[] dataStrings = null;
				try {
					FileInputStream fis=new FileInputStream(file);
					BufferedReader reader=new BufferedReader(new InputStreamReader(fis));
					String partString=null;
					while ((partString=reader.readLine())!=null) {
						sBuffer.append(partString);
					}
					fis.close();
					//stringTextView.setText(sBuffer.toString());
					dataStrings=sBuffer.toString().split(" ");
					
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			/*	float[]	data=new float[dataStrings.length];
				for (int i = 0; i < dataStrings.length; i++) {
					data[i]=Float.valueOf(dataStrings[i]);
				}*/
				Intent intent=new Intent();
				intent.setClass(MessureActivity.this, DisplayViewActivity.class);
				startActivity(intent);
			
				
				
			}
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		int id=item.getItemId();
		if (id==R.id.action_refresh) {
			xi=0;
			yi=0;
			judge=false;
			EditText xEditText = (EditText) findViewById(R.id.xcoordinate);
			EditText yEditText = (EditText) findViewById(R.id.ycoordinate);
			xEditText.setText(String.valueOf(XINITIAL + xi * XLENGTH));
			yEditText.setText(String.valueOf(YINITIAL + yi * YLENGTH));
		}
		return super.onOptionsItemSelected(item);
	}
}
