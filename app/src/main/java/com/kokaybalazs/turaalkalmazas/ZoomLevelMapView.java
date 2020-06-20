/**
 * @author K�kay Bal�zs
 */

package com.kokaybalazs.turaalkalmazas;

import org.osmdroid.views.MapView;

import android.content.Context;
import android.util.AttributeSet;

public class ZoomLevelMapView extends MapView {
	
	
	
	public ZoomLevelMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public ZoomLevelMapView(Context context, int tileSizePixels) {
		super(context, tileSizePixels);
	}

	@Override
	public int getMaxZoomLevel() {
		return 20;
	}
	
	@Override
	public int getMinZoomLevel() {
		return 10;
	}

}
