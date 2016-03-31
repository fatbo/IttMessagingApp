package com.oneport.fragment;

import java.util.ArrayList;
import java.util.Arrays;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import com.oneport.fragment.SelectPopUpDialog.SelectPopUpDialogItemClickListener;
import com.oneport.itt.ITTApplication;
import com.oneport.itt.ITTApplication.LanguageType;
import com.oneport.itt.MainActivity;
import com.oneport.itt.R;


public class SettingFragment extends Fragment {
	
	SelectPopUpDialog dialog;
	ArrayList<String> language_arr;
	TextView tv_language,label_language,lblSetting;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	
        View view = inflater.inflate(R.layout.fragment_setting, null);

        SpannableString content = new SpannableString(getString(R.string.setting_underline));
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
		tv_language = (TextView) view.findViewById(R.id.tv_language);
		label_language = (TextView) view.findViewById(R.id.tv_label_language); 
		lblSetting = (TextView) view.findViewById(R.id.lblSetting); 
		lblSetting.setText(content);
		tv_language.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				showLanguagePopUp();
			}});
		tv_language.setText(ITTApplication.getInstance().getCurrentLanguage());
		language_arr = new ArrayList<String>(Arrays.asList(getActivity()
				.getResources().getStringArray(R.array.language_arr)));
        
        return view;
    }	
    
	private void showLanguagePopUp() {
		dialog = new SelectPopUpDialog();
		dialog.setSelections(language_arr);
		dialog.setListener(new SelectPopUpDialogItemClickListener(){

			@Override
			public void onItemClickListener(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				if (tv_language != null)
					tv_language.setText(((TextView) view).getText().toString());
				switch (position) {
				case 0:
					ITTApplication.getInstance().changeLanguage(LanguageType.TC);
					break;
				case 1:
					ITTApplication.getInstance().changeLanguage(LanguageType.SC);
					break;
				case 2:
					ITTApplication.getInstance().changeLanguage(LanguageType.ENG);
					break;
				}
				if (getActivity() != null)
					((MainActivity)getActivity()).refreshUI();
				refreshFragmentUI();
				if (dialog != null)
					dialog.dismissAllowingStateLoss();
			}
			
		});
		if (getActivity() == null)
			return;
		dialog.setStyle(DialogFragment.STYLE_NO_FRAME, R.style.fullpagedialog);
		dialog.show(getActivity().getSupportFragmentManager(),
				"SelectPopUpDialog");
	}
	
	private void refreshFragmentUI(){
        SpannableString content = new SpannableString(getString(R.string.setting_underline));
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
		label_language.setText(this.getResources().getString(R.string.language));
		lblSetting.setText(content);
	}
	
	
}
