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
import android.widget.ImageView;
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
    	//long start = System.currentTimeMillis();
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
//Log.d(this.getClass().getSimpleName(),"time elapsed: "+(System.currentTimeMillis()-start));        
        return view;
    }
	
	private View createR01BView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) throws JSONException {
        View view = inflater.inflate(R.layout.fragment_msgdetail_r01b, null);
        String jsonStr = ITTApplication.getInstance().msgManager.msgList.get(num).contentJson;
		JSONObject msg = new JSONObject(jsonStr);

		// set corresponding values to views
		TextView content = (TextView)view.findViewById(R.id.fg_r01b_txt_content);
		content.setText(getString(R.string.fg_r01b_content,msg.getString("pickupLocationCode")));
		
		/*ImageView imgReefer = (ImageView)view.findViewById(R.id.fg_r01b_img_reefer);
		imgReefer.setImageResource(R.drawable.ic_reefer);
		
		ImageView imgDanger = (ImageView)view.findViewById(R.id.fg_r01b_img_danger);
		imgDanger.setImageResource(R.drawable.ic_danger);
		
		ImageView imgDamage = (ImageView)view.findViewById(R.id.fg_r01b_img_damage);
		imgDamage.setImageResource(R.drawable.ic_damage);*/
		View ctnrTypeLayout = view.findViewById(R.id.fg_r01b_ctnr_type);
		applyCtnrTypeLayout(ctnrTypeLayout,true,true,true);
		
		
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
			View ctnrFTypeLayout = view.findViewById(R.id.fg_r003_ctnr_f_type);
			applyCtnrTypeLayout(ctnrFTypeLayout,true,true,true);
			
			TextView contentCtnrA = (TextView)view.findViewById(R.id.fg_r003_txt_content_ctnr_a);
			if (!msg.isNull("ctnrA")) {
				contentCtnrA.setText(getString(R.string.fg_r003_content_ctnr_a,msg.getString("ctnrA"),msg.getString("actualPickupLocationA")));
				View ctnrATypeLayout = view.findViewById(R.id.fg_r003_ctnr_a_type);
				applyCtnrTypeLayout(ctnrATypeLayout,true,false,true);
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
			View ctnrFTypeLayout = view.findViewById(R.id.fg_r007_ctnr_f_type);
			applyCtnrTypeLayout(ctnrFTypeLayout,true,false,false);
			
			TextView contentCtnrA = (TextView)view.findViewById(R.id.fg_r007_txt_ctnr_a);
			if (!msg.isNull("ctnrA")) {
				contentCtnrA.setText(getString(R.string.fg_r007_txt_ctnr_a,msg.getString("ctnrA"),msg.getString("actualGroundingLocationA")));
				View ctnrATypeLayout = view.findViewById(R.id.fg_r007_ctnr_a_type);
				applyCtnrTypeLayout(ctnrATypeLayout,false,true,false);
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
				//contentCtnrF2.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,R.drawable.ic_danger,0);
				View ctnrF2TypeLayout = view.findViewById(R.id.fg_r007_ctnr_f2_type);
				applyCtnrTypeLayout(ctnrF2TypeLayout,false,true,true);
			} else {
				contentCtnrF2.setVisibility(View.GONE);
			}

			TextView contentCtnrA2 = (TextView)view.findViewById(R.id.fg_r007_txt_ctnr_a2);
			if (!msg.isNull("nextCtnrA")) {
				contentCtnrA2.setText(getString(R.string.fg_r007_txt_ctnr_a2,msg.getString("nextCtnrA"),msg.getString("nextActualPickupLocationA")));
				//contentCtnrA2.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,R.drawable.ic_damage,0);
				View ctnrA2TypeLayout = view.findViewById(R.id.fg_r007_ctnr_a2_type);
				applyCtnrTypeLayout(ctnrA2TypeLayout,false,false,true);
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
	
	// configure the layout layout/ctnr_type.xml
	// hide the layout if the container is not reefer, danger or damaged
	private void applyCtnrTypeLayout(View ctnrTypeLayout, boolean isReefer, boolean isDanger, boolean isDamage) {
		if (!isReefer && !isDanger && !isDamage) {
			ctnrTypeLayout.setVisibility(View.GONE);
		} else {
			ctnrTypeLayout.setVisibility(View.VISIBLE);
			if (isReefer) {
				ImageView imgReefer = (ImageView)ctnrTypeLayout.findViewById(R.id.layout_ctnr_type_img_reefer);
				imgReefer.setVisibility(View.VISIBLE);
			}
			
			if (isDanger) {
				ImageView imgDanger = (ImageView)ctnrTypeLayout.findViewById(R.id.layout_ctnr_type_img_danger);
				imgDanger.setVisibility(View.VISIBLE);
			}
			
			if (isDamage) {
				ImageView imgDamage = (ImageView)ctnrTypeLayout.findViewById(R.id.layout_ctnr_type_img_damage);
				imgDamage.setVisibility(View.VISIBLE);
			}
		}
	}
}
