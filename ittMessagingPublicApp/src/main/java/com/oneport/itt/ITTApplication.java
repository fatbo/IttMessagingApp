package com.oneport.itt;

//import io.fabric.sdk.android.Fabric;

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
import android.util.DisplayMetrics;
import android.util.Log;
import cn.jpush.android.api.JPushInterface;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
//import com.crashlytics.android.Crashlytics;
import com.oneport.itt.utils.AppUtils;
import com.oneport.manager.MsgManager;
import com.oneport.manager.SqliteController;
import com.oneport.model.Info;
import com.oneport.model.Msg;

public class ITTApplication extends Application {
	private static ITTApplication singleton;

	public MsgManager msgManager;
	SqliteController sqlController;
	
	static final String TAG = "OnePort";
	
	AtomicInteger msgId = new AtomicInteger();
	String regid;
	String tid;
	
	private Info info = new Info();
	public Info getInfo() {
		return info;
	}

	public static boolean renewReceiver = false;
	public static final String EXTRA_MESSAGE = "message";
	private static final String PROPERTY_REG_ID = "registration_id";
	private static final String PROPERTY_APP_VERSION = "appVersion";
	private static final String PROPERTY_LAST_INTERACTION = "lastInteraction";
	private static final String PROPERTY_MQTT_VERIFIED_STATUS = "mqttVerifiedStatus";
	
	public final static String ITT_APK_LOC = "http://www.e1port.com/itt/ITT_MESSENGER.apk";
	//public final static String ITT_APK_LOC = "http://www.oneport.com/itt/ITT_MESSENGER.apk";
	
	public final static String ITT_DOMAIN = "http://itt.e1port.com/IttWeb/IttAppServlet?";
	//public final static String ITT_DOMAIN = "http://itt.oneport.com/IttWeb/IttAppServlet?";

	public final static String DEFAULT_DATE_TIME_FORMAT = "yyyy/MM/dd HH:mm:ss:SSS";
	public final static String MSG_TIMESTAMP_FORMAT = "yyyy/MM/dd HH:mm:ss:SSS";
	public final static String MSG_DATE_TIME_DISPLAY_FORMAT = "MM/dd HH:mm";
	
	public final static int NOTIFICATION_ID_NEW_MESSAGE = 1;
	public final static long DEFAULT_TIMEOUT_PERIOD = (2*60*60*1000); // default timeout = 2 hours without user interaction	

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
		//Fabric.with(this, new Crashlytics());
		singleton = this;
		initLocates();
		checkLanguage();
		load();
Log.d("onCreate","reg id: "+info.registrationId);		
		initJPush();
	}

	int getCurrentVersion() {
		return getAppVersion(singleton);
	}

	private void initJPush() {
		JPushInterface.setDebugMode(true);
		JPushInterface.resumePush(this);
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

	/*public void save() {
		SharedPreferences settings = getApplicationContext()
				.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
	}*/

	public void saveLogin() {
		SharedPreferences settings = getApplicationContext()
				.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		//editor.putBoolean("logined", (info.deviceId==null||info.deviceId.isEmpty())?false:true);
		editor.putBoolean("activated", info.activated);
		editor.putString("device_id", info.deviceId);
		editor.putString("registration_id", info.registrationId);
		
		editor.putBoolean("logined", info.logined);
		editor.putString("truck_id", info.truckId);
		editor.putString("tid",info.tid);
		editor.putString("password",info.password);
		editor.putString("license_number",info.licenseNumber);
		editor.putString("tractor_provider",info.tractorProvider);
		
		//SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT);
		editor.putString("last_login_time",info.lastLoginTime);
		editor.commit();
	}

	public boolean load() {
		//info = new Info();
		SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
		//boolean logined = settings.getBoolean("logined", false);
		boolean activated = settings.getBoolean("activated", false);
		if (activated) {
			info.activated = activated;
			info.deviceId = settings.getString("device_id", "");
			info.registrationId = settings.getString("registration_id", "");
			info.truckId = settings.getString("truck_id", "");
			info.logined = settings.getBoolean("logined", false);
			info.tid = settings.getString("tid", "");
			info.password = settings.getString("password", "");
			info.licenseNumber = settings.getString("license_number", "");
			info.tractorProvider = settings.getString("tractor_provider", "");
			
			//try {
				//SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT);
				//String lastLoginTimeStr = settings.getString("last_login_time",null);
				//info.lastLoginTime = lastLoginTimeStr!=null?sdf.parse(lastLoginTimeStr):null;
			
				info.lastLoginTime = settings.getString("last_login_time",null);
				
			/*} catch (ParseException pe) {
				Log.e(this.getClass().getCanonicalName(),String.format("Unable to parse lastLoginTime %s",settings.getString("last_login_time",null)));
				info.lastLoginTime = null;
			}*/
		}
		return activated;
	}

	public boolean isRegistrationIdRetrieved() {
		SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
		String regId = settings.getString("registration_id", "");
		if (regId!=null && !regId.isEmpty())
			return true;
		else 
			return false;
	}

	public int getLangIndex() {
		return currentLanguage.ordinal();
	}

	public void changeLanguage(LanguageType lang) {
		currentLanguage = lang;
		Resources res = getApplicationContext().getResources();
		DisplayMetrics dm = res.getDisplayMetrics();
		android.content.res.Configuration conf = res.getConfiguration();
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

		if (checked) {
			// get last language
			int iLang = settings.getInt("Lang", 0);
			changeLanguage(LanguageType.values()[iLang]);
		} else {
			String lang = Locale.getDefault().getLanguage();
			// Log.v("Current language",lang);

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

			}

			settings = getApplicationContext().getSharedPreferences(PREFS_NAME,
					0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("checkedLang", true);
			editor.commit();
		}
	}

	public RequestQueue getRequestQueue() {
		if (requestQueue == null) {
			//CookieManager cookieManager = new CookieManager();
			//CookieHandler.setDefault(cookieManager);			
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
	
	public void saveLastInteraction(Context context, long lastInteraction) {
		SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
		Log.v(TAG, "Saving lastInteraction " + lastInteraction);
		SharedPreferences.Editor editor = settings.edit();
		editor.putLong(PROPERTY_LAST_INTERACTION,lastInteraction);
		editor.commit();
	}
	
	public long getLastInteraction() {
		SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
		return settings.getLong(PROPERTY_LAST_INTERACTION,0);		
		//return settings.getLong(PROPERTY_LAST_INTERACTION,System.currentTimeMillis());
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
