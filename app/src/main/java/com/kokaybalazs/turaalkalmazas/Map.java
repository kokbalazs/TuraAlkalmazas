/**
 * @author Kókay Balázs
 */

package com.kokaybalazs.turaalkalmazas;

import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.PathOverlay;
import org.osmdroid.views.overlay.TilesOverlay;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;


public class Map extends Activity {

    private static ZoomLevelMapView mMapView;
    private static IMapController mMapController;
    private TilesOverlay mTilesOverlay;
    private MapTileProviderBasic mProvider;
    private ITileSource mCustomTileSource;
    private static PathOverlay mPathOverlay;
    
    //menük    
    public static final int TRACKMENU = Menu.FIRST;
    public static final int TRACKLIST = TRACKMENU + 1;
    public static final int DISTANCETHERE = TRACKLIST + 1;
    public static final int INFOSCREEN = DISTANCETHERE + 1;
    
    static double latitude;
    static double longitude;
    
    static double distance;
    static double fulldistance = 0;
    static double formatedDistance;
    
    static Timer timer = new Timer();
    static boolean timerOn = false;
	static int elapsedTime = 0;
	static int minutes=0;
	static int hours;
	
	static int pause = 0;
    
    private static String DBNAME = TrackingService.getDbName();
    private static String TABLE_NAME = TrackingService.getTableName();
    
    static String latitudeQuery = "SELECT latitude FROM " + TABLE_NAME + " WHERE _id in (SELECT COUNT(_id) FROM " + TABLE_NAME + ")";
	static String longitudeQuery = "SELECT longitude FROM " + TABLE_NAME + " WHERE _id in (SELECT COUNT(_id) FROM " + TABLE_NAME + ")";
	
	static TextView tvMapDistance;
	static TextView tvMapDuration;
	
	static String globalUrl;
    

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
    	String URL = "http://map.turistautak.hu/tiles/turistautak/";
    	setUrl(URL);
               
        setContentView(R.layout.activity_map);
            
        mMapView = (ZoomLevelMapView) findViewById(R.id.mapview);
        
        tvMapDuration = (TextView) findViewById(R.id.tvMapDuration);
        tvMapDuration.setBackgroundColor(Color.BLACK);
        tvMapDuration.setVisibility(TextView.GONE);
        
        tvMapDistance = (TextView) findViewById(R.id.tvMapDistance);
        tvMapDistance.setBackgroundColor(Color.BLACK);
        tvMapDistance.setVisibility(TextView.GONE);
                
        //mMapView.setBuiltInZoomControls(true);
        mMapController = mMapView.getController();
        mMapController.setZoom(13);
        GeoPoint geoPecs = new GeoPoint(46.070833,18.233056);
        mMapController.setCenter(geoPecs);
        
        mProvider = new MapTileProviderBasic(getApplicationContext());
        mCustomTileSource = new XYTileSource("Turaterkep", null, 13, 15, 256, ".png", URL);
        mProvider.setTileSource(mCustomTileSource);
        mTilesOverlay = new TilesOverlay(mProvider,this.getBaseContext());
        
        mPathOverlay = new PathOverlay(Color.BLUE, this);
        mPathOverlay.getPaint().setStrokeWidth(8.0f);
        
