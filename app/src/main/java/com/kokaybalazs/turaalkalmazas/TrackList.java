/**
 * @author Kókay Balázs
 */

package com.kokaybalazs.turaalkalmazas;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import android.app.Activity;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.widget.SimpleCursorAdapter;

/**
 * A TrackList osztály az elõzõ utak listáját mutatja egy activityben, egy SimpleCursorAdapter segítségével
 */

public class TrackList extends Activity implements OnItemClickListener{
	
	private SimpleCursorAdapter sqlAdapter;
	private Cursor tableCursor;
	private Cursor tableAdapterCursor;
	
	private String TABLE_NAME = "tracklist";	
	private String tableQuery = "INSERT INTO " + TABLE_NAME + "(name) SELECT name FROM sqlite_master WHERE name like '%gps%'";
	private String DBNAME = TrackingService.getDbName();
	
	public static final int GPXMENU = Menu.FIRST;
	
	//kurzoroknál az aktuális tábla
	private String actualTable;
	//listviewnál az aktuális tábla
	private String table;
		
	long durdate;
	DateTime ftime;
	DateTime ltime;
	
	private double latA;
	private double lonA;
	private double latB;
	private double lonB;
	
	private double length;
	private double formatedLength;
	private String formatedDuration;
	
	ListView listView;
	TextView tvActualTable;
			
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tracklist);
				
		String getDataQuery = "SELECT _id,name FROM " + TABLE_NAME;
		String adapterQuery = "SELECT _id,name,duration,length FROM " + TABLE_NAME;
		try{
			SQLiteDatabase database = openOrCreateDatabase(DBNAME, MODE_PRIVATE, null);
			
			database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(" + 
					"_id INTEGER PRIMARY KEY AUTOINCREMENT,	 " + 
					"name TEXT NOT NULL, " + 
					"duration TEXT, " + 
					"length TEXT" + ");");
			
			database.execSQL(tableQuery);
			
			tableCursor = database.rawQuery(getDataQuery, null);
			
			getData();
			
			tableAdapterCursor = database.rawQuery(adapterQuery, null);
			
			String[] sqliteColumns = new String[] {"name","duration","length"};
			int[] textViewList = new int[] {R.id.tvTitle,R.id.tvDuration,R.id.tvLength};
			
			sqlAdapter = new SimpleCursorAdapter(this, R.layout.tracklist_row, tableAdapterCursor, sqliteColumns, textViewList, 0);
			
			listView = (ListView) findViewById(R.id.listView);
			listView.setOnItemClickListener(this);
			listView.setAdapter(sqlAdapter);
			
			database.close();
			
			tvActualTable = (TextView) findViewById(R.id.tvActualTable);
		}
		catch(SQLException ex){
			Log.v("TRACKLIST", "onCreate hiba: " + ex.getMessage());
		}
		
		
	}	
	
	@Override
	protected void onDestroy() {
		try{
			SQLiteDatabase database = openOrCreateDatabase(DBNAME, MODE_PRIVATE, null);
			database.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
			database.close();
			finish();
		}
		catch(SQLException ex){
			Log.v("TRACKLIST", "onDestroy hiba: " + ex.getMessage());
		}
		
		super.onDestroy();
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		
		View customView = parent.getChildAt(position);
		TextView tv = (TextView) customView.findViewById(R.id.tvTitle);
				
		table = tv.getText().toString();
		tvActualTable.setText("Az aktuális tábla: " + table);
		tvActualTable.setBackgroundColor(Color.BLACK);
		//Toast.makeText(getApplicationContext(),"Az aktuális tábla beállítva: " + table, Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, GPXMENU, 1, R.string.menuItemGpxFile);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
			case GPXMENU:
			{
				gpxFileCreation();
				break;
			}
		}
		return true;
	}

	private void getData() {
		tableCursor.move(-1);
		
		while (tableCursor.moveToNext()){
			length=0;
			durdate=0;
			actualTable = tableCursor.getString(tableCursor.getColumnIndex("name"));
			Log.v("TRACKLIST","kurzor tábla név:" + actualTable);
			
			//kurzorok Queryje			
			String firstTimeQuery = "SELECT time FROM " + actualTable + " ORDER BY _id ASC LIMIT 1";
			String lastTimeQuery = "SELECT time FROM " + actualTable + " ORDER BY _id DESC LIMIT 1";
			
			String latQuery = "SELECT latitude FROM " + actualTable + " ORDER BY _id";
			String lonQuery = "SELECT longitude FROM " + actualTable + " ORDER BY _id";
			
			DateTimeFormatter parser = ISODateTimeFormat.dateTimeParser();
			
			//kurzorok
			try{
				SQLiteDatabase database = openOrCreateDatabase(DBNAME, MODE_PRIVATE, null);
				
				//az elsõ idõpont az adatbázisból
				Cursor cFirstTime = database.rawQuery(firstTimeQuery, null);
				if (cFirstTime.moveToFirst()) {
					ftime = /*new Date(cFirstTime.getString(0));*/parser.parseDateTime(cFirstTime.getString(0));
					Log.v("KURVANYÁD","" + String.valueOf(ftime));
				}
				else {
					ftime = null;
				}
				
				//az utolsó idõpont az adatbázisból
				Cursor cLastTime = database.rawQuery(lastTimeQuery, null);
				if (cLastTime.moveToFirst()) {
					ltime = parser.parseDateTime(cLastTime.getString(0));
					Log.v("KURVANYÁD","" + String.valueOf(ltime));
				}
				else {
					ltime = null;
				}
				
				Cursor latCursor = database.rawQuery(latQuery, null);
				Cursor lonCursor = database.rawQuery(lonQuery, null);
				
				ArrayList<Double> latArrayList = new ArrayList<Double>();
				ArrayList<Double> lonArrayList = new ArrayList<Double>();
				
				latCursor.moveToFirst();
				lonCursor.moveToFirst();
				while(!latCursor.isAfterLast()){
					latArrayList.add(latCursor.getDouble(0));
					lonArrayList.add(lonCursor.getDouble(0));
					latCursor.moveToNext();
					lonCursor.moveToNext();
				}
								
				Log.v("TRACKLIST", "latarray: " + String.valueOf(latArrayList.size()));
								
				for(int i=0; i<latArrayList.size()-1;i++){
					try{
					latA = latArrayList.get(i);
					lonA = lonArrayList.get(i);
					
					latB = latArrayList.get(i+1);
					lonB = lonArrayList.get(i+1);
					
					Location locationA = new Location("");
					Location locationB = new Location("");
					
					locationA.setLatitude(latA);
					locationA.setLongitude(lonA);
					
					locationB.setLatitude(latB);
					locationB.setLongitude(lonB);
					
						length += locationA.distanceTo(locationB);
					}
					catch(Exception ex){
						Log.v("TRACKLIST", "" + ex.getMessage());
						length += 0;
					}
				}
				Log.v("TRACKLIST","" + String.valueOf(length));
				formatedLength = lengthFormat(length);
				database.execSQL("UPDATE " + TABLE_NAME + " SET length=\"" + formatedLength + " km\" WHERE name=\"" + actualTable + "\"");
								
				//durdate = ltime - ftime;
				Seconds sekonc = Seconds.secondsBetween(ftime, ltime);
				durdate = sekonc.getSeconds();
				Log.v("TRACKLIST", String.valueOf(durdate));
				formatedDuration = durationFormat(durdate);
				database.execSQL("UPDATE " + TABLE_NAME + " SET duration= \"" + formatedDuration + "\" WHERE name=\"" + actualTable + "\"");
				
				database.close();
			}
			catch (SQLException ex){
				Log.v("TRACKLIST", "kurzornál hiba: " + ex.getMessage());
			}
		}
	}
	
	public void gpxFileCreation(){
		String cursorQuery = "SELECT latitude,longitude,altitude,time FROM \"" + table + "\" ORDER BY _id";
		ArrayList<Double> latArrayList = new ArrayList<Double>();
		ArrayList<Double> lonArrayList = new ArrayList<Double>();
		//ArrayList<Double> altArrayList = new ArrayList<Double>();
		ArrayList<String> timeArrayList = new ArrayList<String>();
		
		try{
			SQLiteDatabase database = openOrCreateDatabase(DBNAME, MODE_PRIVATE, null);
			Cursor gpxCursor = database.rawQuery(cursorQuery, null);
					
			gpxCursor.moveToFirst();
			
			while(!gpxCursor.isAfterLast()){
				latArrayList.add(gpxCursor.getDouble(0));
				lonArrayList.add(gpxCursor.getDouble(1));
				//altArrayList.add(gpxCursor.getDouble(2));
				timeArrayList.add(gpxCursor.getString(3));
				gpxCursor.moveToNext();
			}
			
			File directory = Environment.getExternalStorageDirectory();
			if (directory.canWrite()){
				File gpxFile = new File(directory, "" + table + ".gpx");
				FileWriter fileWriter = new FileWriter(gpxFile);
				BufferedWriter outWriter = new BufferedWriter(fileWriter);
				
				outWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + 
								"\n <gpx version=\"1.1\" creator=\"Túra Alkalmazás\"" + 
								"\n xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\" " +
								"\n xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
								"\n xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 " +
								"\n http://www.topografix.com/GPX/1/1/gpx.xsd\">" + 
								"\n  <metadata>" + 
								"\n   <time>" + DateFormat.format("yyyy-MM-dd'T'kk:mm:ssZ", new Date()) + "</time>" + 
								"\n  </metadata>" + 
								"\n  <trk>" + 
								"\n   <trkseg> \n");
				for(int i=0; i<latArrayList.size(); i++){
					outWriter.write("   <trkpt lat=\"" + latArrayList.get(i) + "\" lon=\"" + lonArrayList.get(i) + "\">" +
										"\n<ele>" /*+ altArrayList.get(i)*/ + "</ele>" + 
										"\n<time>" + timeArrayList.get(i) + "</time>" + 
										"\n</trkpt>");
				}
				
				outWriter.write("\n</trkseg>\n" + 
								"</trk>\n" + 
								"</gpx>");
				
				/*outWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
						"\n <kml xmlns=\"http://www.opengis.net/kml/2.2\">" +
						"\n <Document>" + "\n");
						for (int i = 0; i<latArrayList.size();i++){
							outWriter.write("<Placemark>" + 
											"\n <name>" + i + "</name>" +
											"\n <description> </description>" +
											"\n <Point>" +
											"\n <coordinates>" + latArrayList.get(i) + "," + lonArrayList.get(i) + "</coordinates>" +
											"\n </Point>" +
											"\n </Placemark>" + "\n");
						}
				outWriter.write("</Document>" + 
								"\n </kml>");*/
				outWriter.close();
			}
			else{
				Log.v("TRACKLIST","Nincs engedélyezve az írás");
			}
			
			database.close();
			Toast.makeText(getApplicationContext(), "A gpx fájl sikeresen létrehozva, "  + table + ".gpx néven", Toast.LENGTH_SHORT).show();
			
		}
		catch(Exception ex){
			Log.v("TRACKLIST","" + ex.getMessage());
		}
		
	}
	
	public Double lengthFormat(double length){
		DecimalFormat lengthFormat = new DecimalFormat("#.##");
		length = Double.valueOf(lengthFormat.format(length/1000).replace(",", ""));
		return length;
	}
	
	
	public String durationFormat(long seconds){
		String durString = "";
				
		long minutes = seconds/60;
		long hours = minutes/60;
		
		if (minutes > 0){
			seconds = seconds - (minutes * 60);
		}
		
		if (hours > 0){
			minutes = minutes - (hours * 60);
		}
		
		String secondString = String.valueOf(seconds);
		String minuteString = String.valueOf(minutes);
		String hourString = String.valueOf(hours);
		
		if(seconds < 10)
			secondString = "0" + secondString;
		if (minutes < 10)
			minuteString = "0" + minuteString;
		if(hours < 10)
			hourString = "0" + hourString;
		
		durString = hourString + ":" + minuteString + ":" + secondString;		
		
		return durString;
	}

}
