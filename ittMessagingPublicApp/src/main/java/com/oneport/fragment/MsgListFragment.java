package com.oneport.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.oneport.adapter.MsgListAdapter;
import com.oneport.itt.ITTApplication;
import com.oneport.itt.MainActivity;
import com.oneport.itt.R;

public class MsgListFragment extends Fragment implements OnItemClickListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.fragment_msglist, null);

		ListView list = (ListView) view.findViewById(R.id.list);
		TextView tv_empty_msg = (TextView) view.findViewById(R.id.tv_empty_msg);
		MsgListAdapter adapter = new MsgListAdapter(getActivity());
		list.setAdapter(adapter);
		list.setOnItemClickListener(this);
		if (getActivity() != null)
			((MainActivity) getActivity()).setupLastMsgTime();
		if (ITTApplication.getInstance().msgManager.msgList.size() > 0) {
			tv_empty_msg.setVisibility(View.GONE);
		}
		return view;
	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View v, int position,
			long arg3) {

		MsgViewPagerFragment msgViewPagerFragment = new MsgViewPagerFragment(
				position);
		((MainActivity) getActivity()).replaceFragment(msgViewPagerFragment);
		((MainActivity) getActivity()).setupMsgTime(position);
	}

}
