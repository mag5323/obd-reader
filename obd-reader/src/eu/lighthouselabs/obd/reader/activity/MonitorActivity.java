/*
 * TODO put header
 */
package eu.lighthouselabs.obd.reader.activity;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import android.widget.TextView;
import eu.lighthouselabs.obd.commands.SpeedObdCommand;
import eu.lighthouselabs.obd.commands.engine.EngineRPMObdCommand;
import eu.lighthouselabs.obd.commands.fuel.FuelEconomyWithMAFObdCommand;
import eu.lighthouselabs.obd.enums.AvailableCommandNames;
import eu.lighthouselabs.obd.enums.FuelTrim;
import eu.lighthouselabs.obd.enums.FuelType;
import eu.lighthouselabs.obd.reader.IPostListener;
import eu.lighthouselabs.obd.reader.R;
import eu.lighthouselabs.obd.reader.io.ObdCommandJob;
import eu.lighthouselabs.obd.reader.io.ObdGatewayService;
import eu.lighthouselabs.obd.reader.io.ObdGatewayServiceConnection;

/**
 * The monitor activity.
 */
public class MonitorActivity extends Activity {

	private static final String TAG = "MonitorActivity";

	/*
	 * TODO put description
	 */
	static final int NO_BLUETOOTH_ID = 0;
	static final int BLUETOOTH_DISABLED = 1;
	static final int NO_GPS_ID = 2;
	static final int START_LIVE_DATA = 3;
	static final int STOP_LIVE_DATA = 4;
	static final int SETTINGS = 5;
	static final int COMMAND_ACTIVITY = 6;
	static final int TABLE_ROW_MARGIN = 7;
	static final int NO_ORIENTATION_SENSOR = 8;

	private Handler mHandler = new Handler();

	/**
	 * Callback for ObdGatewayService to update UI.
	 */
	private IPostListener mListener = null;
	private Intent mServiceIntent = null;
	private ObdGatewayServiceConnection mServiceConnection = null;

	private SharedPreferences prefs = null;

	private PowerManager powerManager = null;
	private PowerManager.WakeLock wakeLock = null;

	private boolean preRequisites = true;

	private int speed = 1;
	private double maf = 1;
	private float ltft = 0;	
	private double equivRatio = 1;
	private TextView tvRpm = null, tvSpeed = null;

	//set up SoundPool
	private SoundPool soundPool;
	private int vehicle;
	Bundle bundle = null;
	private int carIndex;
	private int rpm = 1;
	private int rpmMax = 2080; //8192
	private boolean loaded = false;
	
	//Dial-chart
	DialView dv;
	RelativeLayout mLayout;
	
	public void updateTextView(final TextView view, final String txt) {
		new Handler().post(new Runnable() {
			public void run() {
				view.setText(txt);
			}
		});
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.monitor);
		
		//add DialView to RelativeLayout
		mLayout = (RelativeLayout)findViewById(R.id.vehicle_view);
		dv = new DialView(this, null);		
		mLayout.addView(dv);
		
		tvSpeed = (TextView) findViewById(R.id.spd_text);
		
		//create a mp3 file
		bundle = getIntent().getExtras();
		carIndex = bundle.getInt("carIndex");
			
		int[] vehicles = {	R.raw.bentley_continental_gt,
							R.raw.mini_coopers_coupe,
							R.raw.mustang_shelby_gt500,
							R.raw.nissan_370zs,
							R.raw.porsche_gt3  };
		
