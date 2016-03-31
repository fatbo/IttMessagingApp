package com.oneport.fragment;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.oneport.itt.R;

public class SelectPopUpDialog extends DialogFragment implements
		OnItemClickListener {
	ListView listview_popup;
	SelectionAdapter adapter;
	ArrayList<String> selections;
	SelectPopUpDialogItemClickListener listener;
	ArrayList<String> list;

	public interface SelectPopUpDialogItemClickListener {
		public void onItemClickListener(AdapterView<?> parent, View view,
				int position, long id);
	}

	public SelectPopUpDialog() {

	}


	/*public SelectPopUpDialog(ArrayList<String> selections,
			SelectPopUpDialogItemClickListener listener) {
		this.selections = selections;
		this.listener = listener;
	}*/

	public void setSelections(ArrayList<String> selections) {
		this.selections = selections;
	}

	public void setListener(SelectPopUpDialogItemClickListener listener) {
		this.listener = listener;
	}

	// @Override
	// public Dialog onCreateDialog(Bundle savedInstanceState) {
	// final Dialog dialog = new Dialog(getActivity());
	// // dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
	// // dialog.getWindow().setBackgroundDrawable(
	// // new ColorDrawable(Color.TRANSPARENT));
	// dialog.setContentView(R.layout.dialog_select_popup);
	// dialog.setCancelable(false);
	// setupView(dialog);
	// return dialog;
	// }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.dialog_select_popup, container,
				false);
		setupView(view);
		return view;
	}

	private void setupView(View view) {
		listview_popup = (ListView) view.findViewById(R.id.listview_popup);
		if (getActivity() != null) {
			adapter = new SelectionAdapter(getActivity(), selections);
			listview_popup.setAdapter(adapter);
			listview_popup.setOnItemClickListener(this);
		}
	}

	private class SelectionAdapter extends ArrayAdapter<String> {
		public SelectionAdapter(Context context, List<String> objects) {
			super(context, R.layout.adapter_view_select_popup, objects);
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return super.getCount();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			return super.getView(position, convertView, parent);
		}

	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		listener.onItemClickListener(parent, view, position, id);
	}

}
