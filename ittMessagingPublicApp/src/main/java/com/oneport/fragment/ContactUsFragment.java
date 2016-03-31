package com.oneport.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.oneport.itt.R;


public class ContactUsFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	
        View view = inflater.inflate(R.layout.fragment_contactus, null);
        
        TextView textView = (TextView) view.findViewById(R.id.lblContactUs);
        SpannableString content = new SpannableString(getString(R.string.contactus_underline));
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        textView.setText(content);
        
        TextView  tv_version = (TextView) view.findViewById(R.id.tv_version);
        tv_version.setText(getString(R.string.version_tv) + getString(R.string.version));
        

        return view;
    }	
	
	
}
