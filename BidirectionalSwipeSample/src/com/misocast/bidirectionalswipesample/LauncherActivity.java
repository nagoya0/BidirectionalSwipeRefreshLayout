package com.misocast.bidirectionalswipesample;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class LauncherActivity extends ListActivity {

	public static final String[] options = { "ListView", "ExpandableListView", "GridView", "WebView", "ScrollView" };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, options));
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent;

		switch (position) {
		case 0:
			intent = new Intent(this, SwipeRefreshListActivity.class);
			break;
		case 1:
			intent = new Intent(this, SwipeRefreshExpandableListActivity.class);
			break;
		case 2:
			intent = new Intent(this, SwipeRefreshGridActivity.class);
			break;
		case 3:
			intent = new Intent(this, SwipeRefreshWebViewActivity.class);
			break;
		case 4:
			intent = new Intent(this, SwipeRefreshScrollViewActivity.class);
			break;
		default:
			return;
		}

		startActivity(intent);
	}

}
