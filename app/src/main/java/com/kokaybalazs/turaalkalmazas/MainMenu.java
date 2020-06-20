/**
 * @author K�kay Bal�zs
 */

package com.kokaybalazs.turaalkalmazas;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.Menu;
import android.widget.Button;

/**
 * A f�men� activity-je
 */
public class MainMenu extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_menu);
		
		// Intentek be�ll�t�sa
		final Intent mapIntent = new Intent(getApplicationContext(), Map.class);
		final Intent trackIntent = new Intent(getApplicationContext(), TrackingMenu.class);
		// Intent calculatingIntent = new Intent(getApplicationContext(), Calculating.class);
		
		//Gombok be�ll�t�sa
		Button btnMap = (Button) findViewById(R.id.btnShowMap);
		btnMap.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startActivity(mapIntent);
			}
		});
		//Button btnCalculate = (Button) findViewById(R.id.btnTrackCalculation);
		
		Button btnTrack = (Button) findViewById(R.id.btnTrackingDetails);
		btnTrack.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startActivity(trackIntent);				
			}
		});
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

}
