package com.lizh.slidingcalendar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.slide_calendar.bean.DateInfoBean;
import com.example.slide_calendar.view.SlidingCalendarView;

public class SelectDateActivity extends Activity {

    public static final String TAG_SELECT = "select_result";
    private SlidingCalendarView scv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_date);
        scv = findViewById(R.id.scv_main);
        scv.setAllowRange(true);
//        scv.setFutureEnable(true);
//        scv.setPassEnable(false);
        DateInfoBean dateInfoBean = new DateInfoBean();
        dateInfoBean.setYear(2024);
        dateInfoBean.setMonth(8);
        dateInfoBean.setDate(3);
        DateInfoBean dateInfoBean2 = new DateInfoBean();
        dateInfoBean2.setYear(2024);
        dateInfoBean2.setMonth(8);
        dateInfoBean2.setDate(5);
        scv.initDate(dateInfoBean,dateInfoBean2);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scv != null) {
            scv.onDestroy();
        }
    }

    public void selectOver(View view) {
        //完成button
        DateInfoBean startBean = scv.getStartBean();
        DateInfoBean endBean = scv.getEndBean();
        if (startBean == null) {
            return;
        }
        if (endBean == null) {
            endBean = startBean;
        }

        Intent intent = getIntent();
        intent.putExtra(TAG_SELECT, startBean.dateToString() + "至" + endBean.dateToString());
        setResult(-1, intent);
        finish();

    }
}
