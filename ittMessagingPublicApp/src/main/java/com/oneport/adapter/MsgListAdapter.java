package com.oneport.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.oneport.itt.ITTApplication;
import com.oneport.itt.R;
import com.oneport.manager.MsgManager;
import com.oneport.manager.SqliteController;
import com.oneport.model.Msg;


public class MsgListAdapter extends BaseAdapter {

	Context context;
	 public MsgListAdapter(Context context) {
		 super();
		 
		 this.context = context;
	}
	 
	@Override
	public int getCount() {
    	int numOfItem = MsgManager.getInstance().msgList.size();
    	if(numOfItem > SqliteController.show_amount)
    		numOfItem = SqliteController.show_amount;
        return numOfItem;
	}

	@Override
	public Object getItem(int position) {
		return ITTApplication.getInstance().msgManager.msgList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		final Msg item = (Msg)getItem(position);

		ViewHolder holder = null;
		View v = convertView;
		
		if (convertView == null) {
			
			holder=new ViewHolder();
			LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = mInflater.inflate(R.layout.item_msg, null);
			v.setTag(holder);
			holder.txtMsg = (TextView)v.findViewById(R.id.txtMsg);
			holder.txtTime = (TextView)v.findViewById(R.id.txtTime);
		}
        else {
        	holder = (ViewHolder)v.getTag();
        }
		

		holder.txtMsg.setText((position + 1)  + ". " + item.content);
		holder.txtTime.setText(item.time);
		//holder.txtTime.setText(item.msgDateTime);
		
		return v;
	}

    public static class ViewHolder {
        public TextView txtMsg;
        public TextView txtTime;
    }
	
}
