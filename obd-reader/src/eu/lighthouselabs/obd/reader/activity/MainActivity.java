package eu.lighthouselabs.obd.reader.activity;

import java.util.ArrayList;
import java.util.List;
import eu.lighthouselabs.obd.reader.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;

/**
 * The main activity.
 */

public class MainActivity extends Activity {
	
	private ViewPager mPager;
	private ObdPageAdapter mPageAdapter;
	private Button enterBtn; 
	private int carIndex = 0;
	private ArrayList<View> mListViews;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		enterBtn = (Button)findViewById(R.id.enter);
		mPageAdapter = new ObdPageAdapter();
		mPager = (ViewPager) findViewById(R.id.pager);	
		mPager.setAdapter(mPageAdapter);
		mListViews = new ArrayList<View>();		  
		
		LayoutInflater inflater = LayoutInflater.from(getBaseContext()); 
        mListViews.add(inflater.inflate(R.layout.item1, null));
        mListViews.add(inflater.inflate(R.layout.item2, null));
        mListViews.add(inflater.inflate(R.layout.item3, null));
        mListViews.add(inflater.inflate(R.layout.item4, null));
        mListViews.add(inflater.inflate(R.layout.item5, null));
        
        mPager.setCurrentItem(mListViews.size()*100);
              
        mPager.setOnPageChangeListener(new OnPageChangeListener(){        	
        	@Override
            public void onPageSelected(int position) {
                carIndex = position;
            }          
                     
            @Override
            public void onPageScrollStateChanged(int state) {
            }
            
            @Override
            public void onPageScrolled(int position, float offset, int offsetPixels) {            	
            }
        });
        
        enterBtn.setOnClickListener(new Button.OnClickListener(){
			@Override
            public void onClick(View v) {
				Bundle bundle = new Bundle();
				bundle.putInt("carIndex",carIndex % 5);
				Intent intent = new Intent();
				intent.setClass(MainActivity.this, MonitorActivity.class);
				intent.putExtras(bundle);
				startActivity(intent);
			}
		});
	}
	
	protected class ObdPageAdapter extends PagerAdapter {   
		@Override
		public Object instantiateItem(ViewGroup container, int position) {
				((ViewPager)container).addView(mListViews.get(position % mListViews.size()), 0);  
	            return mListViews.get(position % mListViews.size());  
		}
		
		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			((ViewPager)container).removeView(mListViews.get(position % mListViews.size())); 
		}
		
		 @Override
		public int getCount() {
	        return 1000;
	    }
		
		@Override
		public boolean isViewFromObject(View view, Object object) {
			return (view==object);
		}
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}
}