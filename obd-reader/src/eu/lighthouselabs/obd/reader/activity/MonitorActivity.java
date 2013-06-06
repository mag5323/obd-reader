/*
 * TODO put header
 */
package eu.lighthouselabs.obd.reader.activity;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import eu.lighthouselabs.obd.commands.SpeedObdCommand;
import eu.lighthouselabs.obd.commands.control.CommandEquivRatioObdCommand;
import eu.lighthouselabs.obd.commands.engine.EngineRPMObdCommand;
import eu.lighthouselabs.obd.commands.engine.MassAirFlowObdCommand;
import eu.lighthouselabs.obd.commands.fuel.FuelEconomyObdCommand;
import eu.lighthouselabs.obd.commands.fuel.FuelEconomyWithMAFObdCommand;
import eu.lighthouselabs.obd.commands.fuel.FuelLevelObdCommand;
import eu.lighthouselabs.obd.commands.fuel.FuelTrimObdCommand;
import eu.lighthouselabs.obd.commands.temperature.AmbientAirTemperatureObdCommand;
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

	private static final String TAG = "MainActivity";

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
	private Button stopBtn = null;
	private Button backBtn = null;
	private Button startBtn = null;
	private TextView tvRpm = null, tvSpeed = null;

	//set up mediaplayer
	private MediaPlayer mp;
	private float MAX_RPM = 16383.75f + 1.0f;
	private int rpm = 1;
	private float volume = 0.0f;
	Bundle bundle = null;
	private int carIndex = -1;

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

		/*
		 * TODO clean-up this upload thing
		 * 
		 * ExceptionHandler.register(this,
		 * "http://www.whidbeycleaning.com/droid/server.php");
		 */
		setContentView(R.layout.monitor);
		
		tvRpm = (TextView) findViewById(R.id.rpm_text);
		tvSpeed = (TextView) findViewById(R.id.spd_text);
		
		bundle = getIntent().getExtras();
		carIndex = bundle.getInt("carIndex");	
		
		Log.d("carIndex:",String.valueOf(carIndex));
		
		/*switch(carIndex){
			case 0:
				mp.create(this, R.raw.bentley_continental_gt);
				break;
			case 1:
				mp.create(this, R.raw.mini_coopers_coupe);
				break;
			case 2:
				mp.create(this, R.raw.mustang_shelby_gt500);
				break;
			case 3:
				mp.create(this, R.raw.nissan_370zs);
				break;
			case 4:
				mp.create(this, R.raw.porsche_gt3);
				break;
			default:
				break;				
		}*/
		
		mp.create(this, R.raw.porsche_gt3);
		
		mListener = new IPostListener() {
			public void stateUpdate(ObdCommandJob job) {
				String cmdName = job.getCommand().getName();
				String cmdResult = job.getCommand().getFormattedResult();
				
				Log.d(TAG, FuelTrim.LONG_TERM_BANK_1.getBank() + " equals " + cmdName + "?");			
				
				if (AvailableCommandNames.ENGINE_RPM.getValue().equals(cmdName)) {					
					tvRpm.setText(cmdResult);
					rpm = ((EngineRPMObdCommand)job.getCommand()).getRPM();		
				} else if (AvailableCommandNames.SPEED.getValue().equals(
						cmdName)) {					
					tvSpeed.setText(cmdResult);
					speed = ((SpeedObdCommand) job.getCommand())
							.getMetricSpeed();					
				} 
				
				Log.d(TAG, "RPM : " + rpm);					
								
				if(rpm>0){
					volume = (float) (1 - (Math.log(MAX_RPM - rpm) / Math.log(MAX_RPM)));					
					mp.start();		
					mp.setVolume(volume, volume);
				}
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
		
		if (mp !=  null )  {
			mp.release();  
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

		// screen won't turn off until wakeLock.release()
		wakeLock.acquire();
	}

	private void stopLiveData() {
		Log.d(TAG, "Stopping live data..");

		if (mServiceConnection.isRunning())
			stopService(mServiceIntent);

		// remove runnable
		mHandler.removeCallbacks(mQueueCommands);
		
		tvRpm.setText("0");
		tvSpeed.setText("0");
		mp.stop();
		
		releaseWakeLockIfHeld();
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
		//MenuItem commandItem = menu.findItem(COMMAND_ACTIVITY);

		// validate if preRequisites are satisfied.
		if (preRequisites) {
			if (mServiceConnection.isRunning()) {
				startItem.setEnabled(false);
				stopItem.setEnabled(true);
				settingsItem.setEnabled(false);
				//commandItem.setEnabled(false);
			} else {
				stopItem.setEnabled(false);
				startItem.setEnabled(true);
				settingsItem.setEnabled(true);
				//commandItem.setEnabled(false);
			}
		} else {
			startItem.setEnabled(false);
			stopItem.setEnabled(false);
			settingsItem.setEnabled(false);
			//commandItem.setEnabled(false);
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
						FuelType.DIESEL, speed, maf, ltft, false /* TODO */);
				//TextView tvMpg = (TextView) findViewById(R.id.fuel_econ_text);
				String liters100km = String.format("%.2f", fuelEconCmd.getLitersPer100Km());
				//tvMpg.setText("" + liters100km);
				Log.d(TAG, "FUELECON:" + liters100km);
			}

			if (mServiceConnection.isRunning())
				queueCommands();
				Log.d(TAG,"queueCommands();");

			// run again in 2s
			mHandler.postDelayed(mQueueCommands, 2000);
		}
	};

	/**
	 * 
	 */
	private void queueCommands() {
		final ObdCommandJob airTemp = new ObdCommandJob(
				new AmbientAirTemperatureObdCommand());
		final ObdCommandJob speed = new ObdCommandJob(new SpeedObdCommand());
		final ObdCommandJob fuelEcon = new ObdCommandJob(
				new FuelEconomyObdCommand());
		final ObdCommandJob rpm = new ObdCommandJob(new EngineRPMObdCommand());
		final ObdCommandJob maf = new ObdCommandJob(new MassAirFlowObdCommand());
		final ObdCommandJob fuelLevel = new ObdCommandJob(
				new FuelLevelObdCommand());
		final ObdCommandJob ltft1 = new ObdCommandJob(new FuelTrimObdCommand(
				FuelTrim.LONG_TERM_BANK_1));
		final ObdCommandJob ltft2 = new ObdCommandJob(new FuelTrimObdCommand(
				FuelTrim.LONG_TERM_BANK_2));
		final ObdCommandJob stft1 = new ObdCommandJob(new FuelTrimObdCommand(
				FuelTrim.SHORT_TERM_BANK_1));
		final ObdCommandJob stft2 = new ObdCommandJob(new FuelTrimObdCommand(
				FuelTrim.SHORT_TERM_BANK_2));
		final ObdCommandJob equiv = new ObdCommandJob(new CommandEquivRatioObdCommand());

		//mServiceConnection.addJobToQueue(speed);
		mServiceConnection.addJobToQueue(rpm);
		mServiceConnection.addJobToQueue(maf);
		mServiceConnection.addJobToQueue(fuelLevel);
		mServiceConnection.addJobToQueue(ltft1);
	}
}