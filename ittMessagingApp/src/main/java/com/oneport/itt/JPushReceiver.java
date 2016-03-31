package com.oneport.itt;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import cn.jpush.android.api.JPushInterface;

public class JPushReceiver extends BroadcastReceiver {
	private static final String TAG = "JPushReceiver";

	public static final int NOTIFICATION_ID = 1;
	private NotificationManager nm;
	NotificationCompat.Builder builder;

	@Override
	public void onReceive(Context context, Intent intent) {
		if (null == nm) {
			nm = (NotificationManager) context
					.getSystemService(Context.NOTIFICATION_SERVICE);
		}

		Bundle bundle = intent.getExtras();
		/*if (JPushInterface.ACTION_CONNECTION_CHANGE.equals(intent.getAction())){
            boolean connected = intent.getBooleanExtra(JPushInterface.EXTRA_CONNECTION_CHANGE, false);
            Log.e("JPushReceiver", "[JPushReceiver]" + intent.getAction() +" connected:"+connected);
            //if (connected)
            Intent reloadMsgIntent = new Intent("reload_msg");
            reloadMsgIntent.putExtra("isConnected", connected);
            context.sendBroadcast(reloadMsgIntent);
            
		} else */
		if (JPushInterface.ACTION_MESSAGE_RECEIVED.equals(intent.getAction())) {
			String msg = bundle.getString(JPushInterface.EXTRA_MESSAGE);
			/*String msgId = bundle.getString(JPushInterface.EXTRA_MSG_ID);
			Log.d(TAG, "Push message received - msgId: " + msgId);
			JPushInterface.reportNotificationOpened(context,bundle.getString(JPushInterface.EXTRA_MSG_ID));*/
			JSONObject extras;
			try {
				extras = new JSONObject(msg);
				Log.d(TAG, "Unhandled intent - " + intent.getAction());

				PowerManager pm = (PowerManager) context
						.getSystemService("power");
				WakeLock wl = pm.newWakeLock(
						PowerManager.SCREEN_BRIGHT_WAKE_LOCK
								| PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG");
				wl.acquire();

				int msgType = Integer.parseInt(extras.getString("msgtype"));
				if (appIsRunning(context)) {
					if (msgType == 0) {
						// new msg
						Intent msgIntent = new Intent("new_msg");
						msgIntent.putExtra("msgID",
								extras.getString("MessageID"));
						context.sendBroadcast(msgIntent);
						try {
							turnUpVolume(context);
							Uri notification = RingtoneManager
									.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
							Ringtone r = RingtoneManager.getRingtone(
									context.getApplicationContext(),
									notification);
							r.play();
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else if (msgType == 1) {
						// new TID
						Intent tidIntent = new Intent("tid_update");
						tidIntent.putExtra("tid", extras.getString("TID"));
						ITTApplication.getInstance().saveTID(
								extras.getString("TID"));
						context.sendBroadcast(tidIntent);
					} else if (msgType == 2) {
						// update check
						Intent versionIntent = new Intent("version_check");
						versionIntent.putExtra("version",
								extras.getString("CurrentVersion"));
						context.sendBroadcast(versionIntent);
					}
				} 
				/*else {
					sendNotification(context.getString(R.string.app_name),
							msgType, bundle, context);
				}*/
				Log.i(TAG, "Received: " + extras.toString());

				wl.release();
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}
	}

	/*private void sendNotification(String msg, int type, Bundle extras,
			Context context) {
		nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		Intent goToMainIntent = new Intent(context, MainActivity.class);
		goToMainIntent.putExtra("msgType", type);
		goToMainIntent.putExtra("bundle", extras);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				goToMainIntent, 0);

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				context).setSmallIcon(R.drawable.abc_ab_bottom_solid_dark_holo)
				.setContentTitle("GCM Notification")
				.setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
				.setContentText(msg);

		mBuilder.setContentIntent(contentIntent);
		nm.notify(NOTIFICATION_ID, mBuilder.build());
	}*/

	private boolean appIsRunning(Context context) {
		ActivityManager activityManager = (ActivityManager) context
				.getSystemService("activity");
		List<RunningAppProcessInfo> procInfos = activityManager
				.getRunningAppProcesses();
		for (int i = 0; i < procInfos.size(); i++) {
			if (procInfos.get(i).processName.equals("com.oneport.itt")) {
				return true;
			}
		}
		return false;
	}

	private void turnUpVolume(Context context) {
		AudioManager am = (AudioManager) context
				.getSystemService(Context.AUDIO_SERVICE);

		am.setStreamVolume(AudioManager.STREAM_NOTIFICATION,
				am.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION), 0);
	}

}