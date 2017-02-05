package jp.gr.java_conf.mitchibu.miwidget;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import jp.gr.java_conf.mitchibu.widget.CalendarView;

public class MainActivity extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		CalendarView c = (CalendarView)findViewById(R.id.view);
		c.setOnItemClickListener(new CalendarView.OnItemClickListener() {
			@Override
			public void onItemClick(CalendarView view, int year, int month, int dayOfMonth) {
				view.setDate(year, month, dayOfMonth);
			}
		});
	}
}
