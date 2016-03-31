package com.oneport.fragment;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.oneport.itt.R;

public class MsgAlertDialog extends DialogFragment {
	Button btn_close;
	TextView tv_msg;
	String title;
	MsgAlertListener listener;
	int mBackStackId;

	
	public interface MsgAlertListener{
		public abstract void onCloseClick();
	}

	public MsgAlertDialog() {

	}

	public MsgAlertDialog(String title) {
		this.title = title;
	}
	
	public MsgAlertDialog(String title,MsgAlertListener listener) {
		this.title = title;
		this.listener = listener;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.dialog_alert, container,
				false);
		setupView(view);
		return view;
	}

	private void setupView(View view) {
		btn_close = (Button) view.findViewById(R.id.btn_close);
		tv_msg = (TextView)view.findViewById(R.id.tv_msg);
		tv_msg.setText(title);
		btn_close.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				MsgAlertDialog.this.dismissAllowingStateLoss();
				if(listener != null)
					listener.onCloseClick();
			}

		});
	}
	
	public int show(FragmentTransaction transaction, String tag,boolean allowStateLoss) {
		transaction.add(this, tag);
	    mBackStackId = allowStateLoss ? transaction.commitAllowingStateLoss() : transaction.commit();
	    return mBackStackId;
	} 

}
