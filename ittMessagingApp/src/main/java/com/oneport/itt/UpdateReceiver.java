package com.oneport.itt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;


public class UpdateReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, Intent intent) {

		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetInfoWIFI = connectivityManager
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		NetworkInfo activeNetInfo = connectivityManager
				.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		boolean isConnected = activeNetInfo != null
				&& activeNetInfo.isConnected()
				|| activeNetInfoWIFI != null
				&& activeNetInfoWIFI.isConnected();
		if (isConnected) {
			context.sendBroadcast(new Intent("check_version"));
			//ITTApplication.getInstance().regid = JPushInterface.getRegistrationID(context);
			//context.sendBroadcast(new Intent("reload_msg"));
		} else
			Log.i("NET", "not connected" + isConnected);
	}


}