		soundPool = new SoundPool(1, AudioManager.STREAM_SYSTEM,1);
		vehicle = soundPool.load(this, vehicles[carIndex], 1);		
		Log.d(TAG,"vehicle : " + vehicle);
		
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener(){
			@Override
			public void onLoadComplete(SoundPool soundPool, int SampleId, int status) {					
				if(status==0){
					loaded = true;					
				}			
			}
		});    
        
		mListener = new IPostListener() {
			public void stateUpdate(ObdCommandJob job) {
				String cmdName = job.getCommand().getName();
				String cmdResult = job.getCommand().getFormattedResult();
				
				if (cmdResult=="NODATA") {
					cmdResult = "0";
				}
				
				if (AvailableCommandNames.ENGINE_RPM.getValue().equals(cmdName)) {					
					rpm = ((EngineRPMObdCommand)job.getCommand()).getRPM();
					rpm = (rpm+1) / 2;	
										
				} else if (AvailableCommandNames.SPEED.getValue().equals(cmdName)) {					
					tvSpeed.setText(cmdResult);
					speed = ((SpeedObdCommand) job.getCommand()).getMetricSpeed();					
				}
				
				Log.d(TAG, "RPM : " + rpm);			
				
				dv.setRPM(rpm);
				dv.invalidate();
				
				
				int temp = rpmMax - rpm; 					
				float rate = (float)(2.0 - (Math.log(temp>0 ? temp:1)/Math.log(rpmMax)));	
				
				Log.d("RPM : ", rpm+"");
				
				soundPool.setRate(vehicle, rate);
				
				Log.d(TAG, "rate : " + rate);
				
			}
		};

		/*
		 * Validate GPS service.
		 */
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		if (locationManager.getProvider(LocationManager.GPS_PROVIDER) == null) {
			/*
			 * TODO for testing purposes we'll not make GPS a pre-requisite.
			 */
			// preRequisites = false;
			showDialog(NO_GPS_ID);
		}

		/*
		 * Validate Bluetooth service.
		 */
		// Bluetooth device exists?
		final BluetoothAdapter mBtAdapter = BluetoothAdapter
				.getDefaultAdapter();
		if (mBtAdapter == null) {
			preRequisites = false;
			showDialog(NO_BLUETOOTH_ID);
		} else {
			// Bluetooth device is enabled?
			if (!mBtAdapter.isEnabled()) {
				preRequisites = false;
				showDialog(BLUETOOTH_DISABLED);
			}
		}

		// validate app pre-requisites
		if (preRequisites) {
			/*
			 * Prepare service and its connection
			 */
			mServiceIntent = new Intent(this, ObdGatewayService.class);
			mServiceConnection = new ObdGatewayServiceConnection();
			/*start listening here*/
			mServiceConnection.setServiceListener(mListener);

			// bind service
			Log.d(TAG, "Binding service..");
			bindService(mServiceIntent, mServiceConnection,
					Context.BIND_AUTO_CREATE);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		releaseWakeLockIfHeld();
		mServiceIntent = null;
		mServiceConnection = null;
		mListener = null;
		mHandler = null;
		
		if (soundPool !=  null )  {
			soundPool.release();  
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "Pausing..");
		releaseWakeLockIfHeld();
	}

	/**
	 * If lock is held, release. Lock will be held when the service is running.
	 */
	private void releaseWakeLockIfHeld() {
		if (wakeLock.isHeld()) {
			wakeLock.release();
		}
	}

	protected void onResume() {
		super.onResume();

		Log.d(TAG, "Resuming..");

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
				"ObdReader");
	}

	private void updateConfig() {
		Intent configIntent = new Intent(this, ConfigActivity.class);
		startActivity(configIntent);
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, START_LIVE_DATA, 0, "Start Live Data");
		menu.add(0, COMMAND_ACTIVITY, 0, "Run Command");
		menu.add(0, STOP_LIVE_DATA, 0, "Stop");
		menu.add(0, SETTINGS, 0, "Settings");
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case START_LIVE_DATA:
			startLiveData();
			return true;
		case STOP_LIVE_DATA:
			stopLiveData();
			return true;
		case SETTINGS:
			updateConfig();
			return true;
		case COMMAND_ACTIVITY:
			//staticCommand();
			return true;
		}
		return false;
	}

	private void startLiveData() {
		Log.d(TAG, "Starting live data..");

		if (!mServiceConnection.isRunning()) {
			Log.d(TAG, "Service is not running. Going to start it..");
			startService(mServiceIntent);
		}

		// start command execution
		mHandler.post(mQueueCommands);
		if (loaded) {
			soundPool.play(vehicle, 1, 1, 1, -1, 0.5f);
		}

		// screen won't turn off until wakeLock.release()
		wakeLock.acquire();
	}

	private void stopLiveData() {
		Log.d(TAG, "Stopping live data..");
		
		tvSpeed.setText("0");
		soundPool.stop(vehicle);
		
		if (mServiceConnection.isRunning())
			stopService(mServiceIntent);

		// remove runnable
		mHandler.removeCallbacks(mQueueCommands);		
		releaseWakeLockIfHeld();
		soundPool.stop(vehicle);
		soundPool.release();
	}

	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder build = new AlertDialog.Builder(this);
		switch (id) {
		case NO_BLUETOOTH_ID:
			build.setMessage("Sorry, your device doesn't support Bluetooth.");
			return build.create();
		case BLUETOOTH_DISABLED:
			build.setMessage("You have Bluetooth disabled. Please enable it!");
			return build.create();
		case NO_GPS_ID:
			build.setMessage("Sorry, your device doesn't support GPS.");
			return build.create();
		case NO_ORIENTATION_SENSOR:
			build.setMessage("Orientation sensor missing?");
			return build.create();
		}
		return null;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem startItem = menu.findItem(START_LIVE_DATA);
		MenuItem stopItem = menu.findItem(STOP_LIVE_DATA);
		MenuItem settingsItem = menu.findItem(SETTINGS);
		MenuItem commandItem = menu.findItem(COMMAND_ACTIVITY);

		// validate if preRequisites are satisfied.
		if (preRequisites) {
			if (mServiceConnection.isRunning()) {
				startItem.setEnabled(false);
				stopItem.setEnabled(true);
				settingsItem.setEnabled(false);
				commandItem.setEnabled(false);
			} else {
				stopItem.setEnabled(false);
				startItem.setEnabled(true);
				settingsItem.setEnabled(true);
				commandItem.setEnabled(false);
			}
		} else {
			startItem.setEnabled(false);
			stopItem.setEnabled(false);
			settingsItem.setEnabled(false);
			commandItem.setEnabled(false);
		}

		return true;
	}

	/**
	 * 
	 */
	private Runnable mQueueCommands = new Runnable() {
		public void run() {
			/*
			 * If values are not default, then we have values to calculate MPG
			 */
			Log.d(TAG, "SPD:" + speed + ", MAF:" + maf + ", LTFT:" + ltft);
			if (speed > 1 && maf > 1 && ltft != 0) {
				FuelEconomyWithMAFObdCommand fuelEconCmd = new FuelEconomyWithMAFObdCommand(
						FuelType.DIESEL, speed, maf, ltft, false );
				String liters100km = String.format("%.2f", fuelEconCmd.getLitersPer100Km());
				Log.d(TAG, "FUELECON:" + liters100km);
			}

			if (mServiceConnection.isRunning())
				queueCommands();
				Log.d(TAG,"queueCommands();");

			// run again in 0.001s
			mHandler.postDelayed(mQueueCommands,500);
		}
	};

	/**
	 * 
	 */
	private void queueCommands() {
		final ObdCommandJob speed = new ObdCommandJob(new SpeedObdCommand());
		final ObdCommandJob rpm = new ObdCommandJob(new EngineRPMObdCommand());

		mServiceConnection.addJobToQueue(speed);
		mServiceConnection.addJobToQueue(rpm);
	}
}