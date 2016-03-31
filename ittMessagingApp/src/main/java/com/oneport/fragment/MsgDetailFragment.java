package com.oneport.fragment;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView.FindListener;
import android.widget.TextView;

import com.oneport.itt.ITTApplication;
import com.oneport.itt.R;
import com.oneport.model.Msg;


public class MsgDetailFragment extends Fragment {

	private int num;
    public static MsgDetailFragment newInstance(int num) {
    	MsgDetailFragment fragment = new MsgDetailFragment();
        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putInt("num", num);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		/* 
		 * new instruction message format 20160314
		 * create different fragments for different message type
		 */
    	num = getArguments().getInt("num");
    	Msg msg = ITTApplication.getInstance().msgManager.msgList.get(num);
//Log.d(this.getClass().getSimpleName(),String.format("msgType: %s, contentJson: %s",msg.msgType,msg.contentJson));    	
    	try {
			if (msg.contentJson!=null) {
				if ("R01B".equalsIgnoreCase(msg.msgType)) {
		    		return createR01BView(inflater, container, savedInstanceState);
		    	} else if ("R003".equalsIgnoreCase(msg.msgType)) {
		    		return createR003View(inflater, container, savedInstanceState);
		    	} else if ("R007".equalsIgnoreCase(msg.msgType)) {
		    		return createR007View(inflater, container, savedInstanceState);
		    	}
	    	}
    	} catch (JSONException e) {
    		Log.e(this.getClass().getSimpleName(),"Failed to parse JSON...",e);
    	}
    	
    	// otherwise...
        View view = inflater.inflate(R.layout.fragment_msgdetail, null);
        TextView txtMsg = (TextView) view.findViewById(R.id.txtMsg);
        txtMsg.setText(ITTApplication.getInstance().msgManager.msgList.get(num).content);
        return view;
    }
	
	private View createR01BView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) throws JSONException {
        View view = inflater.inflate(R.layout.fragment_msgdetail_r01b, null);
        // TODO parse JSON
        //try {
	        String jsonStr = ITTApplication.getInstance().msgManager.msgList.get(num).contentJson;
//Log.d(this.getClass().getSimpleName(),"parse json..."+num);
			JSONObject msg = new JSONObject(jsonStr);
//Log.d(this.getClass().getSimpleName(),"parse json done");

			// set corresponding values to views
			TextView content = (TextView)view.findViewById(R.id.fg_r01b_txt_content);
			content.setText(getString(R.string.fg_r01b_content,msg.getString("pickupLocationCode")));
        /*} catch (Exception e) {
        	Log.e(this.getClass().getSimpleName(),"Exception occurred",e);
        }*/
        return view;
	}

	private View createR003View(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) throws JSONException {
        View view = inflater.inflate(R.layout.fragment_msgdetail_r003, null);
        //try {
	        String jsonStr = ITTApplication.getInstance().msgManager.msgList.get(num).contentJson;
//Log.d(this.getClass().getSimpleName(),"parse json...");
			JSONObject msg = new JSONObject(jsonStr);
//Log.d(this.getClass().getSimpleName(),"parse json done");

			TextView contentPt = (TextView)view.findViewById(R.id.fg_r003_txt_content_pt);
			contentPt.setText(getString(R.string.fg_r003_content_pt,msg.getString("pickupLocationCode")));
			
			TextView contentCtnrF = (TextView)view.findViewById(R.id.fg_r003_txt_content_ctnr_f);
			contentCtnrF.setText(getString(R.string.fg_r003_content_ctnr_f,msg.getString("ctnrF"),msg.getString("actualPickupLocationF")));
			
			TextView contentCtnrA = (TextView)view.findViewById(R.id.fg_r003_txt_content_ctnr_a);
			if (!msg.isNull("ctnrA")) {
				contentCtnrA.setText(getString(R.string.fg_r003_content_ctnr_a,msg.getString("ctnrA"),msg.getString("actualPickupLocationA")));
			} else {
				contentCtnrA.setVisibility(View.GONE);
			}
			
			TextView contentGt = (TextView)view.findViewById(R.id.fg_r003_txt_content_gt);
			contentGt.setText(getString(R.string.fg_r003_content_gt,msg.getString("groundingLocationCode")));
        /*} catch (Exception e) {
        	Log.e(this.getClass().getSimpleName(),"Exception occurred",e);
        }*/
        return view;
	}
	
	private View createR007View(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) throws JSONException {
        View view = inflater.inflate(R.layout.fragment_msgdetail_r007, null);
        //try {
	        String jsonStr = ITTApplication.getInstance().msgManager.msgList.get(num).contentJson;
//Log.d(this.getClass().getSimpleName(),"parse json...");
			JSONObject msg = new JSONObject(jsonStr);
//Log.d(this.getClass().getSimpleName(),"parse json done");
			TextView contentGt = (TextView)view.findViewById(R.id.fg_r007_txt_content_gt);
			contentGt.setText(getString(R.string.fg_r007_txt_content_gt,msg.getString("groundingLocationCode")));
			
			TextView contentCtnrF = (TextView)view.findViewById(R.id.fg_r007_txt_ctnr_f);
			contentCtnrF.setText(getString(R.string.fg_r007_txt_ctnr_f,msg.getString("ctnrF"),msg.getString("actualGroundingLocationF")));
			contentCtnrF.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_dg_ctnr, 0);
			
			TextView contentCtnrA = (TextView)view.findViewById(R.id.fg_r007_txt_ctnr_a);
			if (!msg.isNull("ctnrA")) {
				contentCtnrA.setText(getString(R.string.fg_r007_txt_ctnr_a,msg.getString("ctnrA"),msg.getString("actualGroundingLocationA")));
			} else {
				contentCtnrA.setVisibility(View.GONE);
			}
			
			TextView contentNextPt = (TextView)view.findViewById(R.id.fg_r007_txt_next_pt);
			if (!msg.isNull("nextPickupLocationCode")) {				
				contentNextPt.setText(getString(R.string.fg_r007_txt_next_pt,msg.getString("nextPickupLocationCode")));
			} else {
				contentNextPt.setVisibility(View.GONE);
			}
			
			TextView contentCtnrF2 = (TextView)view.findViewById(R.id.fg_r007_txt_ctnr_f2);
			if (!msg.isNull("nextCtnrF")) {
				contentCtnrF2.setText(getString(R.string.fg_r007_txt_ctnr_f2,msg.getString("nextCtnrF"),msg.getString("nextActualPickupLocationF")));
			} else {
				contentCtnrF2.setVisibility(View.GONE);
			}

			TextView contentCtnrA2 = (TextView)view.findViewById(R.id.fg_r007_txt_ctnr_a2);
			if (!msg.isNull("nextCtnrA")) {
				contentCtnrA2.setText(getString(R.string.fg_r007_txt_ctnr_a2,msg.getString("nextCtnrA"),msg.getString("nextActualPickupLocationA")));
			} else {
				contentCtnrA2.setVisibility(View.GONE);
			}
			
			TextView contentNextGt = (TextView)view.findViewById(R.id.fg_r007_txt_next_gt);
			if (!msg.isNull("nextGroundingLocationCode")) {
				contentNextGt.setText(getString(R.string.fg_r007_txt_next_gt,msg.getString("nextGroundingLocationCode")));
			} else {
				contentNextGt.setVisibility(View.GONE);
			}
        /*} catch (Exception e) {
        	Log.e(this.getClass().getSimpleName(),"Exception occurred",e);
        }*/
        return view;
	}
}
