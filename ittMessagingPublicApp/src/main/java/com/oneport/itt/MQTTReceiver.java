package com.oneport.itt;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.oneport.mqtt.MQTTservice;

public class MQTTReceiver extends BroadcastReceiver {
	private static final String TAG = "MQTTReceiver";

	public static final int NOTIFICATION_ID = 1;
	private NotificationManager nm;
	NotificationCompat.Builder builder;

	@Override
	public void onReceive(Context context, Intent intent) {
		if (null == nm) {
			nm = (NotificationManager) context
					.getSystemService(Context.NOTIFICATION_SERVICE);
		}

Log.d(TAG, "intent action: "+intent.getAction());
		Bundle bundle = intent.getExtras();
		if ("com.oneport.mqtt.MESSAGE_RECEIVED".equals(intent.getAction())) {
			String msg = bundle.getString(MQTTservice.MESSAGE);
Log.d(TAG, "message: "+msg);
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
				Log.d(TAG, "msgtype: "+msgType);
				if (appIsRunning(context)) {
					if (msgType == 0) {
						// new msg
						Intent msgIntent = new Intent("new_msg");
						msgIntent.putExtra("msgID",
								extras.getString("MessageID"));
						context.sendBroadcast(msgIntent);
						try {
							//turnUpVolume(context);
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
					} else if (msgType == 3) {
						// login session invalidated, force logout
						Intent forceLogoutIntent = new Intent("force_logout");
						forceLogoutIntent.putExtra("ForceLogoutBy",extras.getString("ForceLogoutBy"));
						forceLogoutIntent.putExtra("Timestamp",extras.getString("Timestamp"));
						context.sendBroadcast(forceLogoutIntent);
					}  else if (msgType == 4) {
						// login session invalidated, force logout
						Intent mqttWelcomeMsgReceivedIntent = new Intent("mqtt_welcome_msg_received");
						context.sendBroadcast(mqttWelcomeMsgReceivedIntent);
					}
				}
				
				Log.i(TAG, "Received: " + extras.toString());

				wl.release();
			} catch (JSONException e1) {
				e1.printStackTrace();
			}

		}
	}

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

	/*private void turnUpVolume(Context context) {
		AudioManager am = (AudioManager) context
				.getSystemService(Context.AUDIO_SERVICE);

		am.setStreamVolume(AudioManager.STREAM_NOTIFICATION,
				am.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION), 0);
	}*/

}