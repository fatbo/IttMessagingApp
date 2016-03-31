package com.oneport.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.oneport.adapter.MsgPagerAdapter;
import com.oneport.itt.ITTApplication;
import com.oneport.itt.MainActivity;
import com.oneport.itt.R;


public class MsgViewPagerFragment extends Fragment implements OnClickListener{
	
	private ViewPager viewPager;
    private MsgPagerAdapter adapter;
    private ImageButton  btnLeft;
    private ImageButton  btnRight;
    private TextView tv_empty_msg;
    private int start_position = 0;
	
    public int getStart_position() {
		return start_position;
	}

	public void setStart_position(int start_position) {
		this.start_position = start_position;
	}
	
	public void notifyAdapterDataChange(){
		if(adapter == null)return;
		adapter.notifyDataSetChanged();
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    public MsgViewPagerFragment(int start_position){
    	this.start_position = start_position;
    }
    
    public MsgViewPagerFragment(){
    	
    }

    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {   
    	
        View view = inflater.inflate(R.layout.fragment_msgviewpager, null);
        tv_empty_msg = (TextView)view.findViewById(R.id.tv_empty_msg);
        viewPager = (ViewPager) view.findViewById(R.id.pager);
        
        FragmentManager fm = getChildFragmentManager();
        adapter =  new MsgPagerAdapter(fm);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(start_position, false);
        //viewPager.setPageTransformer(true, new ZoomOutPageTransformer());
        //viewPager.setPageTransformer(true, new DepthPageTransformer());
        viewPager.setOnPageChangeListener(new OnPageChangeListener(){

			@Override
			public void onPageScrollStateChanged(int arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onPageSelected(int arg0) {
				//Log.d("onPageSelected",">> arg0: "+arg0+", adapter count: "+adapter.getCount());
				// TODO Auto-generated method stub
				((MainActivity)getActivity()).setupMsgTime(arg0);
				((MainActivity)getActivity()).updatePageIndex(arg0 + 1,ITTApplication.getInstance().msgManager.msgList.size());
				/*
				if(arg0 == 0){
					btnLeft.setVisibility(View.GONE);
				}
				if(arg0 == (adapter.getCount() - 1)){
					btnRight.setVisibility(View.GONE);
				}
				if(arg0 < (adapter.getCount() - 1) && arg0 > 0){
					btnRight.setVisibility(View.VISIBLE);
					btnLeft.setVisibility(View.VISIBLE);
				}*/				
				boolean isFirstPage = (arg0==0);
				boolean isLastPage = (arg0==(adapter.getCount()-1));
				btnLeft.setVisibility(isFirstPage?View.GONE:View.VISIBLE);
				btnRight.setVisibility(isLastPage?View.GONE:View.VISIBLE);
				
			}
        	
        });
        btnLeft = (ImageButton)view.findViewById(R.id.btnLeft);
        btnLeft.setOnClickListener(this);
        
        btnRight = (ImageButton)view.findViewById(R.id.btnRight);
        btnRight.setOnClickListener(this);
        
        
		if(adapter.getCount() == 0 || adapter.getCount() == 1){
			btnLeft.setVisibility(View.GONE);
			btnRight.setVisibility(View.GONE);
		}
		if(start_position == 0){
			btnLeft.setVisibility(View.GONE);
		}else if(start_position == adapter.getCount() - 1){
			btnRight.setVisibility(View.GONE);
		}
		
		((MainActivity)getActivity()).updatePageIndex(start_position + 1,ITTApplication.getInstance().msgManager.msgList.size());
        
		if(ITTApplication.getInstance().msgManager.msgList.size() > 0){
			tv_empty_msg.setVisibility(View.GONE);
		}
        
        return view;
    }

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
		if (v == btnLeft)
		{
			viewPager.setCurrentItem(viewPager.getCurrentItem()-1, true);
		}
		else if (v == btnRight)
		{
			viewPager.setCurrentItem(viewPager.getCurrentItem()+1, true);
		}
		
	}
	
	public class ZoomOutPageTransformer implements ViewPager.PageTransformer {
	    private static final float MIN_SCALE = 0.85f;
	    private static final float MIN_ALPHA = 0.5f;

	    public void transformPage(View view, float position) {
	        int pageWidth = view.getWidth();
	        int pageHeight = view.getHeight();

	        if (position < -1) { // [-Infinity,-1)
	            // This page is way off-screen to the left.
	            view.setAlpha(0);

	        } else if (position <= 1) { // [-1,1]
	            // Modify the default slide transition to shrink the page as well
	            float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
	            float vertMargin = pageHeight * (1 - scaleFactor) / 2;
	            float horzMargin = pageWidth * (1 - scaleFactor) / 2;
	            if (position < 0) {
	                view.setTranslationX(horzMargin - vertMargin / 2);
	            } else {
	                view.setTranslationX(-horzMargin + vertMargin / 2);
	            }

	            // Scale the page down (between MIN_SCALE and 1)
	            view.setScaleX(scaleFactor);
	            view.setScaleY(scaleFactor);

	            // Fade the page relative to its size.
	            view.setAlpha(MIN_ALPHA +
	                    (scaleFactor - MIN_SCALE) /
	                    (1 - MIN_SCALE) * (1 - MIN_ALPHA));

	        } else { // (1,+Infinity]
	            // This page is way off-screen to the right.
	            view.setAlpha(0);
	        }
	    }
	}

	public class DepthPageTransformer implements ViewPager.PageTransformer {
	    private static final float MIN_SCALE = 0.75f;

	    public void transformPage(View view, float position) {
	        int pageWidth = view.getWidth();

	        if (position < -1) { // [-Infinity,-1)
	            // This page is way off-screen to the left.
	            view.setAlpha(0);

	        } else if (position <= 0) { // [-1,0]
	            // Use the default slide transition when moving to the left page
	            view.setAlpha(1);
	            view.setTranslationX(0);
	            view.setScaleX(1);
	            view.setScaleY(1);

	        } else if (position <= 1) { // (0,1]
	            // Fade the page out.
	            view.setAlpha(1 - position);

	            // Counteract the default slide transition
	            view.setTranslationX(pageWidth * -position);

	            // Scale the page down (between MIN_SCALE and 1)
	            float scaleFactor = MIN_SCALE
	                    + (1 - MIN_SCALE) * (1 - Math.abs(position));
	            view.setScaleX(scaleFactor);
	            view.setScaleY(scaleFactor);

	        } else { // (1,+Infinity]
	            // This page is way off-screen to the right.
	            view.setAlpha(0);
	        }
	    }
	}
}
