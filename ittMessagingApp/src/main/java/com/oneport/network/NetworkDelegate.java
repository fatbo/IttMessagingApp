package com.oneport.network;

import java.util.ArrayList;

import com.android.volley.VolleyError;
import com.oneport.model.Msg;

public interface NetworkDelegate{
	
	public abstract void didActivation(String deviceID , Error error);
	public abstract void didUpdateUUID(Error error);
	public abstract void didRetrieveMsgReload(String tid,ArrayList<Msg> msgList,Error error);
	public abstract void didRetrieveMsg(String tid,ArrayList<Msg> msgList,Error error);
	public abstract void didCheckVersion(String currentVersion , Error error);
	public abstract void failToConnect(VolleyError error);
}
