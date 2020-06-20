/**
 * @author Kókay Balázs
 */

package com.kokaybalazs.turaalkalmazas;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

public class TrackingMenu extends Activity{
	
	//TextViewok
	static TextView tvLastLatitude;
	static TextView tvLastLongitude;
	static TextView tvTime;
	static TextView tvDistance;
	
	
	//a timerhez szükséges változó, ha false a követés le van állítva
	private static boolean isTrackingRunning = false;
	
	//timer példány és az eltelt idõ
	static Timer timer = new Timer();
	static int elapsedTime = 0;
	static int minutes=0;
	static int hours;
	
	//távolság méréséhez két változó
	static double fullDistance = 0;	
	static double actualDistance;
	static double fullFormatedDistance;
	
	//felugró dialógushoz
	private static final int dialogNumber = 1;
	
	//változók a buttonok aktív/inaktív állapotához
	static boolean startButtonEnabled;
	static boolean stopButtonEnabled;
	static boolean pauseButtonEnabled;
	
	//meg van-e állítva az alkalmazás
	static int pause = 0;
	
	//lat és lon
	private static String lastLatitude;
	private static String lastLongitude;
	
	LocationManager lm;
	boolean gps_enabled = false;
	
	/**
	 * Ez az eljárás akkor fut le amikor az activity elindul, beállítja a service intentjét, az activity layoutát,
	 * a textvieweket és a gombokat. A btnStart gomb elindítja a TrackingService-t, amit a btnStop pedig leállítja.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
    {
		final Intent trackServiceIntent = new Intent(getApplicationContext(), TrackingService.class);
		final SharedPreferences sharedSettings = getSharedPreferences("SETTINGS", 0);
		
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking_menu);
        
        tvDistance = (TextView) findViewById(R.id.tvDistance);
        tvTime = (TextView) findViewById(R.id.tvTime);
        
        tvLastLatitude = (TextView) findViewById(R.id.tvLastLatitude);
        tvLastLongitude = (TextView) findViewById(R.id.tvLastLongitude);
        
        // activity indításakor megnézi hogy ezek a változók üresek-e, ha nem beállítja õket a textViewba
        // ez akkor kell ha kiléptünk az activitybõl, de a követõ service még megy
        if(fullFormatedDistance != 0)
        {
          tvDistance.setText(String.valueOf(fullFormatedDistance + " km"));
        }
        if(lastLatitude != null || lastLatitude!= ""){
        	tvLastLatitude.setText(lastLatitude);
        }
        
        if(lastLongitude != null || lastLongitude != ""){
        	tvLastLongitude.setText(lastLongitude);
        }
        
        lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        
        // gombok változóhoz rendelése, sharedPref és aktivitás beállítása valamint az onClickListenerek
        final Button btnStart = (Button) findViewById(R.id.btnStartListen);
        final Button btnStop = (Button) findViewById(R.id.btnStopListen);
        final Button btnPause = (Button) findViewById(R.id.btnPauseListen);
        
        startButtonEnabled = sharedSettings.getBoolean("startbutton", true);
        stopButtonEnabled = sharedSettings.getBoolean("stopbutton", false);
        pauseButtonEnabled = sharedSettings.getBoolean("pausebutton",false);
        
        btnStart.setEnabled(startButtonEnabled);
        btnStop.setEnabled(stopButtonEnabled);
        btnPause.setEnabled(pauseButtonEnabled);
        
        btnStart.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
				if(gps_enabled){
					tvDistance.setText("0 km");
					tvLastLatitude.setText("");
					tvLastLongitude.setText("");
						
					startService(trackServiceIntent);
						
					startButtonEnabled = false;
					stopButtonEnabled = true;
					pauseButtonEnabled = true;
						
					btnStart.setEnabled(startButtonEnabled);
					btnPause.setEnabled(pauseButtonEnabled);
					btnStop.setEnabled(stopButtonEnabled);
					
					Map.newTrack();
					Map.timerOn = true;				
					isTrackingRunning = true;
					
					startTimer();
					Map.startTimer();
				}
				else {
					Toast.makeText(getApplicationContext(), R.string.toastGpsEnabled, Toast.LENGTH_LONG).show();
				}
				}
        	});
        
        btnStop.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showDialog(dialogNumber);
			}
		});
        
        btnPause.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(pause==0){ /* megállítva */
					isTrackingRunning=false;
					Map.timerOn = false;
					pause = 1;
					Map.pause = 1;
					TrackingService.servicePause = true;
					btnPause.setText("Követés folytatása");
					Log.v("TRACKMENU","Pause gomb megnyomva, " + String.valueOf(isTrackingRunning) + ", " +  String.valueOf(pause));
				}else if (pause == 1){ /* folytatás */
					isTrackingRunning = true;
					Map.timerOn = true;
					pause = 0;
					Map.pause = 0;
					TrackingService.servicePause = false;
					btnPause.setText("Követés megállítása");
					Log.v("TRACKMENU","Pause másodszor, folytatódik a követés" + String.valueOf(isTrackingRunning) + ", " +  String.valueOf(pause));
				}
			}
		});
        
        
    }
	
	
	public boolean getTrackRunning(){
		return isTrackingRunning;
	}
	
	/**
	 * Az activity bezárásakor a gombok aktivitását SETTINGS-xml-be tároljuk, ez azért szükséges hogy miután a felhasználó 
	 * átlép a térképnézetbe a gombok megtartsák aktivitásukat.
	 */
	
	@Override
	protected void onStop() {
		super.onStop();
		
		SharedPreferences sharedSettings = getSharedPreferences("SETTINGS", 0);
		SharedPreferences.Editor editor = sharedSettings.edit();
		
		editor.putBoolean("startbutton", startButtonEnabled);
		editor.putBoolean("stopbutton", stopButtonEnabled);
		editor.putBoolean("pausebutton", pauseButtonEnabled);
		
		editor.commit();
	}
	
	/**
	 * A broadcastokat figyeli amit a TrackingService küld ki, ezekben a broadcastokban jönnek a 
	 * service által számolt adatok (latitude, longitude, distance), ezeket a textviewok-ba rakja. Az osztály még
	 * a megtett távolságot számolja
	 */
	
	public static class Broadcaster extends BroadcastReceiver {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.v("TRACKMENU","Broadcast üzenet beérkezett");
			
			actualDistance = intent.getExtras().getDouble("Distance");
			fullDistance += actualDistance;
			
			lastLatitude = intent.getExtras().getString("LastLatitude");
			lastLongitude = intent.getExtras().getString("LastLongitude");
			tvLastLatitude.setText(lastLatitude);
			tvLastLongitude.setText(lastLongitude);
			
			DecimalFormat decform = new DecimalFormat("#.##");
			fullFormatedDistance = Double.valueOf(decform.format(fullDistance/1000));
			tvDistance.setText(String.valueOf(fullFormatedDistance) + " km");
		}
		
	}
	
	/**
	 * Ez a függvény az idõ múlását figyeli
	 */
	
	static void startTimer(){
		
		timer.scheduleAtFixedRate(new TimerTask(){

			@Override
			public void run() {
				if(isTrackingRunning){
					elapsedTime+=1;
					mHandler.obtainMessage(1).sendToTarget();
				}
				else if (pause == 0){
					elapsedTime = 0;
					cancel();
				}
				
				if (elapsedTime/60 == 1){
					minutes+=1;
					elapsedTime = 0;
					mHandler.obtainMessage(1).sendToTarget();
				}
				
				if (minutes / 60 == 1){
					hours+=1;
					minutes=0;
					mHandler.obtainMessage(1).sendToTarget();
				}
				
			}
			
		}, 0, 1000);
	}
	
	/**
	 * Ez a handler beállítja a textview textjét mindig amikor változik az elapsedTime
	 */
	
	public static Handler mHandler = new Handler(){
		public void handleMessage(Message msg){
			tvTime.setText(String.valueOf(hours) + " óra, " + String.valueOf(minutes) + " perc, " + String.valueOf(elapsedTime) + " másodperc");
		}
	};
	
	/**
	 * A megállítás gombra klikkelve felugró dialógus metódusa
	 */
	
	protected Dialog onCreateDialog(int id) {
		
		final Button btnStart = (Button) findViewById(R.id.btnStartListen);
        final Button btnStop = (Button) findViewById(R.id.btnStopListen);
        final Button btnPause = (Button) findViewById(R.id.btnPauseListen);
		
		switch(id){			
		case dialogNumber:
            return new AlertDialog.Builder(this)
                .setIcon(R.drawable.alert_dialog_icon)
                .setTitle(R.string.lblAlertDialog)
                .setPositiveButton(R.string.lblYes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	
                    	final Intent trackServiceIntent = new Intent(getApplicationContext(), TrackingService.class);

                    	stopService(trackServiceIntent);	
        				Log.v("TRACKMENU", "service leállítása");
        				
        				Map.cleanUp();
        				
        				startButtonEnabled = true;
        				stopButtonEnabled = false;
        				pauseButtonEnabled = false;
        				
        				btnStart.setEnabled(startButtonEnabled);
        				btnStop.setEnabled(stopButtonEnabled);
        				btnPause.setEnabled(pauseButtonEnabled); 
        				
        				btnPause.setText("Követés megállítása");
        				
        				isTrackingRunning = false;
        				        				
        				actualDistance = 0;
        				fullDistance = 0;
        				fullFormatedDistance = 0;
        				
        				elapsedTime=0;
        				hours=0;
        				minutes=0;
        				
        				pause=0;
        				
        				TrackingService.servicePause = false;
                    }
                })
                .setNegativeButton(R.string.lblNo, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        /* Nem történik semmi, folytatódik a követés */
                    }
                })
                .create(); 
		}
		return null;
	}
	
}

