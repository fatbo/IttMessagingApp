package com.oneport.itt;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
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

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class GcmIntentService extends IntentService {
	public static final int NOTIFICATION_ID = 1;
	private NotificationManager mNotificationManager;
	NotificationCompat.Builder builder;
	String TAG = "demo";

	public GcmIntentService() {
		super("GcmIntentService");
	}
	
	private void turnUpVolume(){
		AudioManager am = 
			    (AudioManager) getSystemService(Context.AUDIO_SERVICE);

			am.setStreamVolume(
			    AudioManager.STREAM_NOTIFICATION,
			    am.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION),
			    0);
	}
	
	

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
		// The getMessageType() intent parameter must be the intent you received
		// in your BroadcastReceiver.
		String messageType = gcm.getMessageType(intent);

		if (!extras.isEmpty()) {
			if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR
					.equals(messageType)) {
				sendNotification("Send error: " + extras.toString(), -1,extras);
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED
					.equals(messageType)) {
				sendNotification(
						"Deleted messages on server: " + extras.toString(), -1,extras);
				// If it's a regular GCM message, do some work.
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE
					.equals(messageType)) {
				
				
				PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
				WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG");
				wl.acquire();
				
				int msgType = Integer.parseInt(extras.getString("msgtype"));
				if (appIsRunning()) {
					if (msgType == 0) {
						// new msg
						Intent msgIntent = new Intent("new_msg");
						msgIntent.putExtra("msgID", extras.getString("messageid"));
						sendBroadcast(msgIntent);
						try {
							turnUpVolume();
						    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
						    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
						    r.play();
						} catch (Exception e) {
						    e.printStackTrace();
						}
					}
					else if (msgType == 1) {
						// new TID
						Intent tidIntent = new Intent("tid_update");
						tidIntent.putExtra("tid", extras.getString("message"));
						ITTApplication.getInstance().saveTID(extras.getString("message"));
						sendBroadcast(tidIntent);
					}
					else if (msgType == 2) {
						// update check
						Intent versionIntent = new Intent("version_check");
						versionIntent.putExtra("version", extras.getString("message"));
						sendBroadcast(versionIntent);
					}
				} else {
					sendNotification(getString(R.string.app_name), msgType,extras);
				}
				Log.i(TAG, "Received: " + extras.toString());
				
				wl.release();
			}
		}
		// Release the wake lock provided by the WakefulBroadcastReceiver.
		GcmBroadcastReceiver.completeWakefulIntent(intent);
	}

	// Put the message into a notification and post it.
	// This is just one simple example of what you might choose to do with
	// a GCM message.
	private void sendNotification(String msg, int type,Bundle extras) {
		/*mNotificationManager = (NotificationManager) this
				.getSystemService(Context.NOTIFICATION_SERVICE);
		Intent goToMainIntent = new Intent(this, MainActivity.class);
		goToMainIntent.putExtra("msgType", type);
		goToMainIntent.putExtra("bundle", extras);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				goToMainIntent, 0);

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this).setSmallIcon(R.drawable.abc_ab_bottom_solid_dark_holo)
				.setContentTitle("GCM Notification")
				.setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
				.setContentText(msg);

		mBuilder.setContentIntent(contentIntent);
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());*/
	}

	private boolean appIsRunning() {
		ActivityManager activityManager = (ActivityManager) this
				.getSystemService(ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> procInfos = activityManager
				.getRunningAppProcesses();
		for (int i = 0; i < procInfos.size(); i++) {
			if (procInfos.get(i).processName.equals("com.oneport.itt")) {
				return true;
			}
		}
		return false;
	}
}