package com.smartmux.androidapp;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.smartmux.androidapp.adapter.SearchAdapter;
import com.smartmux.androidapp.database.DatabaseHandler;
import com.smartmux.androidapp.util.Constant;

public class TutorialSearchCatagoryTitleActivity extends Activity implements OnItemClickListener,OnClickListener{
	
	//private List<JSONArray> tutorialList = new ArrayList<JSONArray>();
	private List<String> tutorialCatagoryTitle = new ArrayList<String>();
	private String rootTitle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search_item);
		
		getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		getActionBar().setCustomView(R.layout.tutorial_actionbar);

		TextView txtActionBarTitle = (TextView) this
				.findViewById(R.id.textView_numberoftutorial);
		txtActionBarTitle.setText(getString(R.string.category));
		this.findViewById(R.id.Button_list).setVisibility(View.GONE);
		this.findViewById(R.id.textView_top).setVisibility(View.VISIBLE);
		this.findViewById(R.id.textView_top).setOnClickListener(this);
		this.findViewById(R.id.back_content).setOnClickListener(this);
		
		if (getIntent().hasExtra(Constant.root_title)) {
				 rootTitle = getIntent().getExtras().getString(Constant.root_title);

				 TextView txtTitle = (TextView) findViewById(R.id.textView_numberoftutorial);
					txtTitle.setText(rootTitle);
				
				DatabaseHandler db = new DatabaseHandler(this);
				
				String query = "SELECT DISTINCT "+Constant.category+" FROM "+Constant.TABLE_TUTORIAL+" WHERE "+Constant.root_title+" ='"+rootTitle+"'";
				
				Cursor cursor = db.getData(query);
				if (cursor.moveToFirst()) {
					do {
						
						tutorialCatagoryTitle.add(cursor.getString(cursor
								.getColumnIndex(Constant.category)));
					} while (cursor.moveToNext());
				}
				
				db.close();
				cursor.close();
				
				final SearchAdapter adapter = (new SearchAdapter(this, tutorialCatagoryTitle));
				ListView listview = (ListView) this
						.findViewById(R.id.listView_searchitem);
				listview.setAdapter(adapter);

				listview.setOnItemClickListener(this);

				EditText searchKey = (EditText) this
						.findViewById(R.id.editText_item_search);
				searchKey.setCursorVisible(false);
				searchKey.addTextChangedListener(new TextWatcher() {

					@Override
					public void onTextChanged(CharSequence cs, int arg1, int arg2,
							int arg3) {
						adapter.getFilter().filter(cs.toString());

					}

					@Override
					public void beforeTextChanged(CharSequence arg0, int arg1,
							int arg2, int arg3) {
					}

					@Override
					public void afterTextChanged(Editable arg0) {

					}
				});

		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
	

		Intent intent = new Intent(TutorialSearchCatagoryTitleActivity.this,
				TutorialSearchSubCatagoryActivity.class);
		intent.putExtra(Constant.root_title, rootTitle);
		intent.putExtra(Constant.category, tutorialCatagoryTitle.get(position));
		startActivity(intent);
		overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
		
	}

	@Override
	public void onClick(View v) {
		
		int id = v.getId();

		if (id == R.id.back_content || id == R.id.textView_top) {

			finish();
			overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
		}
		
	}
	@Override
	public void onBackPressed(){
		finish();
		overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
	}


}
