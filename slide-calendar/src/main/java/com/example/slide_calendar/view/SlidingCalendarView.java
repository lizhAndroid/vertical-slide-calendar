package com.example.slide_calendar.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.slide_calendar.R;
import com.example.slide_calendar.bean.DateInfoBean;
import com.example.slide_calendar.bean.MonthInfoBean;
import com.example.slide_calendar.tools.AppDateTools;
import com.example.slide_calendar.tools.UIUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SlidingCalendarView extends LinearLayout {
    //最大区间
    private int maxRange = 31;
    //最多显示月 = 当前月+之前若干个月+后面几个月
    private int passMonths = 121;
    private int futureMonths = 3;

    private Context mContext;
    private boolean isShowWeek, isFutureEnable, isPassEnable;
    private GridLayoutManager mLayoutManager;
    private RecyclerView mDateView;

    private List<DateInfoBean> mList;
    private DateAdapter mAdapter;

    private DateInfoBean mStartBean;
    private DateInfoBean mEndBean;
    private DateInfoBean mTodayBean;

    private int mStartPos;
    private boolean isAllowRange;//是否是范围日期

    public SlidingCalendarView(Context context) {
        this(context, null);
    }

    public SlidingCalendarView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingCalendarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SlidingCalendar);
        isShowWeek = typedArray.getBoolean(R.styleable.SlidingCalendar_showWeek, true);
        isFutureEnable = typedArray.getBoolean(R.styleable.SlidingCalendar_isFutureEnable, true);
        isPassEnable = typedArray.getBoolean(R.styleable.SlidingCalendar_isPassEnable, true);
        passMonths = typedArray.getInt(R.styleable.SlidingCalendar_passMonths, passMonths);
        futureMonths = typedArray.getInt(R.styleable.SlidingCalendar_futureMonths, futureMonths);
        maxRange = typedArray.getInt(R.styleable.SlidingCalendar_maxRange, maxRange);
        setBackgroundColor(getResources().getColor(R.color.colorWhite));
        typedArray.recycle();

        init();
    }

    public boolean isPassEnable() {
        return isPassEnable;
    }

    public void setPassEnable(boolean passEnable) {
        isPassEnable = passEnable;
        if (mAdapter != null) {
            mAdapter.setPassEnable(passEnable);
        }
    }

    public boolean isFutureEnable() {
        return isFutureEnable;
    }

    public void setFutureEnable(boolean futureEnable) {
        isFutureEnable = futureEnable;
        if (mAdapter != null) {
            mAdapter.setFutureEnable(futureEnable);
        }
        init();
    }

    public boolean isAllowRange() {
        return isAllowRange;
    }

    public void setAllowRange(boolean allowRange) {
        isAllowRange = allowRange;
        if (!allowRange) {
            clearAndSetStartDate(mStartBean);
        }
    }

    private void init() {
        removeAllViews();
        if (mList != null && !mList.isEmpty())
            mList.clear();
        if (isShowWeek) {
            addHeadView();
        }
        initDate();
        addCalendarView();
    }

    /**
     * 添加星期
     */
    private void addHeadView() {
        setOrientation(LinearLayout.VERTICAL);
        LinearLayout weekView = new LinearLayout(mContext);
        LayoutParams headParams = new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, UIUtils.dp2px(mContext, 32));
        weekView.setLayoutParams(headParams);
        weekView.setOrientation(LinearLayout.HORIZONTAL);
        weekView.setBackgroundColor(Color.WHITE);

        String[] arry = {"日", "一", "二", "三", "四", "五", "六"};
        LayoutParams itemParams = new LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);
        itemParams.weight = 1;
        for (String i : arry) {
            TextView tv = new TextView(mContext);
            tv.setLayoutParams(itemParams);
            tv.setGravity(Gravity.CENTER);
            tv.setTextColor(Color.BLACK);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tv.setText(i);
            weekView.addView(tv);
        }
        addView(weekView);
    }

    /**
     * 添加日期
     */
    private void addCalendarView() {
        mDateView = new RecyclerView(mContext);
        mDateView.setBackgroundColor(Color.WHITE);
        LayoutParams dateParams = new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        dateParams.leftMargin = UIUtils.dp2px(mContext, 16);
        dateParams.rightMargin = UIUtils.dp2px(mContext, 16);
        mDateView.setLayoutParams(dateParams);
        mLayoutManager = new GridLayoutManager(mContext, 7);
        mDateView.setLayoutManager(mLayoutManager);

        mDateView.addItemDecoration(new CalendarDateDecoration(mContext, new CalendarDateDecoration.ChooseCallback() {
            @Override
            public String getGroupId(int position) {
                if (position == -1) {
                    return "";
                }
                //返回年月栏数据，如2019年1月
                int size = mList.size();
                if (position < size) {
                    return mList.get(position).getGroupName();
                } else {
                    return "";
                }
            }
        }));

        mAdapter = new DateAdapter(mContext, mList, isFutureEnable, isPassEnable);
        mAdapter.setListener(new DateAdapter.OnClickDayListener() {
            @Override
            public void onClickDay(View view, DateInfoBean bean, int position) {
                //点击日期的listener
                //获取已选择的日期数目，0,1,2
                int count = getSelectDayCount();
                switch (count) {
                    case 0:
                        //尚未选择日期
                        clearAndSetStartDate(bean);
                        break;
                    case 1:
                        //已选择一天
                        DateInfoBean firstBean = getFirstSelectDay();
                        if (isSameDay(firstBean, bean)) {
                            //同一天则取消选择
                            mStartBean = null;
                            firstBean.setChooseDay(false);
                            mAdapter.notifyItemChanged(position);
                        } else {
                            if (!isAllowRange) {
                                clearAndSetStartDate(bean);
                            } else {
                                //非同一天,为区间结束天
                                if (checkChooseDate(firstBean, bean) == 1) {
                                    //第一次选择之后的一天
                                    bean.setChooseDay(true);
                                    refreshChooseUi(firstBean, bean);
                                } else if (checkChooseDate(firstBean, bean) == 0) {
                                    //第一次选择之前的一天
                                    bean.setChooseDay(true);
                                    refreshChooseUi(bean, firstBean);
                                }
                            }
                        }
                        break;
                    default:
                        //已存在区间
                        clearAndSetStartDate(bean);

                }
            }
        });

        mDateView.setAdapter(mAdapter);
        mDateView.setPadding(0, 0, 0, 0);
        mDateView.setClipChildren(false);
        mDateView.setClipToPadding(false);

        mLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int i) {
                //设置每行item个数，若是title则占7个位置，若是空白或日期则占一个位置
                return mList.get(i).getType() == DateInfoBean.TYPE_DATE_TITLE ? 7 : 1;
            }
        });

        addView(mDateView);

        //滑动到当前月
        for (int i = 0; i < mList.size(); i++) {
            DateInfoBean dateInfoBean = mList.get(i);
            if (dateInfoBean.getMonth() == Calendar.getInstance().get(Calendar.MONTH)
                    && dateInfoBean.getYear() == Calendar.getInstance().get(Calendar.YEAR)) {
                mDateView.scrollToPosition(i);
                return;
            }
        }
    }

    /**
     * 初始化日期
     */
    private void initDate() {

        List<MonthInfoBean> monthList = new ArrayList<>();

        //设置月份
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -passMonths + 1);
        for (int i = 0; i < passMonths; i++) {
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH) + 1;
            MonthInfoBean bean = new MonthInfoBean();
            bean.setYear(year);
            bean.setMonth(month);
            monthList.add(bean);

            calendar.add(Calendar.MONTH, 1);
        }
        if (isFutureEnable) {
            for (int i = 0; i < futureMonths; i++) {
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH) + 1;
                MonthInfoBean bean = new MonthInfoBean();
                bean.setYear(year);
                bean.setMonth(month);
                monthList.add(bean);

                calendar.add(Calendar.MONTH, 1);
            }
        }
        //设置日期
        calendar = Calendar.getInstance();
        for (MonthInfoBean bean : monthList) {
            List<DateInfoBean> dateList = new ArrayList<>();
            //设置当月第一天
            calendar.set(bean.getYear(), bean.getMonth() - 1, 1);
            int currentYear = calendar.get(Calendar.YEAR);
            int currentMonth = calendar.get(Calendar.MONTH);
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            //第一天之前空几天
            int firstOffset = dayOfWeek - 1;
            //当月最后一天
            calendar.add(Calendar.MONTH, 1);
            calendar.add(Calendar.DATE, -1);
            int dayOfSum = calendar.get(Calendar.DATE);
            //设置每月的dateList
            //每月开始空白
            for (int i = 0; i < firstOffset; i++) {
                DateInfoBean dateBean = new DateInfoBean();
                dateBean.setYear(currentYear);
                dateBean.setMonth(currentMonth + 1);
                dateBean.setDate(0);
                dateBean.setType(DateInfoBean.TYPE_DATE_BLANK);
                dateBean.setGroupName(dateBean.monthToString());
                dateList.add(dateBean);
            }
            //每月日期
            for (int i = 0; i < dayOfSum; i++) {
                DateInfoBean dateBean = new DateInfoBean();
                dateBean.setYear(currentYear);
                dateBean.setMonth(currentMonth + 1);
                dateBean.setDate(i + 1);
                dateBean.setType(DateInfoBean.TYPE_DATE_NORMAL);
                dateBean.setGroupName(dateBean.monthToString());
                //设置今天明天后天
                checkRecentDay(dateBean);
                //设置周末
                checkWeekend(dateBean);
                //设置节日
                dateBean.setFestival(checkFestival(currentMonth + 1, i + 1));
                dateList.add(dateBean);
            }
            //每月结束空白
            int lastDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            int lastOffset = 7 - lastDayOfWeek;
            for (int i = 0; i < lastOffset; i++) {
                DateInfoBean dateBean = new DateInfoBean();
                dateBean.setYear(currentYear);
                dateBean.setMonth(currentMonth + 1);
                dateBean.setDate(0);
                dateBean.setType(DateInfoBean.TYPE_DATE_BLANK);
                dateBean.setGroupName(dateBean.monthToString());
                dateList.add(dateBean);
            }

            bean.setDateList(dateList);
        }

        //填充日期
        mList = new ArrayList<>();
        for (MonthInfoBean bean : monthList) {
            DateInfoBean titleBean = new DateInfoBean();
            titleBean.setYear(bean.getYear());
            titleBean.setMonth(bean.getMonth());
            titleBean.setGroupName(titleBean.monthToString());
            titleBean.setType(DateInfoBean.TYPE_DATE_TITLE);
            mList.add(titleBean);
            mList.addAll(bean.getDateList());
        }
    }

    /**
     * 判断节日
     *
     * @param month
     * @param date
     * @return
     */
    private String checkFestival(int month, int date) {
        if (month == 1 && date == 1) {
            return "元旦";
        } else if (month == 2 && date == 14) {
            return "情人节";
        } else if (month == 4 && date == 5) {
            return "清明节";
        } else if (month == 5 && date == 1) {
            return "劳动节";
        } else if (month == 6 && date == 1) {
            return "儿童节";
        } else if (month == 7 && date == 1) {
            return "建党节";
        } else if (month == 8 && date == 1) {
            return "建军节";
        } else if (month == 9 && date == 10) {
            return "教师节";
        } else if (month == 10 && date == 1) {
            return "国庆";
        } else if (month == 11 && date == 11) {
            return "双11";
        } else if (month == 12 && date == 24) {
            return "平安夜";
        } else if (month == 12 && date == 25) {
            return "圣诞节";
        }
        return "";
    }

    /**
     * 初始化默认选择区间
     * endbean为空则以今天为结束
     *
     * @param startBean
     * @param endBean
     */
    public void initDate(DateInfoBean startBean, DateInfoBean endBean) {
        if (startBean == null || mTodayBean == null) {
            return;
        }
        refreshChooseUi(startBean, endBean);
    }

    /**
     * 设置是否今天后天明天
     *
     * @param bean
     */
    private void checkRecentDay(DateInfoBean bean) {
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH) + 1;
        int currentDate = calendar.get(Calendar.DATE);
        if (bean.getYear() == currentYear && bean.getMonth() == currentMonth && bean.getDate() == currentDate) {
            //今天
            mTodayBean = bean;
            bean.setRecentDay(true);
            bean.setRecentDayName(DateInfoBean.STR_RECENT_TODAY);
            //默认选择今天
            bean.setChooseDay(true);
            bean.setIntervalType(DateInfoBean.TYPE_INTERVAL_START);
            return;
        }
        calendar.add(Calendar.DATE, 1);
        currentYear = calendar.get(Calendar.YEAR);
        currentMonth = calendar.get(Calendar.MONTH) + 1;
        currentDate = calendar.get(Calendar.DATE);
        if (bean.getYear() == currentYear && bean.getMonth() == currentMonth && bean.getDate() == currentDate) {
            //明天
            bean.setRecentDay(true);
            bean.setRecentDayName(DateInfoBean.STR_RECENT_TOMORROW);
            return;
        }
        calendar.add(Calendar.DATE, 1);
        currentYear = calendar.get(Calendar.YEAR);
        currentMonth = calendar.get(Calendar.MONTH) + 1;
        currentDate = calendar.get(Calendar.DATE);
        if (bean.getYear() == currentYear && bean.getMonth() == currentMonth && bean.getDate() == currentDate) {
            //后天
            bean.setRecentDay(true);
            bean.setRecentDayName(DateInfoBean.STR_RECENT_ACQUIRED);
        }
    }

    /**
     * 获取总选中天数
     */
    private int getSelectDayCount() {
        int selectNum = 0;
        for (DateInfoBean bean : mList) {
            if (bean.getType() == DateInfoBean.TYPE_DATE_NORMAL && bean.isChooseDay()) {
                selectNum++;
            }
        }
        return selectNum;
    }

    /**
     * 获取第一个选中的
     */
    private DateInfoBean getFirstSelectDay() {
        for (DateInfoBean bean : mList) {
            if (bean.getType() == DateInfoBean.TYPE_DATE_NORMAL && bean.isChooseDay()) {
                return bean;
            }
        }
        return null;
    }

    /**
     * 判断是否同一天
     *
     * @param bean1
     * @param bean2
     * @return
     */
    private boolean isSameDay(DateInfoBean bean1, DateInfoBean bean2) {
        if (null == bean1 || null == bean2) {
            return false;
        }
        return bean1.getYear() == bean2.getYear() && bean1.getMonth() == bean2.getMonth() && bean1.getDate() == bean2.getDate();
    }

    /**
     * 判断bean和firstBean日期前后
     * -1：无效或超出最大范围或同一天
     * 0: bean在firstBean之前
     * 1：bean在firstBean之后
     *
     * @param firstBean
     * @param bean
     * @return
     */
    private int checkChooseDate(DateInfoBean firstBean, DateInfoBean bean) {
        if (null == firstBean || null == bean || isSameDay(firstBean, bean)) {
            return -1;
        }
        long firstLongTime = AppDateTools.getStringToDate(firstBean.dateToString());
        long selectLongTime = AppDateTools.getStringToDate(bean.dateToString());
        long diffLongTime = selectLongTime - firstLongTime;
        if (AppDateTools.diffTime2diffDay(Math.abs(diffLongTime)) >= maxRange) {
            return -1;
        }
        return selectLongTime - firstLongTime > 0 ? 1 : 0;
    }

    /**
     * 判断bean是否在start-end区间
     *
     * @param startBean
     * @param endBean
     * @param bean
     * @return
     */
    private boolean isInRange(DateInfoBean startBean, DateInfoBean endBean, DateInfoBean bean) {
        if (null == startBean || endBean == bean || null == bean) {
            return false;
        }
        return checkChooseDate(startBean, bean) == 1 && checkChooseDate(bean, endBean) == 1;
    }

    /**
     * 判断是否今天之后
     *
     * @param bean
     * @return
     */
    private boolean checkIsAfterToday(DateInfoBean bean) {
        if (null == bean || null == mTodayBean) {
            return true;
        }
        //转为时间戳，时间戳差转化天数判断最大范围
        long todayLongTime = AppDateTools.getStringToDate(mTodayBean.dateToString(), AppDateTools.DATE_FORMAT2);
        long selectLongTime = AppDateTools.getStringToDate(bean.dateToString(), AppDateTools.DATE_FORMAT2);
        return selectLongTime - todayLongTime > 0;
    }

    /**
     * 判断是是否是周末
     *
     * @param bean
     * @return
     */
    private void checkWeekend(DateInfoBean bean) {
        if (bean == null) {
            return;
        }

        Calendar calendar = Calendar.getInstance();
        //注意，Calendar month范围是0-11，所以要-1
        calendar.set(bean.getYear(), bean.getMonth() - 1, bean.getDate());

        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == 1 || dayOfWeek == 7) {
            bean.setWeekend(true);
        }
    }

    /**
     * 选择完起始、结束后更新UI
     *
     * @param startBean
     * @param endBean
     */
    private void refreshChooseUi(DateInfoBean startBean, DateInfoBean endBean) {
        List<DateInfoBean> dateInfoBeans = mList.subList(mList.indexOf(startBean), mList.indexOf(endBean) + 1);
        clearAndSetStartDate(startBean);
        for (DateInfoBean bean : dateInfoBeans) {
            bean.setChooseDay(bean.dateToString().equals(startBean.dateToString())
                    || bean.dateToString().equals(endBean.dateToString()));
            if (bean.getType() == DateInfoBean.TYPE_DATE_NORMAL) {
                if (bean.isChooseDay()) {
                    if (isSameDay(startBean, bean)) {
                        //第一天
                        bean.setIntervalType(DateInfoBean.TYPE_INTERVAL_START);
                        mStartBean = startBean;
                        mStartPos = mList.indexOf(bean);
                    } else if (isSameDay(endBean, bean)) {
                        //最后一天
                        bean.setIntervalType(DateInfoBean.TYPE_INTERVAL_END);
                        mEndBean = endBean;
                    }
                } else if (isInRange(startBean, endBean, bean)) {
                    //中间天
                    bean.setChooseDay(true);
                    bean.setIntervalType(DateInfoBean.TYPE_INTERVAL_MIDDLE);
                }
            }
        }
        mAdapter.notifyDataSetChanged();
        mLayoutManager.scrollToPositionWithOffset(mStartPos, UIUtils.dp2px(mContext, 32));
    }


    /**
     * 清除选中状态并设置起点
     *
     * @param startDate
     */
    private void clearAndSetStartDate(DateInfoBean startDate) {
        mStartBean = null;
        mEndBean = null;
        for (DateInfoBean bean : mList) {
            if (bean.getType() == DateInfoBean.TYPE_DATE_NORMAL && bean.isChooseDay()) {
                //清除旧选中区域
                bean.setChooseDay(false);
            }
            if (isSameDay(startDate, bean)) {
                //设置第一个选中
                mStartBean = bean;
                bean.setChooseDay(true);
                bean.setIntervalType(DateInfoBean.TYPE_INTERVAL_START);
            }
        }
        mAdapter.notifyDataSetChanged();
    }


    public void onDestroy() {
        mAdapter = null;
    }

    public void setShowWeek(boolean showWeek) {
        isShowWeek = showWeek;
        init();
    }

    public void setStartBean(DateInfoBean startBean) {
        mStartBean = startBean;
    }

    public void setEndBean(DateInfoBean endBean) {
        mEndBean = endBean;
    }

    public DateInfoBean getStartBean() {
        if (mStartBean == null) mStartBean = getFirstSelectDay();
        return mStartBean;
    }

    public DateInfoBean getEndBean() {
        return mEndBean;
    }
}
