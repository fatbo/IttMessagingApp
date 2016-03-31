package com.oneport.itt;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import cn.jpush.android.api.JPushInterface;

import com.android.volley.VolleyError;
import com.oneport.fragment.ContactUsFragment;
import com.oneport.fragment.MsgListFragment;
import com.oneport.fragment.MsgViewPagerFragment;
import com.oneport.fragment.SettingFragment;
import com.oneport.fragment.UpdateDialog;
import com.oneport.fragment.UpdateDialog.UpdateListener;
import com.oneport.itt.utils.AppUtils;
import com.oneport.manager.MsgManager;
import com.oneport.manager.SqliteController;
import com.oneport.model.Msg;
import com.oneport.mqtt.MQTTservice;
import com.oneport.network.NetworkAddress;
import com.oneport.network.NetworkDelegate;

public class MainActivity extends BaseActivity implements OnClickListener,
		NetworkDelegate, UpdateListener {

	private static int secretNum;
	private Button btnMenu;
	private ImageButton btnUpdate;
	private View menu;
	private Button btnBackToList;
	private Button btnSetting;
	private Button btnContactUs;
	private TextView txtTrackID, txtMsgTime;
	public TextView tv_index;
	UpdateDialog dialog;
	ProgressDialog progress;
	//boolean active = false;
	private boolean pendingVersionUpdateDialog=false;
	
	private ImageView imgNetworkStatus;
	private ImageView imgBatteryLevel;
	private ImageView imgMqttConnectionStatus;

	private static final String TAG = "MainActivity"; 

	/*BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			checkVersion();
		}

	};*/

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		disableKeyGuard();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		ITTApplication.getInstance().init();
		ITTApplication.getInstance().sqlController.getMsgFromDB();
		btnMenu = (Button) findViewById(R.id.btnMenu);
		btnMenu.setOnClickListener(this);

		btnUpdate = (ImageButton) findViewById(R.id.btnUpdate);
		btnUpdate.setOnClickListener(this);

		txtTrackID = (TextView) findViewById(R.id.txtTrackID);
		txtMsgTime = (TextView) findViewById(R.id.txtMsgTime);
		tv_index = (TextView) findViewById(R.id.tv_index);

		// txtDeviceID.setText(getResources().getString(R.string.device_id)
		// + ITTApplication.getInstance().info.deviceId);
		txtTrackID.setText(getResources().getString(R.string.truck_id)
				+ ITTApplication.getInstance().info.truckId + " ("
				+ ITTApplication.getInstance().info.deviceId + ")");
		setupLastMsgTime();

		menu = findViewById(R.id.menu);

		btnBackToList = (Button) findViewById(R.id.btnBackToList);
		btnBackToList.setOnClickListener(this);

		btnSetting = (Button) findViewById(R.id.btnSetting);
		btnSetting.setOnClickListener(this);

		btnContactUs = (Button) findViewById(R.id.btnContactUs);
		btnContactUs.setOnClickListener(this);
		
		imgNetworkStatus = (ImageView) findViewById(R.id.imgNetworkStatus);
		imgBatteryLevel = (ImageView) findViewById(R.id.imgBatteryLevel);
		imgMqttConnectionStatus = (ImageView) findViewById(R.id.imgMqttConnectionStatus);

		MsgViewPagerFragment fragment = new MsgViewPagerFragment();
		replaceFragment(fragment);

		System.out.println("uuid  = " + ITTApplication.getInstance().regid);

		Bundle bundle = getIntent().getExtras();
		String msg = null;
		if (bundle != null) {
			msg = bundle.getString(JPushInterface.EXTRA_MESSAGE);
			JSONObject object;
			try {
				object = new JSONObject(msg);
				if (object != null) {
					int msgType = object.getInt("msgType");
					Bundle extras = bundle.getBundle("bundle");
					if (msgType < 3) {
						actionFromNotify(msgType, object);
					}
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("start MQTTservice..."+ITTApplication.getInstance().getDeviceID());

		receiverRegister();
		
		// start MQTTService
		Intent mqttServiceIntent = new Intent(this, MQTTservice.class);
		mqttServiceIntent.putExtra(MQTTservice.MQTT_CLIENT_ID,ITTApplication.getInstance().getDeviceID());		
        startService(mqttServiceIntent);

		//checkNetworkStatus();
		//checkVersion();
		updateDeviceIdMapping();
	}

	public void disableKeyGuard() {
		getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
						| WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
	}

	private void actionFromNotify(int msgType, JSONObject extras) {
		try {
			if (msgType == 0) {
				// new msg
				getMsg(extras.getString("MessageID"));

			} else if (msgType == 1) {
				// new TID
				ITTApplication.getInstance().saveTID(extras.getString("TID"));
				txtTrackID.setText(getResources().getString(R.string.truck_id)
						+ ITTApplication.getInstance().info.truckId + " ("
						+ ITTApplication.getInstance().info.deviceId + ")");
			} else if (msgType == 2) {
				// update check
				String version_code = extras.getString("CurrentVersion");
				checkVersion(version_code);
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String convertDateTimeFormat(String dateTimeStr, String fromPattern, String toPattern) {
		try {
			SimpleDateFormat fromFormat = new SimpleDateFormat(fromPattern);
			SimpleDateFormat toFormat = new SimpleDateFormat(toPattern);
			return toFormat.format(fromFormat.parse(dateTimeStr));
		} catch (Exception e) {
			Log.e("convertDateTimeFormat",e.getMessage(),e);
			return "";
		}
	}
	//private final String DATE_TIME_FROM_PATTERN = "yyyy/MM/dd HH:mm:ss:SSS";
	//private final String DATE_TIME_TO_PATTERN = "MM/dd HH:mm";
	
	public void setupLastMsgTime() {		
		if (MsgManager.getInstance().msgList.size() > 0) {
			// format msgDateTime = MM/DD HH:mm
			String formattedMsgDateTime = convertDateTimeFormat(MsgManager.getInstance().msgList.get(0).msgDateTime,ITTApplication.MSG_TIMESTAMP_FORMAT,ITTApplication.MSG_DATE_TIME_DISPLAY_FORMAT);
			txtMsgTime.setText(getResources().getString(R.string.msg_datetime)+formattedMsgDateTime);
		} else
			txtMsgTime.setText(getResources().getString(R.string.msg_datetime));
Log.d("setupLastMsgTime",txtMsgTime.getText().toString());		
	}

	public void setupMsgTime(int index) {
		String formattedMsgDateTime = convertDateTimeFormat(MsgManager.getInstance().msgList.get(index).msgDateTime,ITTApplication.MSG_TIMESTAMP_FORMAT,ITTApplication.MSG_DATE_TIME_DISPLAY_FORMAT);		
		txtMsgTime.setText(getResources().getString(R.string.msg_datetime) + formattedMsgDateTime);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// receiverRegister();
		active = true;
		//this.registerReceiver(receiver, new IntentFilter("check_version"));
		ITTApplication.getInstance().changeLanguage(ITTApplication.getInstance().currentLanguage);
		refreshUI();
		reloadMsg();
		checkVersion();
		checkNetworkStatus();
		stopLoading();
		
		if (pendingVersionUpdateDialog) {
			showVersionUpdateDialog();
			pendingVersionUpdateDialog = false;
		}
	}

	@Override
	protected void onPause() {
		active = false;
		super.onPause();
		// receiverUnreegister();
	}

	private void getMsg(String msgID) {
		if (this.isNetworkOnline()) {
			startLoading();
			NetworkAddress.retrieveMessage(ITTApplication.getInstance().info.deviceId, msgID, this);
		} else {
			showToast(R.string.no_connection);
		}
	}

	private void reloadMsg() {
		if (this.isNetworkOnline()) {
			startLoading();
			NetworkAddress.retrieveMessageReload(ITTApplication.getInstance().info.deviceId, this);
		} else {
			showToast(R.string.no_connection);
		}
	}

	private Fragment currentFragment = null;
	public void replaceFragment(Fragment fragment) {		
		if(!active)return;
		
		currentFragment = fragment;
		FragmentTransaction transaction = getSupportFragmentManager()
				.beginTransaction();
		transaction.replace(R.id.fragment_container, fragment);
		transaction.commitAllowingStateLoss();
	}

	@Override
	public void onBackPressed() {
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		return super.onOptionsItemSelected(item);
	}

	private void checkVersion() {
		if (this.isNetworkOnline()) {
			NetworkAddress.checkVersion(this);
		} else {
			showToast(R.string.no_connection);
		}
	}

	private void checkVersion(String version_code) {
Log.d(TAG, "is app running? "+active);
		if (!active) {
			Log.d(TAG, "start MainActivity now...");
			Intent myIntent=new Intent(this.getBaseContext(),MainActivity.class);   
			myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  
			startActivity(myIntent);
		}
		
		try {
			// if current version > app version
			//if (Double.parseDouble(version_code) > Double.parseDouble(getResources().getString(R.string.version))) {
			if (versionCompare(version_code, getResources().getString(R.string.version))>0) {
				if (!active) {
					Log.d(TAG, "App is not running, show the dialog when the App resume...");
					pendingVersionUpdateDialog = true;
				} else
					showVersionUpdateDialog();
			}

		} catch (NumberFormatException nfe) {
			Log.e(TAG,"Failed to parse version no...");
			return;
		}
	}
	
	/**
	 * Compares two version strings. 
	 * 
	 * Use this instead of String.compareTo() for a non-lexicographical 
	 * comparison that works for version strings. e.g. "1.10".compareTo("1.6").
	 * 
	 * @note It does not work if "1.10" is supposed to be equal to "1.10.0".
	 * 
	 * @param str1 a string of ordinal numbers separated by decimal points. 
	 * @param str2 a string of ordinal numbers separated by decimal points.
	 * @return The result is a negative integer if str1 is _numerically_ less than str2. 
	 *         The result is a positive integer if str1 is _numerically_ greater than str2. 
	 *         The result is zero if the strings are _numerically_ equal.
	 */
	private int versionCompare(String str1, String str2) {
	    String[] vals1 = str1.split("\\.");
	    String[] vals2 = str2.split("\\.");
	    int i = 0;
	    // set index to first non-equal ordinal or length of shortest version string
	    while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) 
	    {
	      i++;
	    }
	    // compare first non-equal ordinal number
	    if (i < vals1.length && i < vals2.length) 
	    {
	        int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
	        return Integer.signum(diff);
	    }
	    // the strings are equal or one string is a substring of the other
	    // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
	    else
	    {
	        return Integer.signum(vals1.length - vals2.length);
	    }
	}

	private void secretAction() {
		MainActivity.secretNum++;
		if (MainActivity.secretNum > 8) {
			MainActivity.secretNum = 0;
			Intent setting = new Intent(
					android.provider.Settings.ACTION_SETTINGS);
			startActivity(setting);
		}
	}

	@Override
	public void onClick(View view) {
		// TODO Auto-generated method stub

		if (view == btnMenu) {
			if (menu.getVisibility() == View.GONE)
				menu.setVisibility(View.VISIBLE);
			else
				menu.setVisibility(View.GONE);
			secretAction();
		} else if (view == btnUpdate) {
			reloadMsg();
		} else if (view == btnBackToList) {
			MsgListFragment fragment = new MsgListFragment();
			replaceFragment(fragment);
			menu.setVisibility(View.GONE);
		} else if (view == btnSetting) {
			SettingFragment fragment = new SettingFragment();
			replaceFragment(fragment);
			menu.setVisibility(View.GONE);
		} else if (view == btnContactUs) {
			ContactUsFragment fragment = new ContactUsFragment();
			replaceFragment(fragment);
			checkVersion();
			menu.setVisibility(View.GONE);
		}

		if (view != btnMenu) {
			MainActivity.secretNum = 0;
		}

	}

	@Override
	public void didActivation(String deviceID, Error error) {
		// TODO Auto-generated method stub

	}

	@Override
	public void didUpdateUUID(Error error) {
		// TODO Auto-generated method stub

	}

	@Override
	public void didRetrieveMsgReload(String tid, ArrayList<Msg> msgList,
			Error error) {
		// TODO Auto-generated method stub
		if (error == null) {
			if (findViewById(R.id.fragment_container) != null) {
				ITTApplication.getInstance().msgManager.msgList = msgList;
				SqliteController.getInstance().deleteAllData();
				addMsgList(msgList);
				Collections
						.reverse(ITTApplication.getInstance().msgManager.msgList);
				MsgViewPagerFragment msgViewPagerFragment = new MsgViewPagerFragment();
				msgViewPagerFragment.setStart_position(0);
				msgViewPagerFragment.notifyAdapterDataChange();
				replaceFragment(msgViewPagerFragment);
				updatePageIndex(1,
						ITTApplication.getInstance().msgManager.msgList.size());
				setupLastMsgTime();

				ITTApplication.getInstance().saveTID(tid);
				txtTrackID.setText(getResources().getString(R.string.truck_id)
						+ ITTApplication.getInstance().info.truckId + " ("
						+ ITTApplication.getInstance().info.deviceId + ")");
			}
		} else {
			showMsg(error.getMessage());
		}
		stopLoading();
	}

	private void addMsgList(ArrayList<Msg> msgList) {
		for (int i = 0; i < msgList.size(); i++) {
			Msg msg = msgList.get(i);
			if (!SqliteController.getInstance().checkMsgIfExist(msg.msgId)) {
				SqliteController.getInstance().putMsgToDB(msg);
			}
		}
	}

	@Override
	public void didRetrieveMsg(String tid, ArrayList<Msg> msgList, Error error) {
		// TODO Auto-generated method stub
		if (error == null) {
			if (findViewById(R.id.fragment_container) != null) {
Log.d("didRetrieveMsg", "new message received and inserted into DB, re-build msg list now...");
				addMsgList(msgList);
				if (msgList.size() > 0
						//&& !SqliteController.getInstance().checkMsgIfExist(msgList.get(0).msgId)
								) {
					/*ITTApplication.getInstance().msgManager.msgList.add(0,
							msgList.get(0));*/
					SqliteController.getInstance().getMsgFromDB();							
					// reload msg from DATABASE to ensure message order
					// push notification may not be received in sending order					
				
				} else {
					stopLoading();
					return;
				}
Log.d("didRetrieveMsg", ">> done");
				//addMsgList(msgList);
				MsgViewPagerFragment msgViewPagerFragment = null;
				if (currentFragment instanceof MsgViewPagerFragment) {
Log.d("didRetrieveMsg", "remove old fragment");
					FragmentTransaction transaction = getSupportFragmentManager()
							.beginTransaction();
					transaction.remove(currentFragment);
					transaction.commitAllowingStateLoss();
				}
				
Log.d("didRetrieveMsg", "create new fragment");
				msgViewPagerFragment = new MsgViewPagerFragment();
				
				msgViewPagerFragment.setStart_position(0);
				msgViewPagerFragment.notifyAdapterDataChange();
				replaceFragment(msgViewPagerFragment);
				updatePageIndex(1,ITTApplication.getInstance().msgManager.msgList.size());
				setupLastMsgTime();
				ITTApplication.getInstance().saveTID(tid);
				txtTrackID.setText(getResources().getString(R.string.truck_id)
						+ ITTApplication.getInstance().info.truckId + " ("
						+ ITTApplication.getInstance().info.deviceId + ")");
				
				//sendNotification(msgList.get(0).content);
				/*PowerManager pm = (PowerManager) this.getSystemService("power");
				WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
											| PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG");
				wl.acquire(6000);
				if (!active) {
Log.d("didRetrieveMsg", "not active, start activity");
					Intent i = new Intent(this, MainActivity.class);
					i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
					this.startActivity(i);
				}*/
				
			}
		} else {
			showMsg(error.getMessage());
		}
		stopLoading();
	}

	public void updatePageIndex(int index, int page) {
		if (page==0)
			index = 0;
		tv_index.setText(index + " / " + page);
	}

	@Override
	public void didCheckVersion(String currentVersion, Error error) {
Log.d(TAG,"didCheckVersion, is app running? "+active);		
		if (error == null) {
			// if current version > app version
			//if (Double.parseDouble(currentVersion) > Double.parseDouble(getResources().getString(R.string.version))) {
			if (versionCompare(currentVersion, getResources().getString(R.string.version))>0) {

				if (!active) {
					Log.d(TAG, "App is not running, show the dialog when the App resume...");
					pendingVersionUpdateDialog = true;
				} else
					showVersionUpdateDialog();
			}
		}
	}

	public class InstallAPK extends AsyncTask<String, Integer, Void> {

		ProgressDialog progressDialog;
		int status = 0;

		private Context context;

		public void setContext(Context context, ProgressDialog progress) {
			this.context = context;
			this.progressDialog = progress;
		}

		public void onPreExecute() {
			progressDialog.show();
		}

		@Override
		protected Void doInBackground(String... arg0) {
			try {
				URL url = new URL(arg0[0]);
				HttpURLConnection c = (HttpURLConnection) url.openConnection();
				c.setRequestMethod("GET");
				c.setDoOutput(true);
				c.connect();
				
	            // getting file length
	            int lengthOfFile = c.getContentLength();
Log.d("InstallAPK","lengthOfFile: "+lengthOfFile);

				File sdcard = Environment.getExternalStorageDirectory();
				File myDir = new File(sdcard,
						"Android/data/com.oneport.itt/temp");
				myDir.mkdirs();
				File outputFile = new File(myDir, "itt.apk");
				if (outputFile.exists()) {
					outputFile.delete();
				}
				FileOutputStream fos = new FileOutputStream(outputFile);

				InputStream is = c.getInputStream();

				byte[] buffer = new byte[1024];
				long total = 0;
				int len1 = 0;
				while ((len1 = is.read(buffer)) != -1) {
					total += len1;
					publishProgress((int)((total*100)/lengthOfFile));
					fos.write(buffer, 0, len1);
				}
				fos.flush();
				fos.close();
				is.close();

				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.fromFile(new File(sdcard,
						"Android/data/com.oneport.itt/temp/itt.apk")),
						"application/vnd.android.package-archive");
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // without this
																// flag android
																// returned a
																// intent error!
				context.startActivity(intent);

			} catch (FileNotFoundException fnfe) {
				status = 1;
				Log.e("File", "FileNotFoundException! " + fnfe);
			}

			catch (Exception e) {
				Log.e("UpdateAPP", "Exception " + e);
			}
			return null;
		}
		
		
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			progressDialog.setProgress(values[0]);
		}

		public void onPostExecute(Void unused) {
			progressDialog.dismiss();
			if (status == 1)
				Toast.makeText(context, "App Not Available", Toast.LENGTH_LONG)
						.show();
		}
	}

	@Override
	public void failToConnect(VolleyError error) {
Log.d(this.getClass().getSimpleName(), "failToConnect, error: "+error);
		if (!this.isNetworkOnline()) 
			showToast(R.string.no_connection);
		else {		
			showToast(R.string.server_connection_failed);
		}
		stopLoading();
	}

	public void refreshUI() {
Log.d("refreshUI","refreshUI");		
		btnBackToList.setText(this.getResources().getString(
				R.string.back_to_list));
		btnSetting.setText(this.getResources().getString(R.string.setting));
		btnContactUs
				.setText(this.getResources().getString(R.string.contact_us));

		// txtDeviceID.setText(getResources().getString(R.string.device_id)
		// + ITTApplication.getInstance().info.deviceId);
		txtTrackID.setText(getResources().getString(R.string.truck_id)
				+ ITTApplication.getInstance().info.truckId + " ("
				+ ITTApplication.getInstance().info.deviceId + ")");
		setupLastMsgTime();
	}
	
	private BroadcastReceiver reload_receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			/*
			boolean isConnected = intent.getBooleanExtra("isConnected",true);
			checkNetworkStatus();
			if (isConnected) {
Log.d("reload_receiver","reloadMsg");
				showToast(R.string.reconnected);
				reloadMsg();
			} else {
				showToast(R.string.no_connection);
			}
			*/
			reloadMsg();
		}
	};

	private BroadcastReceiver new_msg_receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = intent.getExtras();
			getMsg(bundle.getString("msgID"));
		}
	};

	private BroadcastReceiver tid_updater_receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// update tid
			Bundle bundle = intent.getExtras();
			txtTrackID.setText(getResources().getString(R.string.truck_id)
					+ ITTApplication.getInstance().info.truckId + " ("
					+ ITTApplication.getInstance().info.deviceId + ")");
		}
	};

	private BroadcastReceiver version_check_receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = intent.getExtras();
			String version_code = bundle.getString("version");			
			checkVersion(version_code);
		}
	};
	
	private BroadcastReceiver connectivity_receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			checkNetworkStatus();
			if (NETWORK_STATUS_CONNECTED==getNetworkStatus()) {
				reloadMsg();
				checkVersion();
			}
		}
	};

	private void receiverRegister() {
		IntentFilter mFilter01 = new IntentFilter("new_msg");
		registerReceiver(new_msg_receiver, mFilter01);
		IntentFilter mFilter02 = new IntentFilter("tid_update");
		registerReceiver(tid_updater_receiver, mFilter02);
		IntentFilter mFilter03 = new IntentFilter("version_check");
		registerReceiver(version_check_receiver, mFilter03);
		IntentFilter mFilter04 = new IntentFilter("reload_msg");
		registerReceiver(reload_receiver,mFilter04);
		IntentFilter mFilter05 = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
		registerReceiver(connectivity_receiver,mFilter05);
		IntentFilter mFilter06 = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		registerReceiver(battery_changed_receiver,mFilter06);
		IntentFilter mFilter07 = new IntentFilter("mqtt_status");
		registerReceiver(mqttStatusReceiver,mFilter07);
	}

	private void receiverUnregister() {
		if (new_msg_receiver != null) {
			unregisterReceiver(new_msg_receiver);
		}
		if (tid_updater_receiver != null) {
			unregisterReceiver(tid_updater_receiver);
		}
		if (version_check_receiver != null) {
			unregisterReceiver(version_check_receiver);
		}
		if (reload_receiver != null) {
			unregisterReceiver(reload_receiver);
		}
		if (connectivity_receiver != null) {
			unregisterReceiver(connectivity_receiver);
		}
		if (battery_changed_receiver != null) {
			unregisterReceiver(battery_changed_receiver);
		}
		if (mqttStatusReceiver != null) {
			unregisterReceiver(mqttStatusReceiver);
		}
	}

	@Override
	public void onUpdateClick() {
		InstallAPK apk = new InstallAPK();
		progress = new ProgressDialog(MainActivity.this);
		progress.setCancelable(false);
		progress.setMessage("Downloading...");
		progress.setMax(100);
		progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		apk.setContext(getApplicationContext(), progress);
		apk.execute(ITTApplication.ITT_APK_LOC);
		dialog.dismiss();
	}
	
	private void checkNetworkStatus() {
		Log.d("checkNetworkStatus","checkNetworkStatus");
		
		this.updateNetworkStatusImage(this.getNetworkStatus());
	}
	
	private void updateNetworkStatusImage(int networkStatus) {
Log.d("networkStatusImage","networkStatus: "+networkStatus);
		int resId;
		switch (networkStatus) {
		case NETWORK_STATUS_FLIGHT_MODE:
			//resId = R.drawable.circle_black_16_ns;
			resId = R.drawable.ic_network_status_flight_mode;
			break;
			
		case NETWORK_STATUS_DISCONNECTED:
			//resId = R.drawable.circle_red_16_ns;
			resId = R.drawable.ic_network_status_disconnected;
			break;

		case NETWORK_STATUS_CONNECTING:
			//resId = R.drawable.circle_yellow_16_ns;
			resId = R.drawable.ic_network_status_connecting;
			break;

		case NETWORK_STATUS_CONNECTED:
			//resId = R.drawable.circle_green_16_ns;
			resId = R.drawable.ic_network_status_connected;
			break;
			
		default:
			Log.e("networkStatusImage","unknown networkStatus");
			return;
		};
		
		imgNetworkStatus.setImageResource(resId);
	}

	/*@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus); 
		Log.d("Focus debug", "Focus changed !");
		if(!hasFocus) {
			Log.d("Focus debug", "Lost focus !");
			Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
			sendBroadcast(closeDialog);
		}
	}*/
	
	private int mid = 0;
	private void sendNotification(String message) {
		Context context = getBaseContext();
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,new Intent(context, MainActivity.class), 0);
			
		//build the notification
		Builder notificationCompat = new Builder(context);
		notificationCompat.setAutoCancel(true)  
		        .setContentIntent(pendingIntent)
				//.setFullScreenIntent(pendingIntent, true)
				.setContentTitle("ITT")
		        .setContentText(message)
		        .setSmallIcon(R.drawable.ic_launcher);

		Notification notification = notificationCompat.build();
		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(mid++, notification);
	}
	
	private void showVersionUpdateDialog() {
		//dialog = new UpdateDialog(this);
		dialog = new UpdateDialog();
		dialog.setListener(this);
		dialog.setCancelable(false);
		dialog.setStyle(DialogFragment.STYLE_NO_FRAME,
				R.style.fullpagedialog);
		dialog.show(getSupportFragmentManager(), "UpdateDialog");
	}

	private void updateDeviceIdMapping() {
		String regId = ITTApplication.getInstance().regid;
		String deviceId = ITTApplication.getInstance().getDeviceID();
		String appVersion = getResources().getString(R.string.version);
		//if (regId!=null && regId.length()>0 && deviceId!=null && deviceId.length()>0) {
		if (!AppUtils.isBlank(regId) && !AppUtils.isBlank(deviceId)) {
			if (this.isNetworkOnline()) {
				Log.d(TAG,String.format("updateDeviceIdMapping %s,%s",regId,deviceId));
				NetworkAddress.updateUUID(regId,deviceId,appVersion,this);
			} 
			else {
				showToast(R.string.no_connection);
			}
		}
	}

	private BroadcastReceiver battery_changed_receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			//int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
			//int batteryPct = level / scale;
			
			int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
			boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
			                     status == BatteryManager.BATTERY_STATUS_FULL;

			
