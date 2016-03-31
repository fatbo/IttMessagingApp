package com.oneport.manager;

import java.util.ArrayList;

import com.oneport.model.Msg;


public class MsgManager {
	
	static public MsgManager sharedManager = null;
	
	static public MsgManager getInstance()
	{
		return sharedManager;
	}

	public ArrayList <Msg> msgList;
	
	public void init()
	{
		//dummy
		msgList = new ArrayList <Msg>();

		{
			Msg msg = new Msg();
			msg.msgId = "1";
			msg.time = "20115/12/13 32:12";
			msg.content = "fns353454v3\n 45432er> 1";
			msgList.add(msg);
		}

		{
			Msg msg = new Msg();
			msg.msgId = "2";
			msg.time = "20115/12/13 32:13";
			msg.content = "fns353454v342135432er> 2";
			msgList.add(msg);
		}

		{
			Msg msg = new Msg();
			msg.msgId = "3";
			msg.time = "20115/12/13 32:14";
			msg.content = "fns353454v34sfdf5432er> 3";
			msgList.add(msg);
		}
		
		{
			Msg msg = new Msg();
			msg.msgId = "4";
			msg.time = "20115/12/13 32:14";
			msg.content = "fns353454v34sfdf5432er> 4";
			msgList.add(msg);
		}
		
		{
			Msg msg = new Msg();
			msg.msgId = "5";
			msg.time = "20115/12/13 32:14";
			msg.content = "fns353454v34sfdf5432er> 5";
			msgList.add(msg);
		}		
		
		{
			Msg msg = new Msg();
			msg.msgId = "6";
			msg.time = "20115/12/13 32:14";
			msg.content = "fns353454v34sfdf5432er> 6";
			msgList.add(msg);
		}		
		
	}
	

}
