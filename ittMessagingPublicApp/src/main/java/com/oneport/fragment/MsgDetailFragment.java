package com.oneport.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.oneport.itt.ITTApplication;
import com.oneport.itt.R;


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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	
    	num = getArguments().getInt("num");  
        View view = inflater.inflate(R.layout.fragment_msgdetail, null);
        TextView txtMsg = (TextView) view.findViewById(R.id.txtMsg);
        txtMsg.setText(ITTApplication.getInstance().msgManager.msgList.get(num).content);
        return view;
    }	
	
	
}
