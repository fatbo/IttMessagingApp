package com.oneport.itt;



import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.oneport.mqtt.MQTTservice;

// start MQTT service at device start up
public class BootReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		Log.d(getClass().getCanonicalName(), "onReceive "+ITTApplication.getInstance().getDeviceID());
		String deviceId = ITTApplication.getInstance().getDeviceID();
		Log.d(getClass().getCanonicalName(), "deviceId "+deviceId);
		if (deviceId!=null && !deviceId.isEmpty()) {
			Intent mqttServiceIntent = new Intent(context, MQTTservice.class);
			mqttServiceIntent.putExtra(MQTTservice.MQTT_CLIENT_ID,deviceId);
			context.startService(mqttServiceIntent);
		}
	}
}