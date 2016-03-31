package com.oneport.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.oneport.itt.R;

public class UpdateDialog extends DialogFragment{
	
	UpdateListener listener;
	
	public interface UpdateListener{
		public abstract void onUpdateClick();
	}
	
	public UpdateDialog(UpdateListener listener){
		this.listener = listener;
	}

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_update, null);
        
        Button update = (Button)view.findViewById(R.id.btn_update);
        update.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				listener.onUpdateClick();
			}});
        return view;
	}
	
}
