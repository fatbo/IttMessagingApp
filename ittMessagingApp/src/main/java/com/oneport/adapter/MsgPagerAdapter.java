package com.oneport.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.oneport.fragment.MsgDetailFragment;
import com.oneport.manager.MsgManager;
import com.oneport.manager.SqliteController;


public class MsgPagerAdapter extends FragmentPagerAdapter {
    public MsgPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public int getCount() {
    	int numOfItem = MsgManager.getInstance().msgList.size();
    	if(numOfItem > SqliteController.show_amount)
    		numOfItem = SqliteController.show_amount;
        return numOfItem;
    }

    @Override
    public Fragment getItem(int position) {
        return MsgDetailFragment.newInstance(position);
    }
	
}
