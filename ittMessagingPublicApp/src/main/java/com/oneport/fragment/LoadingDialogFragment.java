package com.oneport.fragment;

import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.oneport.itt.R;

public class LoadingDialogFragment extends DialogFragment implements OnKeyListener
{
	int mBackStackId;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) 
    {
        View view = inflater.inflate(R.layout.dialog_fragment_loading, container, false);
        return view;
    }
	
	@Override
    public void onStart() 
    {
        super.onStart();
    
        getDialog().setOnKeyListener(this);
    }

	// OnKeyListener
	@Override
	public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
		return true;
	}


	public int show(FragmentTransaction transaction, String tag,boolean allowStateLoss) {
		transaction.add(this, tag);
	    mBackStackId = allowStateLoss ? transaction.commitAllowingStateLoss() : transaction.commit();
	    return mBackStackId;
	} 
	
	
}