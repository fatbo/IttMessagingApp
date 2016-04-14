package com.oneport.itt;

import io.fabric.sdk.android.Fabric;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.DisplayMetrics;
import android.util.Log;
import cn.jpush.android.api.JPushInterface;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.oneport.itt.utils.AppUtils;
import com.oneport.manager.MsgManager;
import com.oneport.manager.SqliteController;
import com.oneport.model.Info;
import com.oneport.model.Msg;

public class ITTApplication extends Application {
	private static ITTApplication singleton;

	public MsgManager msgManager;
	SqliteController sqlController;
	String SENDER_ID = "275172842363";
	static final String TAG = "OnePort";
	public GoogleCloudMessaging gcm;
	AtomicInteger msgId = new AtomicInteger();
	String regid;
	String tid;
	Info info;
	// boolean hasNewMsg = false;
	// boolean hasTidUpdate = false;
	// boolean hasNewVersion = false;
	public static boolean renewReceiver = false;
	public static final String EXTRA_MESSAGE = "message";
	public static final String PROPERTY_REG_ID = "registration_id";
	private static final String PROPERTY_APP_VERSION = "appVersion";
	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	
	public final static String ITT_APK_LOC = "http://www.e1port.com/itt/ITT.apk";
	//public final static String ITT_APK_LOC = "http://www.oneport.com/itt/ITT.apk";
	
	public final static String ITT_DOMAIN = "http://itt.e1port.com/IttWeb/IttAppServlet?";
	//public final static String ITT_DOMAIN = "http://itt.oneport.com/IttWeb/IttAppServlet?";

	public final static String MSG_TIMESTAMP_FORMAT = "yyyy/MM/dd HH:mm:ss:SSS";	
	public final static String MSG_DATE_TIME_DISPLAY_FORMAT = "MM/dd HH:mm";

	private static final String PROPERTY_MQTT_VERIFIED_STATUS = "mqttVerifiedStatus";

	public static ITTApplication getInstance() {
		return singleton;
	}

	public static final String PREFS_NAME = "ITTPREFS";
	public static final int kLocaleCount = 3;
	public static Locale locales[];

	public enum LanguageType {
		TC, SC, ENG
	};

	public static String[] sysLanguageCode = { "zh_TW", "zh_CN", "en" };

	public static LanguageType currentLanguage;

