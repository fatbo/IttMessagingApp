package com.oneport.itt;

import java.util.ArrayList;

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
	ProgressBar pbHeaderProgress;
	TextView tvAppVersion;
	ImageView imgMqttVerified;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
Log.d(this.getClass().getSimpleName(),"onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_activate);

		btnActivate = (Button) findViewById(R.id.btnActivate);
		btnActivate.setOnClickListener(this);
		btnActivate.setEnabled(false);

		receiverRegister();
		
		ITTApplication.getInstance().load();
		if (ITTApplication.getInstance().getInfo().logined) {
			// if inactive time > session timeout period, force user to login again
			long lastInteraction = ITTApplication.getInstance().getLastInteraction();
Log.d(this.getClass().getSimpleName(),"lastInteraction: "+lastInteraction);
			long currentTime = System.currentTimeMillis();
			if ((currentTime-lastInteraction)>ITTApplication.DEFAULT_TIMEOUT_PERIOD) {
				Intent intent = new Intent(ActivateActivity.this,LoginActivity.class);
				intent.putExtra("IsSessionTimeout", true);
				startActivity(intent);
				finish();
			} else {
				Intent intent = new Intent(ActivateActivity.this,MainActivity.class);
				startActivity(intent);
				finish();
			}
		} else if (ITTApplication.getInstance().getInfo().activated) {
			Intent intent = new Intent(ActivateActivity.this,LoginActivity.class);
			startActivity(intent);
			finish();
		} else {
			tvNotSupport = (TextView) findViewById(R.id.tv_not_support);
			pbHeaderProgress = (ProgressBar) findViewById(R.id.pbHeaderProgress);
			tvAppVersion = (TextView) findViewById(R.id.activity_activate_tv_app_version);
			tvAppVersion.setText("V"+getString(R.string.version));
			
			imgMqttVerified = (ImageView) findViewById(R.id.imgMqttVerified);
						
Log.d("onCreate","enableActivation");
			enableActivation();
			startPushService();
		}
			

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		return true;
	}

	@Override
	protected void onResume() {
Log.d(this.getClass().getSimpleName(),"onResume");
		super.onResume();
		active = true;
		JPushInterface.onResume(this);
		btnActivate.setVisibility(View.VISIBLE);
		tvNotSupport.setVisibility(View.GONE);
	}

	@Override
	protected void onPause() {
//Log.d(this.getClass().getSimpleName(),"onPause");		
		super.onPause();
		active = false;
		JPushInterface.onPause(this);
	}
	
	
	@Override
	protected void onStart() {
Log.d(this.getClass().getSimpleName(),"onStart");
		super.onStart();
		//bindService(new Intent(this, MQTTservice.class), serviceConnection, 0);
//Log.d(this.getClass().getSimpleName(),"onStart done");
	}

	@Override
	protected void onStop() {
//Log.d(this.getClass().getSimpleName(),"onStop");
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		receiverUnregister();
		super.onDestroy();
		
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
	}
	
	@Override
	public void onClick(View v) {
		if (!this.isNetworkOnline()) {
			showMsg(R.string.no_connection);
			return;
		}
		
		startLoading();
		NetworkAddress.sendActivation(
				ITTApplication.getInstance().regid,
				getResources().getString(R.string.version),
				new NetworkDelegate() {

					@Override
					public void didActivation(String deviceID,
							Error error) {
						if (error == null) {
							//ITTApplication.getInstance().info = new Info();
							Info info = ITTApplication.getInstance().getInfo();
							info.activated = true;
							info.deviceId = deviceID;
							info.registrationId = ITTApplication.getInstance().regid;
							info.truckId = "";
							info.logined = false;
							info.lastLoginTime= null;
							
							ITTApplication.getInstance().saveLogin();
							stopPushService();
														
							Intent intent = new Intent(ActivateActivity.this,LoginActivity.class);
							startActivity(intent);
							finish();
						} else {
							showMsg(error.getMessage());
							//btnActivate.setVisibility(View.GONE);
							//tvNotSupport.setVisibility(View.VISIBLE);
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

					@Override
					public void didLogin(int returnCode, String remark, String timestampStr, Error error) {
						// TODO Auto-generated method stub
						
					}
				});
	}	
	
	/*
	private BroadcastReceiver jpushRegSuccessReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
Log.d("jpushRegSuccessReceiver",">> RegID: "+ITTApplication.getInstance().regid);

			Info info = ITTApplication.getInstance().getInfo();			
			info.registrationId = ITTApplication.getInstance().regid;
			ITTApplication.getInstance().saveLogin();
			
			enableActivation();
		}
	};
	
	*/

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
		//IntentFilter mFilter01 = new IntentFilter("jpush_reg_success");
		//registerReceiver(jpushRegSuccessReceiver, mFilter01);
		IntentFilter mqttWelcomeMsgIntentFilter = new IntentFilter("mqtt_welcome_msg_received");
		registerReceiver(mqttWelcomeMsgReceiver, mqttWelcomeMsgIntentFilter);
	}

	private void receiverUnregister() {
		Log.d(this.getClass().getSimpleName(),"receiverUnregister");
		unregisterReceiver(mqttWelcomeMsgReceiver);
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
}
