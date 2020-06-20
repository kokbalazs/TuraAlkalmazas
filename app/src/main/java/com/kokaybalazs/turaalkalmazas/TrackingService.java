/**
 * @author K�kay Bal�zs
 */

package com.kokaybalazs.turaalkalmazas;

import java.util.Calendar;
import java.util.Date;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

public class TrackingService extends Service implements LocationListener {
	
	LocationManager mLocationManager; 
	
	//adatb�zishoz k�t v�ltoz�
	private static final String DBNAME = Environment.getExternalStorageDirectory().toString() + "/database.sqlite";
    private static final String TABLE_NAME = "gps" + getDateForTable();
    
    public static boolean servicePause = false;
    
    /** A service indul�sakor elind�tja a startLocationUpdate-et */
	
	@Override
	 public void onCreate() {
		super.onCreate();
		startLocationUpdate();
	 }
	
	/** A service le�ll�sakor le�ll�tja a locationmanagert */
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mLocationManager.removeUpdates(this);
	}
	
	/**
	 * L�trehozza az adatb�zist �s a t�bl�t, majd elind�tja a Location lek�r�st a requestLocationUpdate-el
	 */
	
	private void startLocationUpdate() {
		try {
			String teszt = getDateForTable();
			Log.v("SERVICE", teszt.toString());
			Log.v("SERVICE", "Adatb�zis �s t�bla ellen�rz�s / csin�l�s");
			SQLiteDatabase database = openOrCreateDatabase(DBNAME, MODE_PRIVATE, null);
			database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(" + 
											"_id INTEGER PRIMARY KEY AUTOINCREMENT,	 " + 
											"latitude REAL NOT NULL, " + 
											"longitude REAL NOT NULL, " + 
											"altitude REAL NOT NULL, " +
											"time TEXT NOT NULL" + ");");
			database.close();
			Log.v("SERVICE", "Adatb�zis k�sz, lez�rva, kezd�dj�n a locationlisten be�ll�t�sa");
		} catch (Exception e) {
			Log.v("SERVICE", e.getMessage());
		}
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Log.v("SERVICE", "a location servicet megkapta a mlocationmagnager");
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000l, 5F, this);
		Log.v("SERVICE", "elindul a requestlocationupdates");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/**
	 * Ez az elj�r�s akkor fut le amikor a gps jelzi hogy v�ltozott a poz�ci�. Az adott poz�ci� latitude-, longitude- �s idej�t
	 * berakja a t�bl�ba. A k�t kurzor kiveszi az el�z� latitude- �s longitudeot a t�vols�gm�r�shez. Egy intentbe berakja
	 * az adatokat �s kik�ldi broadcastban. 
	 */
	
	@Override
	public void onLocationChanged(Location location) {
		try{
		if(!servicePause){
			double latitude = location.getLatitude();
			double longitude = location.getLongitude();
			double latitudePrevious;
			double longitudePrevious;
			String timeString;
			
			double altitude = location.getAltitude();
			
			double distance = 0;
			
			String latitudeQuery = "SELECT latitude FROM " + TABLE_NAME + " WHERE _id in (SELECT COUNT(_id)-1 FROM " + TABLE_NAME + ")";
			String longitudeQuery = "SELECT longitude FROM " + TABLE_NAME + " WHERE _id in (SELECT COUNT(_id)-1 FROM " + TABLE_NAME + ")";
			
			//long time = location.getTime();
			Date time = new Date();
			timeString = (String) DateFormat.format("yyyy-MM-dd'T'kk:mm:ssZ", time);
			SQLiteDatabase database = openOrCreateDatabase(DBNAME, MODE_PRIVATE, null);
			
			Log.v("SERVICE", "Latitude/longitude/time berak�s�nak kezd�se");
			database.execSQL("INSERT INTO " + TABLE_NAME + "(latitude,longitude,altitude,time) VALUES (" + 
											 latitude + ", " +
											 longitude + ", " +
											 altitude + ", \"" +
											 timeString + "\");");
			Log.v("SERVICE", "berak�s v�ge, kiszedj�k az el�z� latot,longot");
			
			Cursor cLat = database.rawQuery(latitudeQuery, null);
			if (cLat.moveToFirst()) {
				latitudePrevious = cLat.getDouble(0);
			}
			else {
				latitudePrevious = latitude;
			}
			
			Cursor cLong = database.rawQuery(longitudeQuery, null);
			if (cLong.moveToFirst()) {
				longitudePrevious = cLong.getDouble(0);
			}
			else {
				longitudePrevious = longitude;
			}
			
			database.close();
			
			distance = distanceCount(latitudePrevious,longitudePrevious,latitude,longitude);
			
			Intent trackIntent = new Intent();
			trackIntent.setAction("com.kokaybalazs.turaalkalmazas.TRACKDATA");
			trackIntent.putExtra("LastLatitude", String.valueOf(latitude));
			trackIntent.putExtra("LastLongitude", String.valueOf(longitude));
			trackIntent.putExtra("Distance", distance);
			sendBroadcast(trackIntent);
			Log.v("SERVICE","broadcast kik�ldve");
			
			Intent mapIntent = new Intent();
			mapIntent.setAction("com.kokaybalazs.turaalkalmazas.MAPDATA");
			mapIntent.putExtra("Distance", distance);
			sendBroadcast(mapIntent);
			Log.v("SERVICE","map broadcast kik�ldve");
		}
		}
		catch(Exception ex){
			Log.v("SERVICE","" + ex.getMessage());
		}
		
	}

	@Override
	public void onProviderDisabled(String arg0) {
		Toast.makeText(getApplicationContext(), "A GPS ki lett kapcsolva", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onProviderEnabled(String arg0) {
		Toast.makeText(getApplicationContext(), "A GPS bekapcsolt", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onStatusChanged(String arg0, int status, Bundle arg2) {
	}
	
	/**
	 * Ez az elj�r�s sz�molja ki a t�vols�got a k�t f�ldrajzi pont k�z�tt
	 */
	
	public double distanceCount(double latA, double lonA, double latB, double lonB){
		double distance = 0;
		
		Location locationA = new Location("");
		Location locationB = new Location("");
		
		locationA.setLatitude(latA);
		locationA.setLongitude(lonA);
		
		locationB.setLatitude(latB);
		locationB.setLongitude(lonB);
		
		try
        {
			distance = locationA.distanceTo(locationB);
                /*final float[] results = new float[3];
                Location.distanceBetween(latA, lonA, latB, lonB, results);
                distance = results[0];*/
        }
        catch (final Exception ex)
        {
                distance = 0;
        }
		
		return distance;
	}
	
	/**
	 * Az elj�r�s visszaadja a mai d�tumot.
	 */
	
	public static String getDateForTable(){
		Calendar calendar = Calendar.getInstance();
		String text = String.valueOf(calendar.get(Calendar.YEAR)) + "_" + String.valueOf(calendar.get(Calendar.MONTH)+1) + "_" + String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)) /*+ "_" + String.valueOf(calendar.get(Calendar.HOUR_OF_DAY))*/;
		return text;
	}
	
	public static String getDbName(){
		return DBNAME;
	}
	
	public static String getTableName(){
		return TABLE_NAME;
	}


}
