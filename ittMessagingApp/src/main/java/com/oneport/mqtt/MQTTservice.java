package com.oneport.mqtt;



import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.oneport.itt.R;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;


public class MQTTservice extends Service
{
	private static boolean serviceRunning = false;
	//private static int mid = 0;
	private static MQTTConnection connection = null;
	private final Messenger clientMessenger = new Messenger(new ClientHandler());
	private AlarmManager mAlarmManager;
	private static final String DEFAULT_DATE_TIME_FORMAT = "yyyy/MM/dd HH:mm:ss:SSS";
	private static SimpleDateFormat defaultSdf = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT);
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
	}
		
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{		
		if (connection==null) {
			if (intent!=null) {
				Bundle bundle = intent.getExtras();
				if (bundle!=null) {
					String clientId = bundle.getString(MQTT_CLIENT_ID);				
					if (clientId!=null)
						connection = new MQTTConnection(clientId);
				}
			}
		}
		
Log.d(getClass().getCanonicalName(),"intent: "+intent);
		String action = intent!=null?intent.getAction():null;
Log.d(getClass().getCanonicalName(), "onStartCommand, action: "+action);
		if (ACTION_KEEPALIVE.equals(action)) {
			sendKeepAlive();
		}
		
		if (isRunning())
		{
			return START_STICKY;
		}
		
		super.onStartCommand(intent, flags, startId);
		if (connection!=null) {
			/*
			 * Start the MQTT Thread.
			 */
		    connection.start();
		}
	    
		return START_STICKY;
	}
	
	@Override
	public void onDestroy()
	{
		connection.end();
		connection=null;
	}
	
	@Override
	public IBinder onBind(Intent intent)
	{
		/*
		 * Return a reference to our client handler.
		 */
		return clientMessenger.getBinder();
	}
	
	 private synchronized static boolean isRunning()
	 {
		 /*
		  * Only run one instance of the service.
		  */
		 if (serviceRunning == false)
		 {
			 serviceRunning = true;
			 return false;
		 }
		 else
		 {
			 return true;
		 }
	 }
	 
	 public static final String MQTT_CLIENT_ID = "mqtt_client_id";
	 
	 /*
	  * These are the supported messages from bound clients
	  */
	 public static final int REGISTER = 0;
	 public static final int SUBSCRIBE = 1;
	 public static final int PUBLISH = 2;
	 public static final int STOP = PUBLISH + 1;
	 public static final int CONNECT = PUBLISH + 2;
	 public static final int RESETTIMER = PUBLISH + 3;
	 
	 /*
	  * Fixed strings for the supported messages.
	  */
	 public static final String TOPIC = "topic";
	 public static final String MESSAGE = "message";
	 public static final String STATUS = "status";
	 public static final String CLASSNAME = "classname";
	 public static final String INTENTNAME = "intentname";
	 public static final String RETAINED = "retained";
	 public static final String QOS = "qos";
	 
	 /*
	  * This class handles messages sent to the service by
	  * bound clients.
	  */
	 class ClientHandler extends Handler
	 {
         @Override
         public void handleMessage(Message msg)
         {
        	 boolean status = false;

        	 switch (msg.what)
        	 {
        	 case SUBSCRIBE:
           	 case PUBLISH:
           		 	/*
           		 	 * These two requests should be handled by
           		 	 * the connection thread, call makeRequest
           		 	 */
           		 	connection.makeRequest(msg);
           		 	break;
           	 case REGISTER:
        	 {
        		 Bundle b = msg.getData();
        		 if (b != null)
        		 {
        			 Object target = b.getSerializable(CLASSNAME);
        			 if (target != null)
        			 {
        				 /*
        				  * This request can be handled in-line
        				  * call the API
        				  */
        				 connection.setPushCallback((Class<?>) target);
        				 status = true;
        			 }
        			 CharSequence cs = b.getCharSequence(INTENTNAME);
        			 if (cs != null)
        			 {
        				 String name = cs.toString().trim();
        				 if (name.isEmpty() == false)
        				 {
            				 /*
            				  * This request can be handled in-line
            				  * call the API
            				  */
        					 connection.setIntentName(name);
        					 status = true;
        				 }
        			 }
        		 }
        		 ReplytoClient(msg.replyTo, msg.what, status);
        		 break;
        	 }
        	 }
         }
	 }
	 
	 private void ReplytoClient(Messenger responseMessenger, int type, boolean status)
	 {
		 /*
		  * A response can be sent back to a requester when
		  * the replyTo field is set in a Message, passed to this
		  * method as the first parameter.
		  */
		 if (responseMessenger != null)
		 {
			 Bundle data = new Bundle();
			 data.putBoolean(STATUS, status);
			 Message reply = Message.obtain(null, type);
			 reply.setData(data);
		 
			 try {
				 responseMessenger.send(reply);
			 } catch (RemoteException e) {
				 e.printStackTrace();
			 }
		 }
	 }
	 		
	public enum CONNECT_STATE
	{
		DISCONNECTED,
		CONNECTING,
		CONNECTED
	}
	
	private class MQTTConnection extends Thread
	{
		private Class<?> launchActivity = null;
		private String intentName = null;
		private MsgHandler msgHandler = null;
		//private static final int STOP = PUBLISH + 1;
		//private static final int CONNECT = PUBLISH + 2;
		//private static final int RESETTIMER = PUBLISH + 3;
		private CONNECT_STATE connState = CONNECT_STATE.DISCONNECTED;
		
		private String clientId;
		
		MQTTConnection(String clientId)
		{
			this.clientId = clientId;
			msgHandler = new MsgHandler(clientId);
			msgHandler.sendMessage(Message.obtain(null, CONNECT));
		}
		
		public String getClientId() {
			return clientId;
		}

		public void end()
		{
			msgHandler.sendMessage(Message.obtain(null, STOP));
		}
		
		public void makeRequest(Message msg)
		{
			/*
			 * It is expected that the caller only invokes
			 * this method with valid msg.what.
			 */
			msgHandler.sendMessage(Message.obtain(msg));
		}
		
		public void setPushCallback(Class<?> activityClass)
		{
			launchActivity = activityClass;
		}
		
		public void setIntentName(String name)
		{
			intentName = name;
		}
		
		private class MsgHandler extends Handler implements MqttCallback
		{
			private final int MINTIMEOUT = 2000;
			private final int MAXTIMEOUT = 8000;
			private int timeout = MINTIMEOUT;
			private MqttClient client = null;
			private MqttConnectOptions options = new MqttConnectOptions();
			private String clientId = null;
		    private String messageTopic = null;
		    private String connectionStatusTopic = null;
		    private boolean isAnnonymous = true;

			
			MsgHandler(String clientId)
			{
Log.d(this.getClass().getSimpleName(),"connect to server with clientId ["+clientId+"]");
				if (MQTTConstant.annonymousClientId.equalsIgnoreCase(clientId))
					this.isAnnonymous = true;
				else
					this.isAnnonymous = false;
				
				this.clientId = clientId;
				this.messageTopic = "IttMessagingApp/"+clientId;
				this.connectionStatusTopic = "IttMessagingApp/ConnectionStatus/"+clientId;
				//options.setCleanSession(true);
				options.setCleanSession(false);
				options.setServerURIs(MQTTConstant.ha_hosts);
				
				options.setUserName(MQTTConstant.mqttUserName);
				options.setPassword(MQTTConstant.mqttPassword.toCharArray());
				
				options.setKeepAliveInterval(MQTTConstant.mqttKeepAliveInterval);
				
				try {
					String willMsg = String.format("logout(V%s)",getResources().getString(R.string.version)); 
					options.setWill(connectionStatusTopic,willMsg.getBytes("UTF-8"),2,true);
				} catch (Exception e) {
					Log.e(getClass().getCanonicalName(), "Exception occurred in setWill(): " + e.getMessage());
				}
				
				try
				{
					String uri = MQTTConstant.ha_hosts[0];
					client = new MqttClient(uri, this.clientId, null);
					client.setCallback(this);
				}
			    catch (MqttException e1)
				{
					e1.printStackTrace();
				}
			}

			@Override
			public void handleMessage(Message msg)
			{
				System.out.println("msg.what "+msg.what);
				switch (msg.what)
				{
					case STOP:
					{
						// publish disconnect status to topic
						String message = String.format("logout(V%s)@%s",getResources().getString(R.string.version),defaultSdf.format(new Date()));
						publish(connectionStatusTopic, message, MQTTConstant.mqttPublishQOS, true);
						/*
						 * Clean up, and terminate.
						 */
						client.setCallback(null);
						if (client.isConnected())
						{
							try {
								client.disconnect();
								client.close();
							} catch (MqttException e) {
								e.printStackTrace();
							}
						}			
						//getLooper().quit();
						connState = CONNECT_STATE.DISCONNECTED;
						this.removeMessages(MQTTservice.CONNECT);
						MQTTservice.this.stopSelf();
						break;
					}
					case CONNECT:
					{
						if (connState != CONNECT_STATE.CONNECTED)
						{
						    try
							{
						    	if (!isNetworkOnline())
						    		throw new MqttException(MqttException.REASON_CODE_CONNECTION_LOST, new Throwable("No network connection."));

								// TODO publish MQTT "connecting" event
								Intent mqttStatusIntent = new Intent("mqtt_status");
								mqttStatusIntent.putExtra("status", CONNECT_STATE.CONNECTING);
								mqttStatusIntent.putExtra("timestamp",System.currentTimeMillis());
								getBaseContext().sendBroadcast(mqttStatusIntent);						    	
						    	
								client.connect(options);
								connState = CONNECT_STATE.CONNECTED;
								Log.d(getClass().getCanonicalName(), "Connected");
								timeout = MINTIMEOUT;
								// TODO publish MQTT "connected" event
								Intent mqttStatusIntent2 = new Intent("mqtt_status");
								mqttStatusIntent2.putExtra("status", CONNECT_STATE.CONNECTED);
								mqttStatusIntent2.putExtra("timestamp",System.currentTimeMillis());
								getBaseContext().sendBroadcast(mqttStatusIntent2);
							}
						    catch (MqttException e)
							{
						    	Log.d(getClass().getCanonicalName(), "Connection attempt failed with reason code = " + e.getReasonCode() + e.getCause());
								if (timeout < MAXTIMEOUT)
								{
									timeout *= 2;
								}
						    	this.sendMessageDelayed(Message.obtain(null, CONNECT), timeout);
						    	return;
							}
						    
						    if (!this.isAnnonymous) {
							    // subscribe message topic for specified deviceId
							    Message subMsg = Message.obtain(null, SUBSCRIBE);
							    Bundle subData = subMsg.getData();
							    subData.putString(TOPIC,messageTopic);
								msgHandler.sendMessage(subMsg);
								
								// publish connection status to topic
								Message pubMsg = Message.obtain(null, PUBLISH);
								Bundle pubData = pubMsg.getData();
								pubData.putString(TOPIC,connectionStatusTopic);
								//pubData.putString(MESSAGE,"login");
								pubData.putString(MESSAGE,String.format("login(V%s)@%s",getResources().getString(R.string.version),defaultSdf.format(new Date())));
								pubData.putBoolean(RETAINED, true);
								msgHandler.sendMessage(pubMsg);
						    } else {
								// subscribe to welcome message topic
							    Message subMsg = Message.obtain(null, SUBSCRIBE);
							    Bundle subData = subMsg.getData();
							    subData.putString(TOPIC,MQTTConstant.welcomeMessageTopic);
								msgHandler.sendMessage(subMsg);
						    }
							
							// timer task to send ping to server
							startKeepAlives();
						}
						break;
					}
					case RESETTIMER:
					{
						timeout = MINTIMEOUT;
						break;
					}
		        	case SUBSCRIBE:
		        	{
		        		Bundle b = msg.getData();
		        		if (b!=null && b.getString(TOPIC)!=null) {
		        			String topic = b.getString(TOPIC);
		        			boolean status = subscribe(topic);
						    Log.d(getClass().getCanonicalName(), "subscription to "+topic+": "+(status?"succeeded":"failed"));
		        		}
		        		//ReplytoClient(msg.replyTo, msg.what, status);
		        		break;
		        	}
		        	case PUBLISH:
		        	{
		        		boolean status = false;
		        		Bundle b = msg.getData();
		        		if (b != null)
		        		{
		        			CharSequence cs = b.getCharSequence(TOPIC);
		        			if (cs != null)
		        			{
		        				String topic = cs.toString().trim();
		        				if (topic.isEmpty() == false)			 
		        				{
		        					cs = b.getCharSequence(MESSAGE);
		        					boolean retained = b.getBoolean(RETAINED, false);
		        					int qos = b.getInt(QOS, MQTTConstant.mqttPublishQOS);
			            			if (cs != null)
			            			{
			            				String message = cs.toString().trim();            			 
			            				if (message.isEmpty() == false)
			            				{
			            					status = publish(topic, message, qos, retained);
			            					Log.d(getClass().getCanonicalName(), "publish to "+topic+": "+(status?"succeeded":"failed"));
			            				}
			            			}
		        				}
		        			}
		        		}
		        		//ReplytoClient(msg.replyTo, msg.what, status);
		        		break;
		        	}
		        	/*case PING:
		        	{
		        		// send ping to server
						// publish keep alive message
						publish("IttMessagingApp/KeepAlive/"+connection.getClientId(),"0",MQTTConstant.mqttKeepAliveQOS,false);
		        		break;
		        	}*/		        		
				}
			}
			
			private boolean subscribe(String topic)
			{
				try
				{
					//client.subscribe(topic);
					client.subscribe(topic,MQTTConstant.mqttSubscriptionQOS);
				}
				catch (MqttException e)
				{
					Log.d(getClass().getCanonicalName(), "Subscribe failed with reason code = " + e.getReasonCode());
					return false;
				}
				return true;
			}
			
			private boolean publish(String topic, String msg, int qos, boolean retained)
			{
				try
				{	
					MqttMessage message = new MqttMessage();
					message.setPayload(msg.getBytes());
					message.setRetained(retained);
					message.setQos(qos);
					client.publish(topic, message);
				}
				catch (MqttException e)
				{
					Log.d(getClass().getCanonicalName(), "Publish failed with reason code = " + e.getReasonCode());
					return false;
				}		
				return true;
			}
			
			@Override
			public void connectionLost(Throwable arg0)
			{
				Log.d(getClass().getCanonicalName(), "connectionLost");
				connState = CONNECT_STATE.DISCONNECTED;
				// TODO publish MQTT "disconnected" event
				Intent mqttStatusIntent = new Intent("mqtt_status");
				mqttStatusIntent.putExtra("status", CONNECT_STATE.DISCONNECTED);
				mqttStatusIntent.putExtra("timestamp",System.currentTimeMillis());
				getBaseContext().sendBroadcast(mqttStatusIntent);
				
				sendMessageDelayed(Message.obtain(null, CONNECT), timeout);
			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken arg0)
			{
			}

			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception
			{
				Log.d(getClass().getCanonicalName(), topic + ":" + message.toString());
				Context context = getBaseContext();
				/*
				PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,new Intent(context, MainActivity.class), 0);
					
				//build the notification
				Builder notificationCompat = new Builder(context);
				notificationCompat.setAutoCancel(true)  
				        .setContentIntent(pendingIntent)
						.setContentTitle("ITT")
				        .setContentText( message.toString())
				        .setSmallIcon(R.drawable.ic_launcher);

				Notification notification = notificationCompat.build();
				NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				nm.notify(mid++, notification);
				*/
				// send broadcast to receiver
				Log.d(getClass().getCanonicalName(), "send broadcast intent [com.oneport.mqtt.MESSAGE_RECEIVED]");
	            Intent msgReceivedIntent = new Intent("com.oneport.mqtt.MESSAGE_RECEIVED");
	            msgReceivedIntent.putExtra(MESSAGE, message.toString());
	            context.sendBroadcast(msgReceivedIntent);
			}
		}
	}

	public boolean isNetworkOnline() {
		boolean status = false;
		try {			
			ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo netInfo = cm.getNetworkInfo(0);
			if (netInfo != null
					&& netInfo.getState() == NetworkInfo.State.CONNECTED) {
				status = true;
			} else {
				netInfo = cm.getNetworkInfo(1);
				if (netInfo != null
						&& netInfo.getState() == NetworkInfo.State.CONNECTED)
					status = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return status;
	}

		
	private static final String ACTION_KEEPALIVE= "ACTION_KEEPALIVE";
	//private static final int MQTT_KEEP_ALIVE = 15000; // KeepAlive Interval in MS
	/**
	 * Schedules keep alives via a PendingIntent
	 * in the Alarm Manager
	 */
	private void startKeepAlives() {
		Intent i = new Intent();
		i.setClass(this, MQTTservice.class);
		i.setAction(ACTION_KEEPALIVE);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		int MQTT_KEEP_ALIVE = MQTTConstant.mqttKeepAliveInterval*1000;
		mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
				System.currentTimeMillis() + MQTT_KEEP_ALIVE,
				MQTT_KEEP_ALIVE, pi);
	}
	
	private void sendKeepAlive() {
		if (connection!=null) {
			// publish keep alive message
			Message keepAliveMsg = Message.obtain(null, PUBLISH);
			Bundle keepAliveData = keepAliveMsg.getData();
			keepAliveData.putString(TOPIC,"IttMessagingApp/KeepAlive/"+connection.getClientId());
			keepAliveData.putString(MESSAGE,"0");
			keepAliveData.putBoolean(RETAINED, false);
			keepAliveData.putInt(QOS,MQTTConstant.mqttKeepAliveQOS);
			connection.makeRequest(keepAliveMsg);
		} else {
			Log.d(getClass().getCanonicalName(), "client is not connected to MQTT server, cannot send keep alive message...");
		}
	}
}
