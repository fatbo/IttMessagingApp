package com.oneport.network;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request.Method;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.oneport.itt.ITTApplication;
import com.oneport.model.Msg;

public class NetworkAddress {
	
	static String domain = ITTApplication.ITT_DOMAIN;
	private static int HTTP_TIMEOUT = 10*1000; 

	public static void sendActivation(String uuID, String appVersion, final NetworkDelegate delegate)
	{
		String action = "activate";
		String url = domain + "Action="+action+"&UUID="+uuID+"&AppVersion="+appVersion+"&IsTerminalVersion=0";
		
		JsonObjectRequest myReq = new JsonObjectRequest(Method.GET,  
				url,  null,
				new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                    	if(response != null){
                    		try {
								if(response.getInt("Status") == 1){
									String deviceId = response.getString("DeviceID");
									delegate.didActivation(deviceId,null);
								}else{
									delegate.didActivation(null,new Error(response.getString("ErrorMessage")));
								}
							} catch (JSONException e) {
								e.printStackTrace();
							}
                    	}else{
                    		delegate.didActivation(null,new Error("response null"));
                    	}
                    }
                },  
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                    	delegate.failToConnect(error);
                    }
                });
		// timeout = 10s, retry = 0
		myReq.setRetryPolicy(new DefaultRetryPolicy(HTTP_TIMEOUT,0,0));
		ITTApplication.getInstance().getRequestQueue().add(myReq);  
	}

	public static void updateUUID(String uuID, String deviceID, String appVersion, final NetworkDelegate delegate)
	{
		String action = "updateID";
		String url = domain + "Action="+action+"&UUID="+uuID+"&DeviceID="+deviceID+"&AppVersion="+appVersion+"&IsTerminalVersion=0";
		
		JsonObjectRequest myReq = new JsonObjectRequest(Method.GET,  
				url,  null,
				new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                    	if(response != null){
                    		try {
								if(response.getInt("Status") == 1){
									delegate.didUpdateUUID(null);
								}else{
									delegate.didUpdateUUID(new Error(response.getString("ErrorMessage")));
								}
							} catch (JSONException e) {
								e.printStackTrace();
							}
                    	}else{
                    		delegate.didUpdateUUID(new Error("response null"));
                    	}
                    }
                },  
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                    	delegate.failToConnect(error);
                    }
                });
		// timeout = 10s, retry = 0
		myReq.setRetryPolicy(new DefaultRetryPolicy(HTTP_TIMEOUT,0,0));
		ITTApplication.getInstance().getRequestQueue().add(myReq);  
	}	

	public static void retrieveMessageReload(String deviceID, final NetworkDelegate delegate)
	{
		String action = "getMessage";
		String url = domain + "Action="+action+"&DeviceID="+deviceID;
		
		JsonObjectRequest myReq = new JsonObjectRequest(Method.GET,  
				url,  null,
				new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                    	if(response != null){
                    		try {
								if(response.getInt("Status") == 1){
									String tid = response.getString("TID");
									ArrayList<Msg> msgArr = null;
									JSONArray msgJsonArr = response.getJSONArray("Messages");
									msgArr = new ArrayList<Msg>();
									for(int i = 0 ; i < msgJsonArr.length() ; i++){
										JSONObject msgJson = msgJsonArr.getJSONObject(i);
										Msg msg = new Msg();
										msg.content = msgJson.getString("Content");
										msg.time = msgJson.getString("MessageDateTime");
										msg.msgId = msgJson.getString("MessageID");
										msg.msgDateTime = msgJson.getString("MessageTimestamp");
										msgArr.add(msg);
									}
									delegate.didRetrieveMsgReload(tid,msgArr,null);
								}else{
									delegate.didRetrieveMsgReload(null,null,new Error("status = not success"));
								}
							} catch (JSONException e) {
								e.printStackTrace();
							}
                    	}else{
                    		delegate.didRetrieveMsgReload(null,null,new Error("response null"));
                    	}
                    }
                },  
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                    	delegate.failToConnect(error);
                    }
                });
		// timeout = 10s, retry = 0
		myReq.setRetryPolicy(new DefaultRetryPolicy(HTTP_TIMEOUT,0,0));
		ITTApplication.getInstance().getRequestQueue().add(myReq);  
	}
	
	public static void retrieveMessage(String deviceID, String MessageID, final NetworkDelegate delegate)
	{
		String action = "getMessage";
		String url = domain + "Action="+action+"&DeviceID="+deviceID+"&MessageID="+MessageID;
		
		JsonObjectRequest myReq = new JsonObjectRequest(Method.GET,  
				url,  null,
				new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                    	if(response != null){
                    		try {
								if(response.getInt("Status") == 1){
									String tid = response.getString("TID");
									ArrayList<Msg> msgArr = null;
									JSONArray msgJsonArr = response.getJSONArray("Messages");
									msgArr = new ArrayList<Msg>();
									for(int i = 0 ; i < msgJsonArr.length() ; i++){
										JSONObject msgJson = msgJsonArr.getJSONObject(i);
										Msg msg = new Msg();
										msg.content = msgJson.getString("Content");
										msg.time = msgJson.getString("MessageDateTime");
										msg.msgId = msgJson.getString("MessageID");
										msg.msgDateTime = msgJson.getString("MessageTimestamp");
										msgArr.add(msg);
									}
									delegate.didRetrieveMsg(tid,msgArr,null);
								}else{
									delegate.didRetrieveMsg(null,null,new Error("status = not success"));
								}
							} catch (JSONException e) {
								e.printStackTrace();
							}
                    	}else{
                    		delegate.didRetrieveMsg(null,null,new Error("response null"));
                    	}
                    }
                },  
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                    	delegate.failToConnect(error);
                    }
                });
		// timeout = 10s, retry = 0
		myReq.setRetryPolicy(new DefaultRetryPolicy(HTTP_TIMEOUT,0,0));
		ITTApplication.getInstance().getRequestQueue().add(myReq);
	}
	
	public static void checkVersion(final NetworkDelegate delegate)
	{
		String action = "enquireVersion";
		String url = domain + "Action="+action+"&IsTerminalVersion=0";
		
		JsonObjectRequest myReq = new JsonObjectRequest(Method.GET,  
				url,  null,
				new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                    	if(response != null){
                    		try {
								if(response.getInt("Status") == 1){
									String currentVersion = response.getString("CurrentVersion");
									delegate.didCheckVersion(currentVersion,null);
								}else{
									delegate.didCheckVersion(null,new Error(response.getString("ErrorMessage")));
								}
							} catch (JSONException e) {
								e.printStackTrace();
							}
                    	}else{
                    		delegate.didCheckVersion(null,new Error("response null"));
                    	}
                    }
                },  
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                    	delegate.failToConnect(error);
                    }
                });
		// timeout = 10s, retry = 0
		myReq.setRetryPolicy(new DefaultRetryPolicy(HTTP_TIMEOUT,0,0));
		ITTApplication.getInstance().getRequestQueue().add(myReq);  
		
		
	}

	public static void login(String tid, String password, String licenseNumber, String tractorProvider, String deviceId, final NetworkDelegate delegate)
	{
		String action = "login";
		String url = domain + "Action="+action+"&Tid="+tid+"&Password="+password+"&LicenseNumber="+licenseNumber+"&TractorProvider="+tractorProvider+"&DeviceID="+deviceId;
		
		JsonObjectRequest myReq = new JsonObjectRequest(Method.GET,  
				url,  null,
				new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                    	if(response != null){
                    		try {
								if(response.getInt("Status") == 1){
									int returnCode = response.getInt("ReturnCode");
									String remark = response.getString("Remark");
									String timestampStr = response.getString("Timestamp");
									delegate.didLogin(returnCode, remark, timestampStr, null);
								}else{
									delegate.didLogin(99, null, null, new Error(response.getString("ErrorMessage")));
								}
							} catch (JSONException e) {
								e.printStackTrace();
								delegate.didLogin(99, null, null, new Error(e.getMessage()));
							}
                    	}else{
                    		delegate.didLogin(99,null,null,new Error("response null"));
                    	}
                    }
                },  
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                    	delegate.failToConnect(error);
                    }
                });
		// timeout = 10s, retry = 0
		myReq.setRetryPolicy(new DefaultRetryPolicy(HTTP_TIMEOUT,0,0));
		ITTApplication.getInstance().getRequestQueue().add(myReq);
	}
}