Log.d("batteryChangedReceiver","level: "+level+", battery level: "+level+", is charging? "+isCharging);
			
			//imgBatteryLevel.setImageBitmap(loadBatteryLevelBitmap(level));
			int batteryLvlImgResId = R.drawable.ic_battery_unknown_black_18dp;
			if (level==100)
				batteryLvlImgResId = isCharging?R.drawable.ic_battery_charging_full_black_18dp:R.drawable.ic_battery_full_black_18dp;
			else if (level>=90)
				batteryLvlImgResId = isCharging?R.drawable.ic_battery_charging_90_black_18dp:R.drawable.ic_battery_90_black_18dp;
			else if (level>=80)
				batteryLvlImgResId = isCharging?R.drawable.ic_battery_charging_80_black_18dp:R.drawable.ic_battery_80_black_18dp;
			else if (level>=60)
				batteryLvlImgResId = isCharging?R.drawable.ic_battery_charging_60_black_18dp:R.drawable.ic_battery_60_black_18dp;
			else if (level>=50)
				batteryLvlImgResId = isCharging?R.drawable.ic_battery_charging_50_black_18dp:R.drawable.ic_battery_50_black_18dp;
			else if (level>=30)
				batteryLvlImgResId = isCharging?R.drawable.ic_battery_charging_30_black_18dp:R.drawable.ic_battery_30_black_18dp;
			else if (level>=20)
				batteryLvlImgResId = isCharging?R.drawable.ic_battery_charging_20_black_18dp:R.drawable.ic_battery_20_black_18dp;
			else if (level>=0)
				batteryLvlImgResId = isCharging?R.drawable.ic_battery_charging_20_black_18dp:R.drawable.ic_battery_unknown_black_18dp;
			
			imgBatteryLevel.setImageResource(batteryLvlImgResId);
		}
	}; 
	
	private Bitmap loadBatteryLevelBitmap(int batteryLevel) {
		
		InputStream is = this.getResources().openRawResource(+R.drawable.battery_level_vertical);
		try {
			BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(is, false);
			//int height = decoder.getHeight();
			//int width = decoder.getWidth();
			int heightPerLevel = 40;//decoder.getHeight()/10;
			int widthPerLevel = 30;//decoder.getWidth()/11;
Log.d("loadBatteryLevelBitmap","batteryLevel: "+batteryLevel);

			Rect r = null;
			if (batteryLevel==100) {
				int left = 10*widthPerLevel;
				int right = 11*widthPerLevel;
				int top = 9*heightPerLevel;
				int bottom = 10*heightPerLevel;
				r = new Rect(left,top,right,bottom);
			} else {
				int left = (batteryLevel%10)*widthPerLevel;
				int right = (batteryLevel%10+1)*widthPerLevel;
				int top = (batteryLevel/10)*heightPerLevel;
				int bottom = (batteryLevel/10+1)*heightPerLevel;
				r = new Rect(left,top,right,bottom);
			}
			
			return decoder.decodeRegion(r, null);
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
	}
	

	private BroadcastReceiver mqttStatusReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(this.getClass().getSimpleName(),"MQTT connect status update...");
			if (intent!=null) {
				Bundle extras = intent.getExtras();
				if (extras!=null) {
					Log.d(this.getClass().getSimpleName(),"status: "+extras.get("status"));
					MQTTservice.CONNECT_STATE connStatus = (MQTTservice.CONNECT_STATE)extras.get("status");					
					switch (connStatus) {
					case CONNECTED:
						imgMqttConnectionStatus.setImageResource(R.drawable.green_corner);
						//imgMqttConnectionStatus.setVisibility(View.VISIBLE);
						imgMqttConnectionStatus.setVisibility(View.GONE);
						break;
					case CONNECTING:
						imgMqttConnectionStatus.setImageResource(R.drawable.yellow_corner);
						//imgMqttConnectionStatus.setVisibility(View.VISIBLE);
						imgMqttConnectionStatus.setVisibility(View.GONE);
						break;
					case DISCONNECTED:
						imgMqttConnectionStatus.setImageResource(R.drawable.red_corner);
						//imgMqttConnectionStatus.setVisibility(View.VISIBLE);
						imgMqttConnectionStatus.setVisibility(View.GONE);
						break;
					default:
						imgMqttConnectionStatus.setVisibility(View.GONE);
						break;
					}
					
				}
			}
		}
	};	
}