	// network
	public RequestQueue requestQueue;

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Fabric.with(this, new Crashlytics());
		singleton = this;
		initLocates();
		checkLanguage();
		load();
		initJPush();
	}

	int getCurrentVersion() {
		return getAppVersion(singleton);
	}

	private void initJPush() {
		JPushInterface.setDebugMode(true);
		JPushInterface.init(this);
		//if(info != null && info.registrationId !=null && info.registrationId.length()>0)
		if(info != null && AppUtils.isBlank(info.registrationId))
			regid = info.registrationId;		

		if(AppUtils.isBlank(regid)){
Log.v("initJPush","retrieve jpush reg id now");
			regid = JPushInterface.getRegistrationID(this);
			if (AppUtils.isBlank(regid)) {
				storeRegistrationId(this,regid);
				if (info!=null)
					info.registrationId = regid;
			}
		}
Log.v("initJPush","registration Id = "+regid);
	}

	
	/* 
	 * 
	 * google push
	 * 
	 * 
	 * */
	// private boolean isGCMReceiverRegister() {
	// ComponentName component = new ComponentName(
	// ITTApplication.getInstance(), GcmBroadcastReceiver.class);
	// return ITTApplication.getInstance().getPackageManager()
	// .getComponentEnabledSetting(component) ==
	// PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
	// }

	// @SuppressLint("NewApi")
	// public void googleplayservice() {
	// if (checkPlayServices()) {
	// gcm = GoogleCloudMessaging.getInstance(this);
	// regid = getRegistrationId(singleton);
	// if (regid.isEmpty()) {
	// registerInBackground();
	// }
	// } else {
	// Log.i(TAG, "No valid Google Play Services APK found.");
	// }
	// }
	//
	// private boolean checkPlayServices() {
	// int resultCode = GooglePlayServicesUtil
	// .isGooglePlayServicesAvailable(this);
	// if (resultCode != ConnectionResult.SUCCESS) {
	// if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
	// // GooglePlayServicesUtil.getErrorDialog(resultCode, this,
	// // PLAY_SERVICES_RESOLUTION_REQUEST).show();
	// } else {
	// Log.i(TAG, "This device is not supported.");
	// }
	// return false;
	// }
	// return true;
	// }
	//
	// @SuppressLint("NewApi")
	// private String getRegistrationId(Context context) {
	// final SharedPreferences prefs = getGCMPreferences(context);
	// String registrationId = prefs.getString(PROPERTY_REG_ID, "");
	// if (registrationId.isEmpty()) {
	// Log.i(TAG, "Registration not found.");
	// return "";
	// }
	// // Check if app was updated; if so, it must clear the registration ID
	// // since the existing regID is not guaranteed to work with the new
	// // app version.
	// int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION,
	// Integer.MIN_VALUE);
	// int currentVersion = getAppVersion(context);
	// if (registeredVersion != currentVersion) {
	// Log.i(TAG, "App version changed.");
	// return "";
	// }
	// return registrationId;
	// }
	//
	// public void cleanRegId(Context context) {
	// final SharedPreferences prefs = getGCMPreferences(context);
	// SharedPreferences.Editor edit = prefs.edit();
	// edit.putString("PROPERTY_REG_ID", "");
	// edit.apply();
	// String a = prefs.getString("PROPERTY_REG_ID", "");
	// }
	//
	// private SharedPreferences getGCMPreferences(Context context) {
	// // This sample app persists the registration ID in shared preferences,
	// // but
	// // how you store the regID in your app is up to you.
	// return getSharedPreferences(MainActivity.class.getSimpleName(),
	// Context.MODE_PRIVATE);
	// }
	// public void registerInBackground() {
	// new AsyncTask<Void, Void, String>() {
	// @Override
	// protected String doInBackground(Void... params) {
	// String msg = "";
	// try {
	// if (gcm == null) {
	// gcm = GoogleCloudMessaging.getInstance(singleton);
	// }
	// regid = gcm.register(SENDER_ID);
	// Log.i("regid after", ITTApplication.getInstance().regid);
	// msg = "Device registered, registration ID=" + regid;
	//
	// storeRegistrationId(singleton, regid);
	// } catch (IOException ex) {
	// msg = "Error :" + ex.getMessage();
	// }
	// return msg;
	// }
	//
	// @Override
	// protected void onPostExecute(String msg) {
	// Log.i("receive msg by google", msg);
	// }
	//
	// }.execute(null, null, null);
	// }

	private static int getAppVersion(Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager()
					.getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}

	private void storeRegistrationId(Context context, String regId) {
		final SharedPreferences prefs = getSharedPreferences(
				MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
		int appVersion = getAppVersion(context);
		Log.i(TAG, "Saving regId on app version " + appVersion);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PROPERTY_REG_ID, regId);
		editor.putInt(PROPERTY_APP_VERSION, appVersion);
		editor.commit();
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
	}

	public void init() {
		msgManager = new MsgManager();
		MsgManager.sharedManager = msgManager;
		msgManager.msgList = new ArrayList<Msg>();
		// MsgManager.getInstance().init();
		SqliteController.init(singleton);
		sqlController = SqliteController.getInstance();
		sqlController.setDB(sqlController.getReadableDatabase());
	}

	public String getCurrentLanguage() {
		SharedPreferences settings = getApplicationContext()
				.getSharedPreferences(PREFS_NAME, 0);
		int iLang = settings.getInt("Lang", 0);
		return this.getResources().getStringArray(R.array.language_arr)[iLang];
	}

	private void initLocates() {
		locales = new Locale[kLocaleCount];
		locales[2] = Locale.ENGLISH;
		locales[0] = Locale.TRADITIONAL_CHINESE;
		locales[1] = Locale.SIMPLIFIED_CHINESE;
	}

	public void save() {
		SharedPreferences settings = getApplicationContext()
				.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		/*
		 * editor.putBoolean("logined", logined);
		 * 
		 * if (userInfo != null) { editor.putString("username",
		 * userInfo.username); editor.putString("email", userInfo.email);
		 * editor.putString("accountToken", accountToken); editor.commit(); }
		 */
	}

	public void saveLogin() {
		SharedPreferences settings = getApplicationContext()
				.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("logined", true);
		editor.putString("device_id", info.deviceId);
		editor.putString("registration_id", info.registrationId);
		editor.putString("truck_id", info.truckId);
		editor.commit();
	}

	public boolean load() {
		SharedPreferences settings = getApplicationContext()
				.getSharedPreferences(PREFS_NAME, 0);
		boolean logined = settings.getBoolean("logined", false);
		if (logined) {
			info = new Info();
			info.deviceId = settings.getString("device_id", "");
			info.registrationId = settings.getString("registration_id", "");
			info.truckId = settings.getString("truck_id", "");
		}
		return logined;
		/*
		 * logined = settings.getBoolean("logined", false);
		 * 
		 * if (logined) { String username = settings.getString("username", "");
		 * String email = settings.getString("email", ""); userInfo = new
		 * UserInfo(username, email); accountToken =
		 * settings.getString("accountToken", ""); } else{ userInfo = new
		 * UserInfo(getResources().getString( R.string.visitor), "xxx@com.hk");
		 * }
		 */
	}

	public int getLangIndex() {
		return currentLanguage.ordinal();
	}

	public void changeLanguage(LanguageType lang) {
		currentLanguage = lang;
		Resources res = getApplicationContext().getResources();
		DisplayMetrics dm = res.getDisplayMetrics();
		Configuration conf = res.getConfiguration();
		conf.locale = locales[currentLanguage.ordinal()];
		res.updateConfiguration(conf, dm);

		SharedPreferences settings = getApplicationContext()
				.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("Lang", currentLanguage.ordinal());
		editor.commit();
	}

	private void checkLanguage() {
		SharedPreferences settings = getApplicationContext()
				.getSharedPreferences(PREFS_NAME, 0);
		boolean checked = settings.getBoolean("checkedLang", false);
Log.d("checkLanguage",Locale.getDefault().getLanguage());
		if (checked) {
			// get last language
			int iLang = settings.getInt("Lang", 0);
			changeLanguage(LanguageType.values()[iLang]);
		} else {
			/*String lang = Locale.getDefault().getLanguage();

			if (lang.contains(sysLanguageCode[0])) {
				changeLanguage(LanguageType.ENG);
			} else {
				if (lang.contains("zh")) {
					if (lang.toLowerCase().contains("cn"))
						changeLanguage(LanguageType.SC);
					else
						changeLanguage(LanguageType.TC);
				} else {
					changeLanguage(LanguageType.ENG);
				}

			}*/
			
			// set default language to zh_TW
			changeLanguage(LanguageType.TC);

			settings = getApplicationContext().getSharedPreferences(PREFS_NAME,
					0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("checkedLang", true);
			editor.commit();
		}
	}

	public RequestQueue getRequestQueue() {
		if (requestQueue == null) {
			requestQueue = Volley.newRequestQueue(getApplicationContext());
		}

		return requestQueue;
	}

	public void saveTID(String tid) {
		SharedPreferences settings = getApplicationContext()
				.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		if (info == null)
			info = new Info();
		info.truckId = tid;
		editor.putString("truck_id", tid);
		editor.commit();
	}

	public String getTID() {
		if (info == null)
			return "";
		return info.truckId;
	}
	
	public String getDeviceID() {
		SharedPreferences settings = getApplicationContext()
				.getSharedPreferences(PREFS_NAME, 0);
		return settings.getString("device_id", "");		
	}

	public void saveMqttVerifiedStatus(boolean isSucceeded) {
		SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(PROPERTY_MQTT_VERIFIED_STATUS, isSucceeded);
		editor.commit();
	}
	
	public boolean isMqttVerified() {
		SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
		return settings.getBoolean(PROPERTY_MQTT_VERIFIED_STATUS, false);
	}
}
