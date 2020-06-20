/**
 * @author K�kay Bal�zs
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
	
	
	//a timerhez sz�ks�ges v�ltoz�, ha false a k�vet�s le van �ll�tva
	private static boolean isTrackingRunning = false;
	
	//timer p�ld�ny �s az eltelt id�
	static Timer timer = new Timer();
	static int elapsedTime = 0;
	static int minutes=0;
	static int hours;
	
	//t�vols�g m�r�s�hez k�t v�ltoz�
	static double fullDistance = 0;	
	static double actualDistance;
	static double fullFormatedDistance;
	
	//felugr� dial�gushoz
	private static final int dialogNumber = 1;
	
	//v�ltoz�k a buttonok akt�v/inakt�v �llapot�hoz
	static boolean startButtonEnabled;
	static boolean stopButtonEnabled;
	static boolean pauseButtonEnabled;
	
	//meg van-e �ll�tva az alkalmaz�s
	static int pause = 0;
	
	//lat �s lon
	private static String lastLatitude;
	private static String lastLongitude;
	
	LocationManager lm;
	boolean gps_enabled = false;
	
	/**
	 * Ez az elj�r�s akkor fut le amikor az activity elindul, be�ll�tja a service intentj�t, az activity layout�t,
	 * a textvieweket �s a gombokat. A btnStart gomb elind�tja a TrackingService-t, amit a btnStop pedig le�ll�tja.
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
        
        // activity ind�t�sakor megn�zi hogy ezek a v�ltoz�k �resek-e, ha nem be�ll�tja �ket a textViewba
        // ez akkor kell ha kil�pt�nk az activityb�l, de a k�vet� service m�g megy
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
        
        // gombok v�ltoz�hoz rendel�se, sharedPref �s aktivit�s be�ll�t�sa valamint az onClickListenerek
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
				if(pause==0){ /* meg�ll�tva */
					isTrackingRunning=false;
					Map.timerOn = false;
					pause = 1;
					Map.pause = 1;
					TrackingService.servicePause = true;
					btnPause.setText("K�vet�s folytat�sa");
					Log.v("TRACKMENU","Pause gomb megnyomva, " + String.valueOf(isTrackingRunning) + ", " +  String.valueOf(pause));
				}else if (pause == 1){ /* folytat�s */
					isTrackingRunning = true;
					Map.timerOn = true;
					pause = 0;
					Map.pause = 0;
					TrackingService.servicePause = false;
					btnPause.setText("K�vet�s meg�ll�t�sa");
					Log.v("TRACKMENU","Pause m�sodszor, folytat�dik a k�vet�s" + String.valueOf(isTrackingRunning) + ", " +  String.valueOf(pause));
				}
			}
		});
        
        
    }
	
	
	public boolean getTrackRunning(){
		return isTrackingRunning;
	}
	
	/**
	 * Az activity bez�r�sakor a gombok aktivit�s�t SETTINGS-xml-be t�roljuk, ez az�rt sz�ks�ges hogy miut�n a felhaszn�l� 
	 * �tl�p a t�rk�pn�zetbe a gombok megtarts�k aktivit�sukat.
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
	 * A broadcastokat figyeli amit a TrackingService k�ld ki, ezekben a broadcastokban j�nnek a 
	 * service �ltal sz�molt adatok (latitude, longitude, distance), ezeket a textviewok-ba rakja. Az oszt�ly m�g
	 * a megtett t�vols�got sz�molja
	 */
	
	public static class Broadcaster extends BroadcastReceiver {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.v("TRACKMENU","Broadcast �zenet be�rkezett");
			
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
	 * Ez a f�ggv�ny az id� m�l�s�t figyeli
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
	 * Ez a handler be�ll�tja a textview textj�t mindig amikor v�ltozik az elapsedTime
	 */
	
	public static Handler mHandler = new Handler(){
		public void handleMessage(Message msg){
			tvTime.setText(String.valueOf(hours) + " �ra, " + String.valueOf(minutes) + " perc, " + String.valueOf(elapsedTime) + " m�sodperc");
		}
	};
	
	/**
	 * A meg�ll�t�s gombra klikkelve felugr� dial�gus met�dusa
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
        				Log.v("TRACKMENU", "service le�ll�t�sa");
        				
        				Map.cleanUp();
        				
        				startButtonEnabled = true;
        				stopButtonEnabled = false;
        				pauseButtonEnabled = false;
        				
        				btnStart.setEnabled(startButtonEnabled);
        				btnStop.setEnabled(stopButtonEnabled);
        				btnPause.setEnabled(pauseButtonEnabled); 
        				
        				btnPause.setText("K�vet�s meg�ll�t�sa");
        				
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

                        /* Nem t�rt�nik semmi, folytat�dik a k�vet�s */
                    }
                })
                .create(); 
		}
		return null;
	}
	
}

