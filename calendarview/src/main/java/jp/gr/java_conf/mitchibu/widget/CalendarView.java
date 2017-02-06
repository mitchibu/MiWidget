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

	private CalendarAdapter adapter;
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
			public boolean onSingleTapUp(MotionEvent e) {
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
		int dayOfWeekLayout;
		int dayLayout;
		try {
			dayOfWeekLayout = a.getResourceId(R.styleable.CalendarView_dayOfWeekLayout, 0);
			dayLayout = a.getResourceId(R.styleable.CalendarView_dayLayout, 0);
		} finally {
			a.recycle();
		}

		Calendar c = Calendar.getInstance();
		setDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH));
		setCalendarAdapter(new DefaultCalendarAdapter(context, dayOfWeekLayout, dayLayout));
	}

	public void setCalendarAdapter(CalendarAdapter adapter) {
		this.adapter = adapter;
		requestLayout();
	}

	public int getYear() {
		return helper.getYear();
	}

	public int getMonth() {
		return helper.getMonth();
	}

	public int getDayOfMonth() {
		return selectedDayOfMonth;
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
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = MeasureSpec.getSize(widthMeasureSpec);
		int height = MeasureSpec.getSize(heightMeasureSpec);

		int childWidth = calcChildWidth(width, height);
		int childHeight = calcChildHeight(width, height);
		if(MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST) {
			width = childWidth * 7;
		}
		if(MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {
			height = childHeight * 7 + layoutDayOfWeek(childWidth, childHeight);
		}
		setMeasuredDimension(width, height);
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
		if(adapter == null) return;

		int width = getMeasuredWidth();
		int height = getMeasuredHeight();
		int size = Math.min(width, height);
		int childWidth = calcChildWidth(width, height);
		int childHeight = calcChildHeight(width, height);

		int maxHeight = layoutDayOfWeek(childWidth, childHeight);
		layoutDay(childWidth, childHeight, maxHeight);
	}

	private int calcChildWidth(int width, int height) {
		return Math.min(width, height) / 7;
	}

	private int calcChildHeight(int width, int height) {
		return Math.min(width, height) / 7;
	}

	@SuppressWarnings("UnusedParameters")
	private int layoutDayOfWeek(int width, int height) {
		int maxHeight = 0;
		for(int i = Calendar.SUNDAY, x = 0; i <= Calendar.SATURDAY; ++ i, x += width) {
			View child = adapter.getDayOfWeekView(i, poolDayOfWeek.poll(), this);
			ViewGroup.LayoutParams params = child.getLayoutParams();
			if(params == null || !(params instanceof LayoutParams)) {
				params = new LayoutParams(width, LayoutParams.WRAP_CONTENT, LayoutParams.TYPE_DAY_OF_WEEK);
			} else {
				params.width = width;
				params.height = LayoutParams.WRAP_CONTENT;
				((LayoutParams)params).dayOfMonth = 0;
			}
			addViewInLayout(child, -1, params);
			int measureWidth = MeasureSpec.makeMeasureSpec(params.width, MeasureSpec.EXACTLY);
			int measureHeight = MeasureSpec.makeMeasureSpec(params.height, MeasureSpec.UNSPECIFIED);
			child.measure(measureWidth, measureHeight);
			child.layout(x, 0, x + child.getMeasuredWidth(), child.getMeasuredHeight());
			if(maxHeight < child.getHeight()) maxHeight = child.getHeight();
		}
		return maxHeight;
	}

	private void layoutDay(int width, int height, int offset) {
		for(int i = 1; i <= helper.getNumberOfDaysInMonth(); ++ i) {
			int x = width * helper.getColumnOf(i);
			int y = offset + height * helper.getRowOf(i);
			View child = adapter.getDayView(helper.getYear(), helper.getMonth(), i, poolDay.poll(), this);
			ViewGroup.LayoutParams params = child.getLayoutParams();
			if(params == null || !(params instanceof LayoutParams)) {
				params = new LayoutParams(width, height, LayoutParams.TYPE_DAY, i);
			} else {
				params.width = width;
				params.height = height;
				((LayoutParams)params).dayOfMonth = i;
			}
			if(child instanceof Checkable) {
				((Checkable)child).setChecked(selectedDayOfMonth == i);
			}
			addViewInLayout(child, -1, params);
			int measureWidth = MeasureSpec.makeMeasureSpec(params.width, MeasureSpec.EXACTLY);
			int measureHeight = MeasureSpec.makeMeasureSpec(params.height, MeasureSpec.EXACTLY);
			child.measure(measureWidth, measureHeight);
			child.layout(x, y, x + child.getMeasuredWidth(), y + child.getMeasuredHeight());
		}
	}

	@SuppressWarnings("WeakerAccess")
	public static class LayoutParams extends ViewGroup.LayoutParams {
		static final int TYPE_DAY_OF_WEEK = 0;
		static final int TYPE_DAY = 1;

		int type;
		int dayOfMonth;

		public LayoutParams(int width, int height, int type) {
			this(width, height, type, 0);
		}

		public LayoutParams(int width, int height, int type, int dayOfMonth) {
			super(width, height);
			this.type = type;
			this.dayOfMonth = dayOfMonth;
		}
	}

	@SuppressWarnings("WeakerAccess")
	public static class DefaultCalendarAdapter implements CalendarAdapter {
		private final Context context;
		private final int dayOfWeekLayout;
		private final int dayLayout;

		public DefaultCalendarAdapter(Context context, int dayOfWeekLayout, int dayLayout) {
			this.context = context;
			this.dayOfWeekLayout = dayOfWeekLayout;
			this.dayLayout = dayLayout;
		}

		@Override
		public View getDayOfWeekView(int dayOfWeek, View convertView, ViewGroup parent) {
			if(convertView == null) {
				convertView = LayoutInflater.from(context).inflate(dayOfWeekLayout, parent, false);
			}
			View view = convertView.findViewById(android.R.id.text1);
			if(view instanceof TextView) {
				Calendar c = Calendar.getInstance();
				c.set(Calendar.DAY_OF_WEEK, dayOfWeek);
				((TextView)convertView).setText(DateUtils.formatDateTime(context, c.getTimeInMillis(), DateUtils.FORMAT_SHOW_WEEKDAY|DateUtils.FORMAT_ABBREV_WEEKDAY));
			}
			return convertView;
		}

		@SuppressLint("SetTextI18n")
		@Override
		public View getDayView(int year, int month, int dayOfMonth, View convertView, ViewGroup parent) {
			if(convertView == null) {
				convertView = LayoutInflater.from(context).inflate(dayLayout, parent, false);
			}
			View view = convertView.findViewById(android.R.id.text1);
			if(view instanceof TextView) {
				((TextView)view).setText("" + dayOfMonth);
			}
			return convertView;
		}
	}

	public interface OnItemClickListener {
		void onItemClick(CalendarView view, int year, int month, int dayOfMonth);
	}

	public interface CalendarAdapter {
		View getDayOfWeekView(int dayOfWeek, View convertView, ViewGroup parent);
		View getDayView(int year, int month, int dayOfMonth, View convertView, ViewGroup parent);
	}
}
