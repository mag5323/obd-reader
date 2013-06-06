package eu.lighthouselabs.obd.reader.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import eu.lighthouselabs.obd.reader.R;

/**
 * The main activity.
 */

public class MainActivity extends Activity {
	
	private Spinner carSpin;
	private Button enterBtn; 
	private ImageView carImage;
	private int[] carDraw = {R.drawable.bentley_continental_gt,
							 R.drawable.mini_coopers_coupe,
							 R.drawable.mustang_shelby_gt500,
							 R.drawable.nissan370zs,
							 R.drawable.porsche_gt3};
	private int carIndex = -1;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		carImage = (ImageView)findViewById(R.id.car_view);
		carSpin = (Spinner)findViewById(R.id.car_spinner);
		enterBtn = (Button)findViewById(R.id.enter);
		
        ArrayAdapter adapter = ArrayAdapter.createFromResource(this, R.array.cars,android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        carSpin.setAdapter(adapter);
        
        carSpin.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                    int arg2, long arg3) {
                // TODO Auto-generated method stub
            	switch(arg2){
            	case 0:
            		carImage.setImageResource(carDraw[0]);
            		carIndex = 0;
            		break;
            	case 1:
            		carImage.setImageResource(carDraw[1]);
            		carIndex = 1;
            		break;
            	case 2:
            		carImage.setImageResource(carDraw[2]);
            		carIndex = 2;
            		break;
            	case 3:
            		carImage.setImageResource(carDraw[3]);
            		carIndex = 3;
            		break;
            	case 4:
            		carImage.setImageResource(carDraw[4]);
            		carIndex = 4;
            		break;
            	}	
            }
 
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub
 
            }
 
        });
        
        enterBtn.setOnClickListener(new Button.OnClickListener(){
			@Override
            public void onClick(View v) {
				Bundle bundle = new Bundle();
				bundle.putInt("carIndex",carIndex);
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