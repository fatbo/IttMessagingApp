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
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
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
import com.oneport.model.Info;
import com.oneport.model.Msg;
import com.oneport.mqtt.MQTTservice;
import com.oneport.network.NetworkAddress;
import com.oneport.network.NetworkDelegate;

public class MainActivity extends BaseActivity implements OnClickListener,
		NetworkDelegate, UpdateListener, LocationListener {

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
	
	private Button btnLogout;
	private ViewGroup mainLayout;

	private static final String TAG = "MainActivity";
	
	//private LocationManager locationManager;
	//private Location location;

	/*BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			checkVersion();
		}

	};*/

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		disableKeyGuard();
		super.onCreate(savedInstanceState);

		//locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		//locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60*1000, 0, this);
		//location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		
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
		Info info = ITTApplication.getInstance().getInfo();
		txtTrackID.setText(getResources().getString(R.string.truck_id) + info.truckId + " ("+ info.deviceId + ")");
		setupLastMsgTime();

		menu = findViewById(R.id.menu);

		btnBackToList = (Button) findViewById(R.id.btnBackToList);
		btnBackToList.setOnClickListener(this);

		btnSetting = (Button) findViewById(R.id.btnSetting);
		btnSetting.setOnClickListener(this);

		btnContactUs = (Button) findViewById(R.id.btnContactUs);
		btnContactUs.setOnClickListener(this);

		btnLogout = (Button) findViewById(R.id.btnLogout);
		btnLogout.setOnClickListener(this);
		
		mainLayout = (ViewGroup)findViewById(R.id.mainLayout);
		mainLayout.setOnClickListener(this);
		
		//imgNetworkStatus = (ImageView) findViewById(R.id.imgNetworkStatus);
		//imgBatteryLevel = (ImageView) findViewById(R.id.imgBatteryLevel);
		//imgBatteryLevel.setImageBitmap(loadBatteryLevelBitmap(100));

		MsgViewPagerFragment fragment = new MsgViewPagerFragment();
		replaceFragment(fragment);

		//System.out.println("uuid  = " + ITTApplication.getInstance().regid);

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
Log.i("onCreate","start push service..."+ITTApplication.getInstance().getDeviceID());		
		startPushService();
Log.i("onCreate","push service...done");        
		//checkNetworkStatus();
		receiverRegister();
		//checkVersion();
		//updateDeviceIdMapping();
		
		// start timeout timer
		startTimer();
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
				Info info = ITTApplication.getInstance().getInfo();
				txtTrackID.setText(getResources().getString(R.string.truck_id)
						+ info.truckId + " ("
						+ info.deviceId + ")");
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
	}

	public void setupMsgTime(int index) {
		String formattedMsgDateTime = convertDateTimeFormat(MsgManager.getInstance().msgList.get(index).msgDateTime,ITTApplication.MSG_TIMESTAMP_FORMAT,ITTApplication.MSG_DATE_TIME_DISPLAY_FORMAT);		
		txtMsgTime.setText(getResources().getString(R.string.msg_datetime) + formattedMsgDateTime);
	}

	@Override
	protected void onResume() {
		super.onResume();
Log.d(this.getClass().getCanonicalName(), "onResume");
		
		active = true;
		//this.registerReceiver(receiver, new IntentFilter("check_version"));
		ITTApplication.getInstance().changeLanguage(
				ITTApplication.getInstance().currentLanguage);
		refreshUI();
		checkNetworkStatus();
		reloadMsg();
		checkVersion();
		updateDeviceIdMapping();
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
Log.d(this.getClass().getCanonicalName(), "onPause");
	}

	private void getMsg(String msgID) {
		if (this.isNetworkOnline()) {
			startLoading();
			NetworkAddress.retrieveMessage(ITTApplication.getInstance().getInfo().deviceId, msgID, this);
		} else {
			showToast(R.string.no_connection);
		}
	}

	private void reloadMsg() {
		if (this.isNetworkOnline()) {
			startLoading();
			NetworkAddress.retrieveMessageReload(ITTApplication.getInstance().getInfo().deviceId, this);
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
			Log.d(TAG, "version diff: "+(versionCompare(version_code, getResources().getString(R.string.version))));			
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
//Log.d("onClick","view: "+view);
		if (view == btnMenu) {
			if (menu.getVisibility() == View.GONE)
				menu.setVisibility(View.VISIBLE);
			else
				menu.setVisibility(View.GONE);
			//secretAction();
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
		} else if (view == btnLogout) {
			// prompt confirmation
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int which) {
                         switch (which) {
                         case DialogInterface.BUTTON_POSITIVE:
                    			menu.setVisibility(View.GONE);
                    			// invalidate the session
                    			Intent intent = new Intent(MainActivity.this,LoginActivity.class);
                    			startActivity(intent);
                    			finish();
                                break;

                         case DialogInterface.BUTTON_NEGATIVE:
                                break;
                         }
                   }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.logout_confirm))
                         .setPositiveButton(getString(R.string.logout_btn_yes), dialogClickListener)
                         .setNegativeButton(getString(R.string.logout_btn_no), dialogClickListener)
                         .show();
            
		} else if (view==mainLayout) {
			menu.setVisibility(View.GONE);
		}

		/*if (view != btnMenu) {
			MainActivity.secretNum = 0;
		}*/

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
	public void didLogin(int returnCode, String remark, String timestampStr, Error error) {
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
				Info info = ITTApplication.getInstance().getInfo();
				txtTrackID.setText(getResources().getString(R.string.truck_id)
						+ info.truckId + " ("
						+ info.deviceId + ")");
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
				Info info = ITTApplication.getInstance().getInfo();
				txtTrackID.setText(getResources().getString(R.string.truck_id)
						+ info.truckId + " ("
						+ info.deviceId + ")");
				
				if (!this.active) {
					sendNotification(msgList.get(0).content);
				}
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
		if (!this.isNetworkOnline()) 
			showToast(R.string.no_connection);
		else {		
			showToast(R.string.server_connection_failed);
		}
		stopLoading();
	}

	public void refreshUI() {
		btnBackToList.setText(this.getResources().getString(R.string.back_to_list));
		btnSetting.setText(this.getResources().getString(R.string.setting));
		btnContactUs.setText(this.getResources().getString(R.string.contact_us));
		btnLogout.setText(this.getResources().getString(R.string.btn_logout));

		Info info = ITTApplication.getInstance().getInfo();
		txtTrackID.setText(getResources().getString(R.string.truck_id)
				+ info.truckId + " ("
				+ info.deviceId + ")");
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
			Info info = ITTApplication.getInstance().getInfo();
			txtTrackID.setText(getResources().getString(R.string.truck_id)
					+ info.truckId + " ("
					+ info.deviceId + ")");
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
		IntentFilter mFilter06 = new IntentFilter("force_logout");
		registerReceiver(forceLogoutReceiver,mFilter06);
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
		/*if (receiver != null) {
			unregisterReceiver(receiver);
		}*/
		if (forceLogoutReceiver!=null) {
			unregisterReceiver(forceLogoutReceiver);
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
		
		//this.updateNetworkStatusImage(this.getNetworkStatus());
	}
	
	private void updateNetworkStatusImage(int networkStatus) {
Log.d("updateNetworkStatusImage","networkStatus: "+networkStatus);
		int resId;
		switch (networkStatus) {
		case NETWORK_STATUS_FLIGHT_MODE:
			resId = R.drawable.circle_black_16_ns;
			break;
			
		case NETWORK_STATUS_DISCONNECTED:
			resId = R.drawable.circle_red_16_ns;
			break;

		case NETWORK_STATUS_CONNECTING:
			resId = R.drawable.circle_yellow_16_ns;
			break;

		case NETWORK_STATUS_CONNECTED:
			resId = R.drawable.circle_green_16_ns;
			break;
			
		default:
			Log.e("updateNetworkStatusImage","unknown networkStatus");
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
		nm.notify(ITTApplication.NOTIFICATION_ID_NEW_MESSAGE, notification);
	}
	
	private void showVersionUpdateDialog() {
		dialog = new UpdateDialog(this);
		dialog.setCancelable(false);
		dialog.setStyle(DialogFragment.STYLE_NO_FRAME,
				R.style.fullpagedialog);
		dialog.show(getSupportFragmentManager(), "UpdateDialog");
	}

	private void updateDeviceIdMapping() {
		String regId = ITTApplication.getInstance().regid;
		String deviceId = ITTApplication.getInstance().getDeviceID();
		String appVersion = getResources().getString(R.string.version);
				
/*Log.d("updateDeviceIdMapping","jpush regId: "+JPushInterface.getRegistrationID(this));		
Log.d("updateDeviceIdMapping","need updateDeviceIdMapping? "+(!AppUtils.isBlank(regId) && !AppUtils.isBlank(deviceId)));
Log.d("updateDeviceIdMapping","regId: "+regId);
Log.d("updateDeviceIdMapping","deviceId: "+deviceId);*/
		
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
Log.d("battery_changed_receiver","level: "+level+", battery level: "+level);
			
			imgBatteryLevel.setImageBitmap(loadBatteryLevelBitmap(level));
		}
	};
	
	private Bitmap loadBatteryLevelBitmap(int batteryLevel) {
		
		InputStream is = this.getResources().openRawResource(R.drawable.battery_level_vertical);
		try {
			BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(is, false);
			//int height = decoder.getHeight();
			//int width = decoder.getWidth();
			int heightPerLevel = 40;//decoder.getHeight()/10;
			int widthPerLevel = 30;//decoder.getWidth()/11;
			
			int widthOffset = 2;
Log.d("loadBatteryLevelBitmap","batteryLevel: "+batteryLevel);

			Rect r = null;
			if (batteryLevel==100) {
				int left = 10*widthPerLevel + widthOffset;
				int right = 11*widthPerLevel + widthOffset;
				int top = 9*heightPerLevel;
				int bottom = 10*heightPerLevel;
				r = new Rect(left,top,right,bottom);
			} else {
				int left = (batteryLevel%10)*widthPerLevel + widthOffset;
				int right = (batteryLevel%10+1)*widthPerLevel + widthOffset;
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

	@Override
	public void onLocationChanged(Location location) {
	    double latitude = location.getLatitude();
	    double longitude = location.getLongitude();

Log.i("Geo_Location", "Latitude: " + latitude + ", Longitude: " + longitude+", provider: "+location.getProvider());
		/*if (service!=null) {
			Message pubMsg = Message.obtain(null,MQTTservice.PUBLISH);
			Bundle pubData = pubMsg.getData();
			pubData.putString(MQTTservice.TOPIC,"IttMessagingApp/gps/"+ITTApplication.getInstance().getInfo().deviceId);
			pubData.putString(MQTTservice.MESSAGE,"");
			pubData.putBoolean(MQTTservice.RETAINED, true);
			try {
				service.send(pubMsg);
			} catch (Exception e) {
				Log.e("onLocationChanged","Exception occurred",e);
			}
		}*/
	}

	@Override	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
Log.i("Geo_Location", String.format("provider: %s, status: %d",provider,status));
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}
	
	private Messenger service;
    private ServiceConnection serviceConnection = new ServiceConnection()
    {
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder binder)
		{
			service = new Messenger(binder);
			
			// subscribe and publish connection status
		    // subscribe message topic for specified deviceId
		    /*Message subMsg = Message.obtain(null, MQTTservice.SUBSCRIBE);
		    Bundle subData = subMsg.getData();
		    subData.putString(MQTTservice.TOPIC,messageTopic);
			msgHandler.sendMessage(subMsg);*/
			
			// publish connection status to topic
			/*Message pubMsg = Message.obtain(null, MQTTservice.PUBLISH);
			Bundle pubData = pubMsg.getData();
			pubData.putString(MQTTservice.TOPIC,connectionStatusTopic);
			pubData.putString(MQTTservice.MESSAGE,String.format("login@%s",defaultSdf.format(new Date())));
			pubData.putBoolean(MQTTservice.RETAINED, true);
			msgHandler.sendMessage(pubMsg);*/
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0)
		{
			service = null;
		}
    };

	@Override
	protected void onStart() {
		super.onStart();
Log.d(this.getClass().getSimpleName(),"onStart, bindService now...");		
		bindService(new Intent(this, MQTTservice.class), serviceConnection, 0);
	}

	@Override
	protected void onStop() {
		super.onStop();
Log.d(this.getClass().getCanonicalName(), "onStop");
	}

	@Override
	protected void onDestroy() {
		logoutAction();
		super.onDestroy();
		Log.d(this.getClass().getCanonicalName(), "onDestroy");
	}
	
	private void stopPushService() {
		try {
			JPushInterface.stopPush(this);
			
			service.send(Message.obtain(null, MQTTservice.STOP));
			unbindService(serviceConnection);			
		} catch (Exception e) {
			Log.e(this.getClass().getCanonicalName(),"Exception occurred in stopPushService()",e);
		}
	}
	
	private void startPushService() {
        JPushInterface.resumePush(this);
        
		// start MQTTService
		Intent mqttServiceIntent = new Intent(this, MQTTservice.class);
		mqttServiceIntent.putExtra(MQTTservice.MQTT_CLIENT_ID,ITTApplication.getInstance().getDeviceID());		
        startService(mqttServiceIntent);
	}
	
	private void logoutAction() {
		//locationManager.removeUpdates(this);
		stopTimer();
		// stop push service (mqtt & jpush)
		stopPushService();
		// unregister all receivers
		receiverUnregister();
		// TODO invalidate session
		ITTApplication.getInstance().getInfo().logined = false;
		ITTApplication.getInstance().saveLogin();
	}

	private BroadcastReceiver forceLogoutReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String forceLogoutBy = intent.getExtras().getString("ForceLogoutBy");
			String forceLogoutTime = intent.getExtras().getString("Timestamp");
			Log.d(this.getClass().getCanonicalName(),String.format("force logout by %s at %s",forceLogoutBy,forceLogoutTime));
			String lastLoginTime = ITTApplication.getInstance().getInfo().lastLoginTime;
			Log.d(this.getClass().getCanonicalName(),String.format("last login time: %s",lastLoginTime));
			
			// compare lastLoginTime and force logout timestamp
			if (forceLogoutTime!=null
					&& lastLoginTime!=null 
					&& !lastLoginTime.trim().isEmpty()
					&& lastLoginTime.compareTo(forceLogoutTime)<=0) {
				Intent i = new Intent(MainActivity.this,LoginActivity.class);
				i.putExtra("IsForceLogout", true);
				i.putExtra("ForceLogoutBy", forceLogoutBy);
				
				startActivity(i);
				finish();
			} else {
				Log.d(this.getClass().getCanonicalName(),"this login session is more recent than the last force logout event");
			}			
		}

	};

	private final Handler handler = new Handler();
	private Timer timer;
	private TimerTask timerTask;
	private long lastInteraction=System.currentTimeMillis();
	
	@Override
	public void onUserInteraction() {		
		super.onUserInteraction();
		// update lastInteraction
		lastInteraction = System.currentTimeMillis();
	}
	private void startTimer() {
		timer = new Timer();
		initializeTimerTask();
		// execute every 60 seconds
		timer.schedule(timerTask, 5000, 60*1000);
	}
	private void stopTimer() {
		if (timer!=null) {
			timer.cancel();
			timer = null;
		}
	}
	public void initializeTimerTask() {
		timerTask = new TimerTask() {
			public void run() {
				//use a handler to run a toast that shows the current timestamp
				handler.post(new Runnable() {
					public void run() {
						// store last interaction time
						ITTApplication.getInstance().saveLastInteraction(MainActivity.this, lastInteraction);
						// if idle > X hours, auto logout
						long currentTime = System.currentTimeMillis();
						if ((currentTime-lastInteraction)>ITTApplication.DEFAULT_TIMEOUT_PERIOD) {
							Log.i(this.getClass().getCanonicalName(), String.format("User inactivity %d > %d (system timeout period), logout now...",(currentTime-lastInteraction),ITTApplication.DEFAULT_TIMEOUT_PERIOD));
							
							Intent i = new Intent(MainActivity.this,LoginActivity.class);
							i.putExtra("IsSessionTimeout", true);
							startActivity(i);
							finish();
						} else {
							Log.d(this.getClass().getCanonicalName(), String.format("not yet timeout, %d < %d",(currentTime-lastInteraction),ITTApplication.DEFAULT_TIMEOUT_PERIOD));
						}
					}
				});
			}
		};
	}
}
