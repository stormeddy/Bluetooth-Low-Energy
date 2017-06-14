package com.android.ble18;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;

import android.R.integer;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.android.ble18.R;

public class DisplayViewActivity extends Activity {
	
	private float[] rssi={-50.0f,-50.0f,-50.0f,-50.0f};
	
	public static final int NUMBER=MessureActivity.XNUMBER*MessureActivity.YNUMBER;
	public static final int K=4;
	public static final int AP=4;
	public static final int ANUMBER=50;
	public static final float Q=2.0f;
	public static float averageXPixel=0.0f,averageYPixel=0.0f;

	Quick paQuick =new Quick();
	String[] dataStrings = null;
	
	FrameLayout ll;
	Timer timer=new Timer();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		/*setContentView(R.layout.displayview);
		 ll=(FrameLayout)findViewById(R.id.frameLayout);*/
		
		StringBuffer sBuffer = new StringBuffer();
		//String[] dataStrings = null;
		String path = Environment.getExternalStorageDirectory().getPath();
		File file = new File(path + "/" + "datastorage");
		try {
			FileInputStream fis = new FileInputStream(file);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					fis));
			String partString = null;
			while ((partString = reader.readLine()) != null) {
				sBuffer.append(partString);
			}
			fis.close();
			
			dataStrings = sBuffer.toString().split(" ");

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}//读取数据
		
		float[] data = new float[dataStrings.length];
		float[] compareData=new float[NUMBER];
		for (int i = 0; i < dataStrings.length; i++) {
			data[i] = Float.valueOf(dataStrings[i]);
		}
		
	/*	int newTemp[][]=new int [AP][ANUMBER];
		
		for (int i = 0; i < AP; i++) {
			for (int j = 0; j < ANUMBER; j++) {
				newTemp[i][j]=BleService.temp[i][j];
			}			
		}
		for (int i = 0; i < AP; i++) {
			paQuick.sort(newTemp[i], 0, newTemp[i].length-1);
		}
		for (int i = 0; i < MessureActivity.rssiAverage.length; i++) {
			MessureActivity.rssiAverage[i]=(newTemp[i][ANUMBER/2]+newTemp[i][(ANUMBER-1)/2])/2.0f;
		}*/
		//下面求的是均值
		float rssiSum[] = new float[AP];		
		for (int i = 0; i < rssiSum.length; i++) {
			rssiSum[i]=0.0f;
		}

		for (int i = 0; i < AP; i++) {
			for (int j = 0; j < ANUMBER; j++) {
				rssiSum[i]+=BleService.temp[i][j];
			}			
		}
		for (int i = 0; i < MessureActivity.rssiAverage.length; i++) {
			MessureActivity.rssiAverage[i]=rssiSum[i]/DisplayViewActivity.ANUMBER;
		}
		/*MessureActivity.rssiAverage[2]=MessureActivity.rssiAverage[0];
		MessureActivity.rssiAverage[3]=MessureActivity.rssiAverage[1];*/
		
		for (int i = 0; i < NUMBER ; i=i+1) {
			float temp= (float) (Math.pow(Math.abs(MessureActivity.rssiAverage[0]-data[AP*i]), Q)+Math.pow(Math.abs(MessureActivity.rssiAverage[1]-data[AP*i+1]), Q)
					+Math.pow(Math.abs(MessureActivity.rssiAverage[2]-data[AP*i+2]), Q)+Math.pow(Math.abs(MessureActivity.rssiAverage[3]-data[AP*i+3]), Q));
			compareData[i]=(float) Math.pow(temp, 1/Q);
		}
		
		//找出三个最小的数(K=3),它们的下标为index[]前三个数
		/*		if (min[0]<min[1]) {
		if (min[1]<min[2]) {
			;
		}else if (min[0]<min[2]) {
			min[2]=compareData[1];min[1]=compareData[2];
			index[1]=2;index[2]=1;				
		}
	}else {
		if (min[0]<min[2]) {
			min[0]=compareData[1];min[1]=compareData[0];
			index[0]=1;index[1]=0;
		}else if (condition) {
			
		}
	}*/
		int [] index=new int [NUMBER];
		for (int i = 0; i < NUMBER; i++) {
			index[i]=i;
		}
		for (int i = 0; i < NUMBER-1; i++) {
			for (int j = 0; j < NUMBER-1-i; j++) {
				if (compareData[j]>compareData[j+1]) {
					float temp=compareData[j];
					compareData[j]=compareData[j+1];
					compareData[j+1]=temp;
					int tempIndex=index[j];
					index[j]=index[j+1];
					index[j+1]=tempIndex;
				}
			}
		}
		float xPixel[]=new float[K];
		float yPixel[]=new float[K];
		float xSumPixel=0.0f,ySumPixel=0.0f,sumCompareData=0.0f;
		for (int i = 0; i < K; i++) {
			xPixel[i]=MessureActivity.XINITIAL+index[i]%MessureActivity.XNUMBER*MessureActivity.XLENGTH;
			yPixel[i]=MessureActivity.YINITIAL+index[i]/MessureActivity.XNUMBER*MessureActivity.YLENGTH;
		}
		for (int i = 0; i < K; i++) {
			xSumPixel+=xPixel[i]/compareData[i];
			sumCompareData+=1/compareData[i];
			ySumPixel+=yPixel[i]/compareData[i];
			
		}
		averageXPixel=xSumPixel/sumCompareData;
		averageYPixel=ySumPixel/sumCompareData;
				
		setContentView(new MyView(this));
		
		timer.schedule(task, 1000, 1000);
	}
	
	   Handler handler = new Handler() {  
	        public void handleMessage(Message msg) {  
	            if (msg.what == 20) {  
	            	float[] data = new float[dataStrings.length];
	        		float[] compareData=new float[NUMBER];
	        		for (int i = 0; i < dataStrings.length; i++) {
	        			data[i] = Float.valueOf(dataStrings[i]);
	        		}
	        		

	        		
	        		/*int newTemp[][]=new int [AP][ANUMBER];
	        		
	        		for (int i = 0; i < AP; i++) {
	        			for (int j = 0; j < ANUMBER; j++) {
	        				newTemp[i][j]=BleService.temp[i][j];
	        			}			
	        		}
	        		for (int i = 0; i < AP; i++) {
	        			paQuick.sort(newTemp[i], 0, newTemp[i].length-1);
	        		}
	        		for (int i = 0; i < MessureActivity.rssiAverage.length; i++) {
	        			MessureActivity.rssiAverage[i]=(newTemp[i][ANUMBER/2]+newTemp[i][(ANUMBER-1)/2])/2.0f;
	        		}*/
	        		//下面求的是均值
	        		float rssiSum[] = new float[AP];		
	        		for (int i = 0; i < rssiSum.length; i++) {
	        			rssiSum[i]=0.0f;
	        		}
	        		for (int i = 0; i < AP; i++) {
	        			for (int j = 0; j < ANUMBER; j++) {
	        				rssiSum[i]+=BleService.temp[i][j];
	        			}			
	        		}
	        		for (int i = 0; i < MessureActivity.rssiAverage.length; i++) {
	        			MessureActivity.rssiAverage[i]=rssiSum[i]/DisplayViewActivity.ANUMBER;
	        		}
	        		/*MessureActivity.rssiAverage[2]=MessureActivity.rssiAverage[0];
	        		MessureActivity.rssiAverage[3]=MessureActivity.rssiAverage[1];*/
	        		
	        		for (int i = 0; i < NUMBER ; i=i+1) {
	        			float temp= (float) (Math.pow(Math.abs(MessureActivity.rssiAverage[0]-data[AP*i]), Q)+Math.pow(Math.abs(MessureActivity.rssiAverage[1]-data[AP*i+1]), Q)
	        					+Math.pow(Math.abs(MessureActivity.rssiAverage[2]-data[AP*i+2]), Q)+Math.pow(Math.abs(MessureActivity.rssiAverage[3]-data[AP*i+3]), Q));
	        			compareData[i]=(float) Math.pow(temp, 1/Q);
	        		}
	        		
	        		//找出三个最小的数(K=3),它们的下标为index[]前三个数
	        		int [] index=new int [NUMBER];
	        		for (int i = 0; i < NUMBER; i++) {
	        			index[i]=i;
	        		}
	        /*		if (min[0]<min[1]) {
	        			if (min[1]<min[2]) {
	        				;
	        			}else if (min[0]<min[2]) {
	        				min[2]=compareData[1];min[1]=compareData[2];
	        				index[1]=2;index[2]=1;				
	        			}
	        		}else {
	        			if (min[0]<min[2]) {
	        				min[0]=compareData[1];min[1]=compareData[0];
	        				index[0]=1;index[1]=0;
	        			}else if (condition) {
	        				
	        			}
	        		}*/
	        		for (int i = 0; i < NUMBER-1; i++) {
	        			for (int j = 0; j < NUMBER-1-i; j++) {
	        				if (compareData[j]>compareData[j+1]) {
	        					float temp=compareData[j];
	        					compareData[j]=compareData[j+1];
	        					compareData[j+1]=temp;
	        					int tempIndex=index[j];
	        					index[j]=index[j+1];
	        					index[j+1]=tempIndex;
	        				}
	        			}
	        		}
	        		float xPixel[]=new float[K];
	        		float yPixel[]=new float[K];
	        		float xSumPixel=0.0f,ySumPixel=0.0f,sumCompareData=0.0f;
	        		for (int i = 0; i < K; i++) {
	        			xPixel[i]=MessureActivity.XINITIAL+index[i]%MessureActivity.XNUMBER*MessureActivity.XLENGTH;
	        			yPixel[i]=MessureActivity.YINITIAL+index[i]/MessureActivity.XNUMBER*MessureActivity.YLENGTH;
	        		}
	        		for (int i = 0; i < K; i++) {
	        			xSumPixel+=xPixel[i]/compareData[i];
	        			sumCompareData+=1/compareData[i];
	        			ySumPixel+=yPixel[i]/compareData[i];
	        			
	        		}
	        		averageXPixel=xSumPixel/sumCompareData;
	        		averageYPixel=ySumPixel/sumCompareData;
	        				
	        		setContentView(new MyView(DisplayViewActivity.this));
	            }  
	            super.handleMessage(msg);  
	        };  
	    };  
	    
	TimerTask task=new TimerTask() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			
			 Message message = new Message();  
	         message.what = 20;  
	         handler.sendMessage(message);  
			
		}
	};
	
	public class MyView extends View{

		public MyView(Context context) {
			super(context);
			// TODO Auto-generated constructor stub
		}
		
		@Override
		protected void onDraw(Canvas canvas) {
			// TODO Auto-generated method stub
			super.onDraw(canvas);
			canvas.drawColor(Color.WHITE);
			Paint paint=new Paint();
			paint.setColor(Color.BLACK);
			paint.setStyle(Style.STROKE);
			paint.setStrokeWidth(10);
			DisplayMetrics metrics=new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(metrics);
			float paddingwidth=metrics.widthPixels*0.1f;//原点坐标
			
			float width=metrics.widthPixels-2*paddingwidth;
			float height=width*(MessureActivity.YNUMBER-1)/(MessureActivity.XNUMBER-1);	
			TypedArray actionbarSizeTypedArray = DisplayViewActivity.this.obtainStyledAttributes(new int[] {  
			        android.R.attr.actionBarSize  
			});  
			  
			float actionBarHeight = actionbarSizeTypedArray.getDimension(0, 0);  
			float paddingheight=(metrics.heightPixels-height-actionBarHeight)/2;//actionBarHeight为actionbar高度
			//原点坐标为(paddingwidth,paddingheight)
			canvas.drawRect(paddingwidth, paddingheight, width+paddingwidth, height+paddingheight, paint);			
			canvas.drawPoint(
					width
							* averageXPixel
							/ ((MessureActivity.XNUMBER-1) * MessureActivity.XLENGTH)+paddingwidth,
					height
							* averageYPixel
							/ ((MessureActivity.YNUMBER-1) * MessureActivity.YLENGTH)+paddingheight,
					paint);
			Paint paintText=new Paint();
			paintText.setTextSize(40);
			canvas.drawText("原点", paddingwidth, paddingheight+40, paintText);
			canvas.drawText("横坐标："+String.valueOf(averageXPixel), 30, 60, paintText);
			canvas.drawText("纵坐标："+String.valueOf(averageYPixel), 30, 100, paintText);			
		}
	}
}
