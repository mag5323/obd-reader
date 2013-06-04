package eu.lighthouselabs.obd.reader.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import eu.lighthouselabs.obd.reader.R;

/**
 * The main activity.
 */

public class MainActivity extends Activity {
	
	private Spinner carSpin;
	private Button enterBtn; 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		carSpin = (Spinner)findViewById(R.id.car_spinner);
		enterBtn = (Button)findViewById(R.id.enter);
		
        ArrayAdapter adapter = ArrayAdapter.createFromResource(this, R.array.cars,android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        carSpin.setAdapter(adapter);
        
        enterBtn.setOnClickListener(new Button.OnClickListener(){
			@Override
            public void onClick(View v) {
				Bundle bundle = new Bundle();
				bundle.putString("car", carSpin.getSelectedItem().toString());
				Intent intent = new Intent();
				intent.setClass(MainActivity.this, MonitorActivity.class);
				intent.putExtras(bundle);
				startActivity(intent);
			}		
		});
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}
}