        mMapView.getOverlays().add(mTilesOverlay);
        
    }
    
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	 menu.add(0, TRACKMENU, 1, R.string.menuItemTrackMenu);
		 menu.add(0, TRACKLIST, 2, R.string.menuItemTrackList);
		 menu.add(0, DISTANCETHERE, 3, R.string.menuItemDistanceFromHere);
		 menu.add(0, INFOSCREEN, 4, R.string.menuItemInfoScreen);
		 //menu.add("Útszámolás innen");
		 return super.onCreateOptionsMenu(menu);
	}
	 
	 @Override
	public boolean onOptionsItemSelected(MenuItem item) {		 
		 switch(item.getItemId()){
			 case TRACKMENU:
			 {
				 Intent trackMenuIntent = new Intent(Map.this, TrackingMenu.class);
				 startActivity(trackMenuIntent);
			 	 break;
			 }
			 case TRACKLIST:
			 {
				 Intent trackListIntent = new Intent(Map.this, TrackList.class);
				 Toast.makeText(getApplicationContext(), R.string.toastLoading, Toast.LENGTH_SHORT).show();
				 startActivity(trackListIntent);
				 break;
			 }
			 case DISTANCETHERE:
			 {
				 Intent distanceIntent = new Intent(Map.this, DistanceCalculation.class);
				 startActivity(distanceIntent);
				 break;
			 }
			 case INFOSCREEN:
			 {
				 Intent settingsIntent = new Intent(Map.this, InfoScreen.class);
				 startActivity(settingsIntent);
				 break;
			 }
		 }
		 return true;
		
	}
    
    /**
     * A broadcastreceiver azt a broadcastot figyeli amikor pozíciót váltunk, ekkor lefuttatja a locateMapot
     */
    
    public static class Broadcast extends BroadcastReceiver {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.v("MAP","Map broadcast beérkezett");
			
			distance = intent.getExtras().getDouble("Distance");
			fulldistance += distance;
			
			DecimalFormat decform = new DecimalFormat("#.##");
			formatedDistance = Double.valueOf(decform.format(fulldistance/1000));
			tvMapDistance.setText(String.valueOf(formatedDistance) + " km");
			
			locateMap(context);
		}
		
	}
    
    /**
     * kiszedi a latot és a lont az adatbázisból és a mapot oda "animálja"
     */
    
    public static void locateMap(Context context) {
		
		try{
			SQLiteDatabase database = context.openOrCreateDatabase(DBNAME, MODE_PRIVATE, null);
			Log.v("MAP","Map classban adatbázis nyitás");
			
			Cursor cLat = database.rawQuery(latitudeQuery, null);
			if (cLat.moveToFirst()) {
				latitude = cLat.getDouble(0);
			}
			else {
				latitude = 0;
			}
			
			Cursor cLong = database.rawQuery(longitudeQuery, null);
			if (cLong.moveToFirst()) {
				longitude = cLong.getDouble(0);
			}
			else {
				longitude = 0;
			}
			
			database.close();
			
			Log.v("MAP","Adatbázis mûvelet kész, jöhet a rajzolás a térképen");
			
			GeoPoint geoPoint = new GeoPoint(latitude,longitude);
			
			mPathOverlay.addPoint(geoPoint);
			Log.v("MAP","Pontok száma a mPathOverlayben: " + String.valueOf(mPathOverlay.getNumberOfPoints()));
			
			mMapView.getOverlays().add(mPathOverlay);
			mMapView.invalidate();
				
		}
		catch(Exception ex){
			String err = (ex.getMessage()==null)?"drawOnMapFailed":ex.getMessage();
			Log.v("MAP",err);
		}
    }
    
    static void startTimer(){
		
    	tvMapDistance.setVisibility(TextView.VISIBLE);
    	tvMapDuration.setVisibility(TextView.VISIBLE);
    	
		timer.scheduleAtFixedRate(new TimerTask(){

			@Override
			public void run() {
				if(timerOn){
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
			
			String sHours;
			String sMinutes;
			String sSeconds;			
			
			sHours = String.valueOf(hours);
			if (hours < 10)
				sHours = "0" + sHours;
			
			sMinutes = String.valueOf(minutes);
			if(minutes < 10)
				sMinutes = "0" + sMinutes;
			
			sSeconds = String.valueOf(elapsedTime);
			if(elapsedTime < 10)
				sSeconds = "0" + sSeconds;
			
			tvMapDuration.setText(sHours + ":" + sMinutes + ":" + sSeconds);
		}
	};
    
    
    public static void newTrack(){
    	mPathOverlay.clearPath();
    }
    
    public static void cleanUp(){
    	distance = 0;
    	fulldistance = 0;
    	formatedDistance = 0;
    	
    	pause = 0;
    	
    	timerOn = false;
    	
    	tvMapDistance.setText(null);
    	tvMapDuration.setText(null);
    	
    	tvMapDistance.setVisibility(TextView.GONE);
    	tvMapDuration.setVisibility(TextView.GONE);
    	
    }
    
    public static Double getLatitude(){
		return latitude;
    }
    
    public static Double getLongitude(){
    	return longitude;
    }
    
    private void setUrl(String Url) {
		Map.globalUrl = Url;
	}
    
    public static String getUrl(){
    	return globalUrl;
    }

}
