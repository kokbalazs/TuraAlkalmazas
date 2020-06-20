/**
 * @author Kókay Balázs
 */

package com.kokaybalazs.turaalkalmazas;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;

public class InfoScreen extends Activity{
	
	Button btnExit;
	WebView webView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_infoscreen);
			
		btnExit = (Button) findViewById(R.id.btnInfoExit);
		btnExit.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

}
