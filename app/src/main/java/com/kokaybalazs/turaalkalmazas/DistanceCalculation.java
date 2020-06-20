package com.kokaybalazs.turaalkalmazas;

import java.util.ArrayList;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.views.MapView.Projection;
import org.osmdroid.views.overlay.PathOverlay;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.util.GeoPoint;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

public class DistanceCalculation extends Activity{
	
	private static ZoomLevelMapView mMapView;
    private static IMapController mMapController;
    private TilesOverlay mTilesOverlay;
    private MapTileProviderBasic mProvider;
    private ITileSource mCustomTileSource;
    private static PathOverlay mPathOverlay;
    
    private ArrayList<GeoPoint> geoArray;
    
	double latitude;
	double longitude;
	
	double distance;
	
	static TextView tvDistanceCount;
	static TextView tvDistanceInfo;
	
	public static final int CALCULATE = Menu.FIRST;
	public static final int DELETEONE = CALCULATE+1;
	public static final int DELETEALL = DELETEONE+1;
	
	String URL = Map.getUrl();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mapdistance);
		
		mMapView = (ZoomLevelMapView) findViewById(R.id.mapviewdist);
		
		mMapController = mMapView.getController();
		mMapController.setZoom(13);
		checkLatitude();
		
		mProvider = new MapTileProviderBasic(getApplicationContext());
        mCustomTileSource = new XYTileSource("Turaterkep", null, 13, 15, 256, ".png", URL);
        mProvider.setTileSource(mCustomTileSource);
        mTilesOverlay = new TilesOverlay(mProvider,this.getBaseContext());
        mMapView.getOverlays().add(mTilesOverlay);
        
        tvDistanceCount = (TextView) findViewById(R.id.tvDistanceCalc);
        tvDistanceCount.setBackgroundColor(Color.BLACK);
        
        tvDistanceInfo = (TextView) findViewById(R.id.tvDistanceCalcInformation);
        tvDistanceInfo.setBackgroundColor(Color.BLACK);
        tvDistanceInfo.setText(R.string.tvInfo);
        
        mPathOverlay = new PathOverlay(Color.BLUE, this);
        mPathOverlay.getPaint().setStrokeWidth(8.0f);
        
        geoArray = new ArrayList<GeoPoint>();
        
	}
	
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch(event.getAction()){
		case(MotionEvent.ACTION_UP):
		{
			Projection projection = mMapView.getProjection();
			GeoPoint geoProj = (GeoPoint) projection.fromPixels((int)event.getX(), (int)event.getY());
			Toast.makeText(getApplicationContext(), "Latitude: " + (double)geoProj.getLatitudeE6()/1000000 + ", Longitude: " + (double)geoProj.getLongitudeE6()/1000000, Toast.LENGTH_SHORT).show();
			drawOnMap(geoProj);
			geoArray.add(geoProj);
			break;
		}}
		return super.onTouchEvent(event);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, CALCULATE, 1, R.string.menuItemCalculate);
		menu.add(0, DELETEONE, 2, R.string.menuItemDeletePoint);
		menu.add(0, DELETEALL, 3, R.string.menuItemDeleteAllPoint);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case CALCULATE:
		{
			distance = 0;
			if (geoArray.size() > 1){
				for(int i = 0; i < geoArray.size()-1; i++){
					distance += geoArray.get(i).distanceTo(geoArray.get(i+1));
					tvDistanceCount.setText(String.valueOf(distance/1000) + " km");
				}
			}
			else if (geoArray.size() == 0){
				Toast.makeText(getApplicationContext(), R.string.toastNoClick, Toast.LENGTH_SHORT).show();
			}
			else{
				Toast.makeText(getApplicationContext(), R.string.toastOneMoreClick, Toast.LENGTH_SHORT).show();
			}
			break;
		}
		case DELETEONE:
		{
			geoArray.remove(geoArray.size()-1);
			Log.v("DISCALC","array törlés után: " + String.valueOf(geoArray.size()));
			mPathOverlay.clearPath();
			for (int i=0; i < geoArray.size(); i++){
				mPathOverlay.addPoint(geoArray.get(i));
				mMapView.getOverlays().add(mPathOverlay);
				mMapView.invalidate();
			}
			break;
		}
		case DELETEALL:
		{
			geoArray.clear();
			mPathOverlay.clearPath();
			mMapView.invalidate();
			break;
		}
		}
		return true;
	}
	
	private void drawOnMap(GeoPoint geo){
		mPathOverlay.addPoint(geo);
		mMapView.getOverlays().add(mPathOverlay);
		mMapView.invalidate();
		Log.v("DISTANCECALC","pontok száma po-ban: " + String.valueOf(mPathOverlay.getNumberOfPoints()));
	}

	private void checkLatitude() {
		latitude = Map.getLatitude();
		if (latitude > 0){
			longitude = Map.getLongitude();
			
			GeoPoint firstgeop = new GeoPoint(latitude, longitude);
			mMapController.setCenter((IGeoPoint) firstgeop);
		}
		else{
			GeoPoint pecsGeo = new GeoPoint(46.070833,18.233056);
			mMapController.setCenter(pecsGeo);
		}
	}

}
