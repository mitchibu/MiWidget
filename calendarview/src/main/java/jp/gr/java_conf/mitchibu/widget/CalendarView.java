package jp.gr.java_conf.mitchibu.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.MonthDisplayHelper;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.TextView;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;

@SuppressWarnings("unused")
public class CalendarView extends ViewGroup {
	private final Queue<View> poolDayOfWeek = new LinkedList<>();
	private final Queue<View> poolDay = new LinkedList<>();
	private final GestureDetector detector;
	private final int dayOfWeekLayout;
	private final int dayLayout;

	private MonthDisplayHelper helper = null;
	private int selectedDayOfMonth = 0;
	private OnItemClickListener listener = null;

	public CalendarView(Context context) {
		this(context, null);
	}

	public CalendarView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CalendarView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		detector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onDown(MotionEvent e) {
				return true;
			}

			@Override
			public boolean onSingleTapConfirmed(MotionEvent e) {
				if(listener != null) {
					Rect rect = new Rect();
					for(int i = 0, n = getChildCount(); i < n; ++i) {
						View child = getChildAt(i);
						ViewGroup.LayoutParams params = child.getLayoutParams();
						if(params instanceof LayoutParams && ((LayoutParams)params).type == LayoutParams.TYPE_DAY) {
							child.getHitRect(rect);
							if(rect.contains((int)e.getX(), (int)e.getY())) {
								listener.onItemClick(CalendarView.this, helper.getYear(), helper.getMonth(), ((LayoutParams)params).dayOfMonth);
							}
						}
					}
				}
				return true;
			}
		});

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CalendarView, defStyleAttr, R.style.Widget_CalendarView);
		try {
			dayOfWeekLayout = a.getResourceId(R.styleable.CalendarView_dayOfWeekLayout, 0);
			dayLayout = a.getResourceId(R.styleable.CalendarView_dayLayout, 0);
		} finally {
			a.recycle();
		}

		Calendar c = Calendar.getInstance();
		setDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH));
	}

	public void setDate(int year, int month) {
		setDate(year, month, -1);
	}

	public void setDate(int year, int month, int dayOfMonth) {
		selectedDayOfMonth = dayOfMonth;
		if(helper == null || helper.getYear() != year || helper.getMonth() != month) {
			helper = new MonthDisplayHelper(year, month);
			requestLayout();
		}
		for(int i = 0, n = getChildCount(); i < n; ++i) {
			View child = getChildAt(i);
			ViewGroup.LayoutParams params = child.getLayoutParams();
			if(params instanceof LayoutParams && ((LayoutParams)params).type == LayoutParams.TYPE_DAY) {
				if(child instanceof Checkable) {
					((Checkable)child).setChecked(((LayoutParams)params).dayOfMonth == dayOfMonth);
				}
			}
		}
	}

	public void setOnItemClickListener(OnItemClickListener listener) {
		this.listener = listener;
	}

	@Override
	public void addView(View child) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addView(View child, int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addView(View child, int width, int height) {
		throw new UnsupportedOperationException();
	}

//	@Override
//	public void addView(View child, LayoutParams params) {
//		throw new UnsupportedOperationException();
//	}
//
//	@Override
//	public void addView(View child, int index, LayoutParams params) {
//		throw new UnsupportedOperationException();
//	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return detector.onTouchEvent(event) || super.onTouchEvent(event);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		for(int i = 0, n = getChildCount(); i < n; ++ i) {
			View child = getChildAt(i);
			ViewGroup.LayoutParams params = child.getLayoutParams();
			if(params != null && params instanceof LayoutParams) {
				switch(((LayoutParams)params).type) {
				case LayoutParams.TYPE_DAY_OF_WEEK:
					poolDayOfWeek.offer(child);
					break;
				case LayoutParams.TYPE_DAY:
					poolDay.offer(child);
					break;
				}
			}
		}
		removeAllViewsInLayout();

		int childWidth = getMeasuredWidth() / 7;
		int childHeight = getMeasuredWidth() / 7;

		int maxHeight = layoutDayOfWeek(childWidth, childHeight);
		layoutDay(childWidth, childHeight, maxHeight);
	}

	@SuppressWarnings("UnusedParameters")
	private int layoutDayOfWeek(int width, int height) {
		int maxHeight = 0;
		for(int i = Calendar.SUNDAY, x = 0; i <= Calendar.SATURDAY; ++ i, x += width) {
			View v = dayOfWeek(i, poolDayOfWeek.poll());
			ViewGroup.LayoutParams params = v.getLayoutParams();
			if(params == null || !(params instanceof LayoutParams)) {
				params = new LayoutParams(width, LayoutParams.WRAP_CONTENT, LayoutParams.TYPE_DAY_OF_WEEK);
			} else {
				params.width = width;
				params.height = LayoutParams.WRAP_CONTENT;
				((LayoutParams)params).dayOfMonth = 0;
			}
			addViewInLayout(v, -1, params);
			int measureWidth = MeasureSpec.makeMeasureSpec(params.width, MeasureSpec.EXACTLY);
			int measureHeight = MeasureSpec.makeMeasureSpec(params.height, MeasureSpec.UNSPECIFIED);
			v.measure(measureWidth, measureHeight);
			v.layout(x, 0, x + v.getMeasuredWidth(), v.getMeasuredHeight());
			if(maxHeight < v.getHeight()) maxHeight = v.getHeight();
		}
		return maxHeight;
	}

	private void layoutDay(int width, int height, int offset) {
		for(int i = 1; i <= helper.getNumberOfDaysInMonth(); ++ i) {
			int x = width * helper.getColumnOf(i);
			int y = offset + height * helper.getRowOf(i);
			View v = day(helper.getYear(), helper.getMonth(), i, poolDay.poll());
			ViewGroup.LayoutParams params = v.getLayoutParams();
			if(params == null || !(params instanceof LayoutParams)) {
				params = new LayoutParams(width, height, LayoutParams.TYPE_DAY, i);
			} else {
				params.width = width;
				params.height = height;
				((LayoutParams)params).dayOfMonth = i;
			}
			if(v instanceof Checkable) {
				((Checkable)v).setChecked(selectedDayOfMonth == i);
			}
			addViewInLayout(v, -1, params);
			int measureWidth = MeasureSpec.makeMeasureSpec(params.width, MeasureSpec.EXACTLY);
			int measureHeight = MeasureSpec.makeMeasureSpec(params.height, MeasureSpec.EXACTLY);
			v.measure(measureWidth, measureHeight);
			v.layout(x, y, x + v.getMeasuredWidth(), y + v.getMeasuredHeight());
		}
	}

	View dayOfWeek(int dayOfWeek, View convertView) {
		if(convertView == null) {
			convertView = LayoutInflater.from(getContext()).inflate(dayOfWeekLayout, this, false);
		}
		bindDayOfWeek(dayOfWeek, convertView);
		return convertView;
	}

	private void bindDayOfWeek(int dayOfWeek, View convertView) {
		View view = convertView.findViewById(android.R.id.text1);
		if(view instanceof TextView) {
			Calendar c = Calendar.getInstance();
			c.set(Calendar.DAY_OF_WEEK, dayOfWeek);
			((TextView)convertView).setText(DateUtils.formatDateTime(getContext(), c.getTimeInMillis(), DateUtils.FORMAT_SHOW_WEEKDAY|DateUtils.FORMAT_ABBREV_WEEKDAY));
		}
	}

	@SuppressWarnings("UnusedParameters")
	View day(int year, int month, int dayOfMonth, View convertView) {
		if(convertView == null) {
			convertView = LayoutInflater.from(getContext()).inflate(dayLayout, this, false);
		}
		bindDay(year, month, dayOfMonth, convertView);
		return convertView;
	}

	@SuppressLint("SetTextI18n")
	@SuppressWarnings("UnusedParameters")
	private void bindDay(int year, int month, int dayOfMonth, View convertView) {
		View view = convertView.findViewById(android.R.id.text1);
		if(view instanceof TextView) {
			((TextView)view).setText("" + dayOfMonth);
		}
	}

	static class LayoutParams extends ViewGroup.LayoutParams {
		static final int TYPE_DAY_OF_WEEK = 0;
		static final int TYPE_DAY = 1;

		int type;
		int dayOfMonth;

		LayoutParams(int width, int height, int type) {
			this(width, height, type, 0);
		}

		LayoutParams(int width, int height, int type, int dayOfMonth) {
			super(width, height);
			this.type = type;
			this.dayOfMonth = dayOfMonth;
		}
	}

	public interface OnItemClickListener {
		void onItemClick(CalendarView view, int year, int month, int dayOfMonth);
	}
}
