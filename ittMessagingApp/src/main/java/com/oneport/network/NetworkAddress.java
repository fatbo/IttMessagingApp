package com.oneport.network;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request.Method;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.oneport.itt.ITTApplication;
import com.oneport.model.Msg;

public class NetworkAddress {
	
	static String domain = ITTApplication.ITT_DOMAIN;
	private static int TIMEOUT_MS = 20*1000; 
	

	public static void sendActivation(String uuID, String appVersion, final NetworkDelegate delegate)
	{
		String action = "activate";
		String url = domain + "Action="+action+"&UUID="+uuID+"&AppVersion="+appVersion;
		
/*		
		{
			"Status":1,
			"DeviceID":"0000001",
			"ErrorMessage":""
		}
*/		
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
								// TODO Auto-generated catch block
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
		
		// no retry
		myReq.setRetryPolicy(new DefaultRetryPolicy(TIMEOUT_MS,0,DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
		ITTApplication.getInstance().getRequestQueue().add(myReq);  
	}

	public static void updateUUID(String uuID, String deviceID, String appVersion, final NetworkDelegate delegate)
	{
		String action = "updateID";
		String url = domain + "Action="+action+"&UUID="+uuID+"&DeviceID="+deviceID+"&AppVersion="+appVersion;
/*		
		{
			"Status":1,
			"ErrorMessage":""
		}		
*/
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
								// TODO Auto-generated catch block
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

		// no retry
		myReq.setRetryPolicy(new DefaultRetryPolicy(TIMEOUT_MS,0,DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
		ITTApplication.getInstance().getRequestQueue().add(myReq);  
	}	

	public static void retrieveMessageReload(String deviceID, final NetworkDelegate delegate)
	{
		String action = "getMessage";
		String url = domain + "Action="+action+"&DeviceID="+deviceID;
//		url = "http://server.4d.com.hk:8080/AppDist/kaden/textapi.php";
/*
{
	"Status":1,
	"TID":"AB1234",
	"Messages": 
	[
		{
			"MessageID":"1",
			"Content":"PI@ACT",
			"MessageDateTime":"2015-03-02 14:55"
		},
		{
			"MessageID":"2",
			"Content":"GD@CHT",
			"MessageDateTime":"2015-03-02 15:30"
		}		
	]
}
*/	
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
										
										msg.msgType = msgJson.has("MessageType")?msgJson.getString("MessageType"):null;
										msg.contentJson = msgJson.has("ContentJSON")?msgJson.getString("ContentJSON"):null;
										msgArr.add(msg);
									}
									delegate.didRetrieveMsgReload(tid,msgArr,null);
								}else{
									delegate.didRetrieveMsgReload(null,null,new Error("status = not success"));
								}
							} catch (JSONException e) {
								// TODO Auto-generated catch block
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

		// no retry
		myReq.setRetryPolicy(new DefaultRetryPolicy(TIMEOUT_MS,0,DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
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
										msg.msgType = msgJson.has("MessageType")?msgJson.getString("MessageType"):null;
										msg.contentJson = msgJson.has("ContentJSON")?msgJson.getString("ContentJSON"):null;
										msgArr.add(msg);
									}
									delegate.didRetrieveMsg(tid,msgArr,null);
								}else{
									delegate.didRetrieveMsg(null,null,new Error("status = not success"));
								}
							} catch (JSONException e) {
								// TODO Auto-generated catch block
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

		// no retry
		myReq.setRetryPolicy(new DefaultRetryPolicy(TIMEOUT_MS,0,DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
		//myReq.setRetryPolicy(new DefaultRetryPolicy(2000,2,1));
		ITTApplication.getInstance().getRequestQueue().add(myReq);
	}
	
	public static void checkVersion(final NetworkDelegate delegate)
	{
		String action = "enquireVersion";
		String url = domain + "Action="+action;
/*		
{
	"Status":1,
	"CurrentVersion":"1.0",
	"ErrorMessage":""
}		
*/		
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
								// TODO Auto-generated catch block
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

		// no retry
		myReq.setRetryPolicy(new DefaultRetryPolicy(TIMEOUT_MS,0,DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
		ITTApplication.getInstance().getRequestQueue().add(myReq);  
		
		
	}

	public static void sendMessageAck(String deviceID, String MessageID, final NetworkDelegate delegate)
	{
		String action = "messageAck";
		String url = domain + "Action="+action+"&DeviceID="+deviceID+"&MessageID="+MessageID;
		
		JsonObjectRequest myReq = new JsonObjectRequest(Method.GET,  
				url,  null,
				new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                    	if(response != null){
                    		try {
								if(response.getInt("Status") == 1){
									
								}else{
									//delegate.didRetrieveMsg(null,null,new Error("status = not success"));
								}
							} catch (JSONException e) {
								e.printStackTrace();
							}
                    	}else{
                    		//delegate.didRetrieveMsg(null,null,new Error("response null"));
                    	}
                    }
                },  
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    	delegate.failToConnect(error);
                    }
                });

		// no retry
		myReq.setRetryPolicy(new DefaultRetryPolicy(TIMEOUT_MS,0,DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
		ITTApplication.getInstance().getRequestQueue().add(myReq);
	}
}
