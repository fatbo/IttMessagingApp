package com.oneport.itt;

import android.content.Context;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.oneport.fragment.LoadingDialogFragment;
import com.oneport.fragment.MsgAlertDialog;
import com.oneport.fragment.MsgAlertDialog.MsgAlertListener;

public class BaseActivity extends FragmentActivity {

	protected boolean active = false;
	private MsgAlertDialog msgDialog;
	private LoadingDialogFragment loadingDialogFragment;

	public void showMsg(int resId) {
		//Log.d("showMsg", "isActive? " + active);
		if (!active)
			return;
		//Log.d("showMsg", "resources==null? " + (getResources() == null));
		if (getResources() == null)
			return;
		showMsg(getString(resId));
	}

	public void showMsg(String title) {
		if (!active)
			return;
		//Log.d("showMsg", "showMsg " + title);
		msgDialog = new MsgAlertDialog(title);
		msgDialog.setStyle(DialogFragment.STYLE_NO_FRAME,
				R.style.fullpagedialog);
		if (!msgDialog.isAdded())
			msgDialog.show(getSupportFragmentManager().beginTransaction(),
					"msgDialog", true);

	}

	public void showMsg(int resId, MsgAlertListener listener) {
		if (!active)
			return;
		msgDialog = new MsgAlertDialog(getString(resId), listener);
		msgDialog.setStyle(DialogFragment.STYLE_NO_FRAME,
				R.style.fullpagedialog);
		if (!msgDialog.isAdded())
			msgDialog.show(getSupportFragmentManager().beginTransaction(),
					"msgDialog", true);
	}

	public void startLoading() {

		if (loadingDialogFragment == null) {
			loadingDialogFragment = new LoadingDialogFragment();
			loadingDialogFragment.setStyle(DialogFragment.STYLE_NO_FRAME,R.style.fullpagedialog);
			loadingDialogFragment.show(getSupportFragmentManager()
					.beginTransaction(), "JCLoadingDialogFragment", true);

		}
	}

	public void stopLoading() {
		if (loadingDialogFragment != null) {
			loadingDialogFragment.dismissAllowingStateLoss();
			loadingDialogFragment = null;
		}
	}

	public boolean isNetworkOnline() {
		boolean status = false;
		try {			
			ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo netInfo = cm.getNetworkInfo(0);
			if (netInfo != null
					&& netInfo.getState() == NetworkInfo.State.CONNECTED) {
				status = true;
			} else {
				netInfo = cm.getNetworkInfo(1);
				if (netInfo != null
						&& netInfo.getState() == NetworkInfo.State.CONNECTED)
					status = true;
			}
			/*
			// is the connection has internet access?
			if (status) {
		        try {
		            HttpURLConnection urlc = (HttpURLConnection) (new URL("http://www.google.com").openConnection());
		            urlc.setRequestProperty("User-Agent", "Test");
		            urlc.setRequestProperty("Connection", "close");
		            urlc.setConnectTimeout(1500); 
		            urlc.connect();
		            return (urlc.getResponseCode() == 200);
		        } catch (IOException e) {
		            Log.e(LOG_TAG, "Error checking internet connection", e);
		        }
			}*/
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return status;
	}

	protected static final int NETWORK_STATUS_FLIGHT_MODE = 0;
	protected static final int NETWORK_STATUS_DISCONNECTED = 1;
	protected static final int NETWORK_STATUS_CONNECTING = 2;
	protected static final int NETWORK_STATUS_CONNECTED = 3;
	
	protected int getNetworkStatus() {
		//Log.d("checkNetworkStatus","checkNetworkStatus");

		// is in flight mode?
		//check the version     
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {//if less than verson 4.2
			//Log.d("checkNetworkStatus","><><> "+Settings.System.getInt(getContentResolver(),Settings.System.AIRPLANE_MODE_ON, 0));
			if (Settings.System.getInt(getContentResolver(),Settings.System.AIRPLANE_MODE_ON, 0)==1)
				return NETWORK_STATUS_FLIGHT_MODE;          
		} else {
			//Log.d("checkNetworkStatus","><><> "+Settings.Global.getInt(getContentResolver(),Settings.Global.AIRPLANE_MODE_ON, 0));
			if (Settings.Global.getInt(getContentResolver(),Settings.Global.AIRPLANE_MODE_ON, 0)==1)
				return NETWORK_STATUS_FLIGHT_MODE; 
		}
		
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getNetworkInfo(0);
		if (netInfo != null
				&& netInfo.getState() == NetworkInfo.State.CONNECTED) {
			return NETWORK_STATUS_CONNECTED;
		} else {
			netInfo = cm.getNetworkInfo(1);
			if (netInfo != null
					&& netInfo.getState() == NetworkInfo.State.CONNECTED)
				return NETWORK_STATUS_CONNECTED;
		}
		
		return NETWORK_STATUS_DISCONNECTED;
	}

	// show single toast only
	private Toast mToast;
	public void showToast(int resId) {
		if (!active)
			return;
		if (getResources() == null)
			return;
		if (mToast!=null)
			mToast.cancel();
		mToast = Toast.makeText(getApplicationContext(), getString(resId), Toast.LENGTH_SHORT);
		mToast.show();
	}

	private WindowManager manager;
	private customViewGroup view;
	private WindowManager.LayoutParams localLayoutParams;
	private void initCustomViewGroup() {
		if (manager==null) {
			manager = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE));
			localLayoutParams = new WindowManager.LayoutParams();
			localLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
			localLayoutParams.gravity = Gravity.TOP;
			localLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
					// this is to enable the notification to recieve touch events
					|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
					// Draws over status bar
					|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
			
			localLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
			localLayoutParams.height = (int) (50 * getResources().getDisplayMetrics().scaledDensity);
			localLayoutParams.format = PixelFormat.TRANSPARENT;
	
			view = new customViewGroup(this);
		}
		manager.addView(view, localLayoutParams);
	}	

	private void removeCustomViewGroup() {
		if (manager!=null)
			manager.removeView(view);
	}

	public class customViewGroup extends ViewGroup {
		public customViewGroup(Context context) {
			super(context);
		}
		@Override
		protected void onLayout(boolean changed, int l, int t, int r, int b) {
		}
		@Override
		public boolean onInterceptTouchEvent(MotionEvent ev) {
			Log.v("customViewGroup", "**********Intercepted");
			return true;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}	

	@Override
	protected void onStart() {
		super.onStart();
		//initCustomViewGroup();
	}
	

	@Override
	protected void onStop() {
		super.onStop();
		//removeCustomViewGroup();		
	}
}
