package com.oneport.itt;

import java.util.ArrayList;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import cn.jpush.android.api.JPushInterface;

import com.android.volley.VolleyError;
import com.oneport.model.Info;
import com.oneport.model.Msg;
import com.oneport.mqtt.MQTTConstant;
import com.oneport.mqtt.MQTTservice;
import com.oneport.network.NetworkAddress;
import com.oneport.network.NetworkDelegate;

public class ActivateActivity extends BaseActivity implements OnClickListener {

	Handler handler = new Handler();
	int tryCount = 0;
	Button btnActivate;
	TextView tvNotSupport;
	TextView tvAppVersion;

	ImageView imgMqttVerified;
	ProgressBar pbHeaderProgress;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_activate);

		btnActivate = (Button) findViewById(R.id.btnActivate);
		btnActivate.setOnClickListener(this);

		receiverRegister();
		
		if (ITTApplication.getInstance().load()) {
			Intent intent = new Intent(ActivateActivity.this,
					MainActivity.class);
			startActivity(intent);
			finish();
		} else {
			tvNotSupport = (TextView) findViewById(R.id.tv_not_support);
			tvAppVersion = (TextView) findViewById(R.id.tv_app_version);
			tvAppVersion.setText("V"+getString(R.string.version));
	
			pbHeaderProgress = (ProgressBar) findViewById(R.id.pbHeaderProgress);
			imgMqttVerified = (ImageView) findViewById(R.id.imgMqttVerified);
			
			enableActivation();
			startPushService();
		}
	}
	

	private void enableActivation() {
		// enable activate button, hide progress bar
		btnActivate.setAlpha(1.0f);
		btnActivate.setEnabled(true);		

		if (ITTApplication.getInstance().isMqttVerified()) {
			imgMqttVerified.setImageResource(R.drawable.green_tick);
			imgMqttVerified.setVisibility(View.VISIBLE);
			pbHeaderProgress.setVisibility(View.GONE);
		} else {
			imgMqttVerified.setVisibility(View.GONE);
			pbHeaderProgress.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		return true;
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		active = true;
		JPushInterface.onResume(this);
		btnActivate.setVisibility(View.VISIBLE);
		tvNotSupport.setVisibility(View.GONE);
	}
	
	

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		active = false;
		JPushInterface.onPause(this);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
	}

	private boolean deviceHasGoogleAccount() {
		AccountManager accMan = AccountManager.get(this);
		Account[] accArray = accMan.getAccountsByType("com.google");
		return accArray.length >= 1 ? true : false;
	}

	@Override
	public void onClick(View v) {
		// if (!deviceHasGoogleAccount()) {
		// btnActivate.setVisibility(View.GONE);
		// tvNotSupport.setVisibility(View.VISIBLE);
		// return;
		// }
		
		if (!this.isNetworkOnline()) {
			showMsg(R.string.no_connection);
			return;
		}
		
		startLoading();
		ITTApplication.getInstance().regid = JPushInterface.getRegistrationID(ActivateActivity.this);
		final Runnable runnable = new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				if (ITTApplication.getInstance().regid == null || ITTApplication.getInstance().regid.isEmpty()) {
					if(++tryCount > 10){
						stopLoading();
						tryCount = 0;
						ITTApplication.getInstance().regid = JPushInterface.getRegistrationID(ActivateActivity.this);
						return;
					}
					handler.postDelayed(this, 1000);
				} else {
					NetworkAddress.sendActivation(
							ITTApplication.getInstance().regid,
							getResources().getString(R.string.version),
							new NetworkDelegate() {

								@Override
								public void didActivation(String deviceID,
										Error error) {
									// TODO Auto-generated method stub
									if (error == null) {
										ITTApplication.getInstance().info = new Info();
										ITTApplication.getInstance().info.deviceId = deviceID;
										ITTApplication.getInstance().info.registrationId = ITTApplication.getInstance().regid;
										ITTApplication.getInstance().info.truckId = "";
										ITTApplication.getInstance().saveLogin();

										stopPushService();
										
										Intent intent = new Intent(ActivateActivity.this,MainActivity.class);
										startActivity(intent);
										finish();
									} else {
										showMsg(error.getMessage());
										btnActivate.setVisibility(View.GONE);
										tvNotSupport
												.setVisibility(View.VISIBLE);
									}
									stopLoading();
								}

								@Override
								public void didUpdateUUID(Error error) {
									// TODO Auto-generated method stub

								}

								@Override
								public void didRetrieveMsg(String tid,
										ArrayList<Msg> msgList, Error error) {
									// TODO Auto-generated method stub

								}

								@Override
								public void didRetrieveMsgReload(String tid,
										ArrayList<Msg> msgList, Error error) {
									// TODO Auto-generated method stub

								}

								@Override
								public void didCheckVersion(
										String currentVersion, Error error) {
									// TODO Auto-generated method stub

								}

								@Override
								public void failToConnect(VolleyError error) {
									// TODO Auto-generated method stub
									showMsg(R.string.no_connection);
									stopLoading();

								}
							});
				}
			}
		};
		handler.postDelayed(runnable, 1000);
	}

	private void startPushService() {
		// start MQTTService
		Intent mqttServiceIntent = new Intent(this, MQTTservice.class);
		mqttServiceIntent.putExtra(MQTTservice.MQTT_CLIENT_ID,MQTTConstant.annonymousClientId);
        startService(mqttServiceIntent);
	}

	private void stopPushService() {
		try {
			Log.d(this.getClass().getSimpleName(),"stopPushService: "+stopService(new Intent(this, MQTTservice.class)));
		} catch (Exception e) {
			Log.e(this.getClass().getCanonicalName(),"Exception occurred in stopPushService()",e);
		}
	}
	

	private BroadcastReceiver mqttWelcomeMsgReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
Log.d("mqttWelcomeMsgReceiver",">> welcome msg received, display verified icon...");
			// display verified icon
			pbHeaderProgress.setVisibility(View.GONE);
			imgMqttVerified.setImageResource(R.drawable.green_tick);
			imgMqttVerified.setVisibility(View.VISIBLE);
			
			// set verified
			ITTApplication.getInstance().saveMqttVerifiedStatus(true);
		}
	};

	private void receiverRegister() {
		IntentFilter mqttWelcomeMsgIntentFilter = new IntentFilter("mqtt_welcome_msg_received");
		registerReceiver(mqttWelcomeMsgReceiver, mqttWelcomeMsgIntentFilter);
	}

	private void receiverUnregister() {
		Log.d(this.getClass().getSimpleName(),"receiverUnregister");
		unregisterReceiver(mqttWelcomeMsgReceiver);
	}

	@Override
	protected void onDestroy() {
		receiverUnregister();
		super.onDestroy();
		
	}
}
