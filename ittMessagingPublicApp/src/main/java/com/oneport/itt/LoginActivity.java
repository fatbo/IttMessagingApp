package com.oneport.itt;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import cn.jpush.android.api.JPushInterface;

import com.android.volley.VolleyError;
import com.oneport.model.Info;
import com.oneport.model.Msg;
import com.oneport.network.NetworkAddress;
import com.oneport.network.NetworkDelegate;

public class LoginActivity extends BaseActivity implements OnClickListener {

	//private Handler handler = new Handler();
	private Button btnLogin;
	private EditText inputTid;
	private EditText inputPassword;
	private EditText inputLicenseNumber;
	private Spinner inputTractorProvider;
	
	private TextView tvAppVersion;
	private TextView tvDeviceId;
	private TextView tvAppName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_login);		
		btnLogin = (Button)findViewById(R.id.btnLogin);
		btnLogin.setOnClickListener(this);
		
		inputTid = (EditText)findViewById(R.id.inputTid);
		inputTid.setFilters(new InputFilter[] {new InputFilter.AllCaps()});
		inputPassword = (EditText)findViewById(R.id.inputPassword);
		inputLicenseNumber = (EditText)findViewById(R.id.inputLicenseNumber);
		inputLicenseNumber.setFilters(new InputFilter[] {new InputFilter.AllCaps()});
		inputTractorProvider = (Spinner)findViewById(R.id.inputTractorProvider);
		
		Info info = ITTApplication.getInstance().getInfo();
		
		inputTid.setText(info.tid);
		inputPassword.setText(info.password);
		//inputLicenseNumber.setText(info.licenseNumber);
		inputTractorProvider.setSelection(getTractorProviderSelectionPosition(info.tractorProvider));
		
		tvAppVersion = (TextView)findViewById(R.id.tv_app_version);
		tvAppVersion.setText("V"+getString(R.string.version));
		tvDeviceId = (TextView)findViewById(R.id.tv_device_id);
		tvDeviceId.setText(info.deviceId);
		
		tvAppName = (TextView)findViewById(R.id.tv_app_name);
	}
	
	private int getTractorProviderSelectionPosition(String tractorProvider) {
		if ("HIT".equalsIgnoreCase(tractorProvider)) {
			return 0;
		} else if ("MTL".equalsIgnoreCase(tractorProvider)) {
			return 1;
		} else if ("CHT".equalsIgnoreCase(tractorProvider)) {
			return 2;
		} else if ("DPW".equalsIgnoreCase(tractorProvider)) {
			return 3;
		} else if ("ACT".equalsIgnoreCase(tractorProvider)) {
			return 4;
		} else
			return 0;
	}

	@Override
	protected void onPause() {
		super.onPause();
		active = false;
	}

	@Override
	protected void onResume() {
		super.onResume();
		ITTApplication.getInstance().changeLanguage(ITTApplication.getInstance().currentLanguage);
		refreshUI();
		active = true;
		
		if (getIntent()!=null && getIntent().getExtras()!=null) {
			boolean isForceLogout = getIntent().getExtras().getBoolean("IsForceLogout", false);
			boolean isSessionTimeout = getIntent().getExtras().getBoolean("IsSessionTimeout", false);
Log.d(this.getClass().getCanonicalName(),"isForceLogout? "+isForceLogout);
Log.d(this.getClass().getCanonicalName(),"isSessionTimeout? "+isSessionTimeout);

			if (isForceLogout) {
				String forceLogoutBy = getIntent().getExtras().getString("ForceLogoutBy");
Log.d(this.getClass().getCanonicalName(),"forceLogoutBy? "+forceLogoutBy);
				showMsg(getString(R.string.login_force_logout, forceLogoutBy));
				getIntent().putExtra("IsForceLogout", false);
			} else if (isSessionTimeout) {
				showMsg(getString(R.string.login_timeout));
				getIntent().putExtra("IsSessionTimeout", false);
			}
		}
	}

	public void refreshUI() {
		btnLogin.setText(this.getResources().getString(R.string.btn_login));
		tvAppName.setText(this.getResources().getString(R.string.app_name));
	}

	@Override
	public void onClick(View view) {
		if (view == btnLogin) {
			if (!this.isNetworkOnline()) {
				showToast(R.string.no_connection);
				return;
			}
			// invoke ITT Platform API to perform login process
			final String tid = inputTid.getText().toString();
			final String password = inputPassword.getText().toString();
			final String licenseNumber = inputLicenseNumber.getText().toString();
			final String tractorProvider = inputTractorProvider.getSelectedItem().toString();
			
			// validate input
			if (tid==null || tid.trim().isEmpty()) {
				inputTid.setError("Please input TID");
				return;
			} else {
				inputTid.setError(null);
			}
			if (password==null || password.trim().isEmpty()) {
				inputPassword.setError("Please input password");
				return;
			} else {
				inputPassword.setError(null);
			}
			if (licenseNumber==null || licenseNumber.trim().isEmpty()) {
				inputLicenseNumber.setError("Please input license number");
				return;
			} else {
				inputLicenseNumber.setError(null);
			}
			
			startLoading();
			NetworkAddress.login(tid, password, licenseNumber, tractorProvider, ITTApplication.getInstance().getInfo().deviceId, new NetworkDelegate() {
				
				@Override
				public void failToConnect(VolleyError error) {
					stopLoading();
					showToast(R.string.login_error_device_other);
				}
				
				@Override
				public void didUpdateUUID(Error error) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void didRetrieveMsgReload(String tid, ArrayList<Msg> msgList,
						Error error) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void didRetrieveMsg(String tid, ArrayList<Msg> msgList, Error error) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void didLogin(int returnCode, String remark, String timestampStr, Error error) {
					stopLoading();
Log.d("didLogin","returnCode: "+returnCode);	
					if (returnCode==0) {
						// save login profile
						Info info = ITTApplication.getInstance().getInfo();
						info.tid = tid;
						info.password = password;
						info.licenseNumber = licenseNumber;
						info.tractorProvider = tractorProvider;
						info.logined = true;
						info.lastLoginTime = timestampStr;
						ITTApplication.getInstance().saveLogin();						
						
						Intent intent = new Intent(LoginActivity.this,MainActivity.class);
						startActivity(intent);
						finish();					
					} else {
						String msg = null;
						switch (returnCode) {
							case 1:
								msg = getString(R.string.login_error_invalid_password);
								break;
							case 2:
								msg = getString(R.string.login_error_invalid_license_number);
								break;
							case 3:
								msg = getString(R.string.login_error_truck_not_deployed,licenseNumber);
								break;
							case 4:
								msg = getString(R.string.login_error_device_not_authorized);
								break;
							case 5:
								msg = getString(R.string.login_error_invalid_tid);
								break;
							case 6:
								msg = getString(R.string.login_error_conflict_device_id);
								break;
							default:
								msg = getString(R.string.login_error_device_other);
						}
						Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
					}
				}
				
				@Override
				public void didCheckVersion(String currentVersion, Error error) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void didActivation(String deviceID, Error error) {
					// TODO Auto-generated method stub
					
				}
			});
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		JPushInterface.stopPush(this);
	}
	
	
}
