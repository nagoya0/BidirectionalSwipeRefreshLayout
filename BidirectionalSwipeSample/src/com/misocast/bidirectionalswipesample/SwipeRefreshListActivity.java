package com.misocast.bidirectionalswipesample;

import java.util.ArrayList;

import com.misocast.bidirectionalswiperefreshlayout.BidirectionalSwipeRefreshLayout;

import android.app.ListActivity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ArrayAdapter;

public class SwipeRefreshListActivity extends ListActivity {

	private ArrayList<String> myStringArray;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_srl_list);

		myStringArray = new ArrayList<String>();
		for(int i = 0; i < 20; i++) {
			myStringArray.add(Integer.toString(i));
		}

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, myStringArray);
		setListAdapter(adapter);

		final BidirectionalSwipeRefreshLayout swipeRefreshLayout = (BidirectionalSwipeRefreshLayout) this.findViewById(R.id.swipe_refresh);
		swipeRefreshLayout.setColorSchemeColors(Color.GREEN, Color.RED, Color.BLUE, Color.YELLOW);
		swipeRefreshLayout.setOnRefreshListener(new BidirectionalSwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				new Handler().postDelayed(new Runnable(){
					@Override
					public void run() {
						swipeRefreshLayout.setRefreshing(false);
					}}, 3000);
			}});
	}

}
