package com.oneport.mqtt;

public class MQTTConstant {
	//public static final String host = "125.215.144.216";	 
    //public static final int port = 1883;
	// the first element is the primary host	
	public static final String[] ha_hosts = {"tcp://www.e1port.com:1883"};
	//public static final String[] ha_hosts = {"tcp://itt.oneport.com:1883"};
	
	public static final String mqttUserName = "sub_client";
	public static final String mqttPassword = "password";
	public static final int mqttKeepAliveInterval = 60;
	public static final int mqttSubscriptionQOS = 2;
	public static final int mqttPublishQOS = 2;
	public static final int mqttKeepAliveQOS = 0;

	public static final String annonymousClientId = "Annonymous";
	public static final String welcomeMessageTopic = "IttMessagingApp/WelcomeMessage";
}
