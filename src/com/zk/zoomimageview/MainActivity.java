package com.zk.zoomimageview;

import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class MainActivity extends ActionBarActivity {

	private int[] imgRes = new int[]{
		R.drawable.test,
		R.drawable.test1
	};
	private ZoomImageView[] imgs = new ZoomImageView[imgRes.length];
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		((ViewPager)findViewById(R.id.mViewPager)).setAdapter(new MyAdapter());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private class MyAdapter extends PagerAdapter {
		
		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView(imgs[position]);
		}

		@Override
		public ZoomImageView instantiateItem(ViewGroup container, int position) {
			ZoomImageView mImage = new ZoomImageView(getBaseContext());
			mImage.setImageResource(imgRes[position]);
			container.addView(mImage);
			imgs[position] = mImage;
			return mImage;
		}

		@Override
		public int getCount() {
			return imgRes.length;
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == arg1;
		}
		
	}
}
