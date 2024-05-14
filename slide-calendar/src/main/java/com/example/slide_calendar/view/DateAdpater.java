package com.example.slide_calendar.view;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.slide_calendar.R;
import com.example.slide_calendar.bean.DateInfoBean;

import java.util.Calendar;
import java.util.List;

public class DateAdpater extends RecyclerView.Adapter<DateAdpater.ViewHolder> {

    private Context mContext;
    private List<DateInfoBean> mList;
    private OnClickDayListener mListener;
    private boolean mIsFutureEnable;

    public boolean isFutureEnable() {
        return mIsFutureEnable;
    }

    public void setFutureEnable(boolean futureEnable) {
        mIsFutureEnable = futureEnable;
        notifyDataSetChanged();
    }

    public DateAdpater(Context context, List<DateInfoBean> list, boolean isFutureEnable) {
        mContext = context;
        mList = list;
        mIsFutureEnable = isFutureEnable;
    }

    public void setListener(OnClickDayListener listener) {
        mListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = null;
        switch (i) {
            case DateInfoBean.TYPE_DATE_BLANK:
                view = LayoutInflater.from(mContext).inflate(R.layout.item_calendar_blank, viewGroup, false);
                break;
            case DateInfoBean.TYPE_DATE_TITLE:
                view = LayoutInflater.from(mContext).inflate(R.layout.item_calendar_title, viewGroup, false);
                break;
            case DateInfoBean.TYPE_DATE_NORMAL:
            default:
                view = LayoutInflater.from(mContext).inflate(R.layout.item_calendar_date, viewGroup, false);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int i) {

        final DateInfoBean bean = mList.get(i);

        switch (bean.getType()) {
            case DateInfoBean.TYPE_DATE_BLANK:
                //空
                break;
            case DateInfoBean.TYPE_DATE_TITLE:
                //title
                viewHolder.tvTitle.setText(bean.getGroupName());
                break;
            case DateInfoBean.TYPE_DATE_NORMAL:
            default:
                //日期
                if (bean.isRecentDay()) {
                    viewHolder.tvDay.setText(bean.getRecentDayName());
                } else if (bean.getDate() <= 0) {
                    viewHolder.tvDay.setText("");
                } else {
                    viewHolder.tvDay.setText(String.valueOf(bean.getDate()));
                }

                viewHolder.tvState.setText(bean.getFestival());

                if (bean.isChooseDay()) {
                    //选中日期
                    viewHolder.tvState.setTextColor(Color.WHITE);
                    switch (bean.getIntervalType()) {
                        case DateInfoBean.TYPE_INTERVAL_START:
                            //开始
                            viewHolder.tvDay.setTextColor(mContext.getResources().getColor(R.color.colorWhite));
                            viewHolder.itemView.setBackgroundResource(R.drawable.bg_calendar_select);
//                            viewHolder.tvState.setText("开始");
                            break;
                        case DateInfoBean.TYPE_INTERVAL_MIDDLE:
                            //中间
                            viewHolder.tvDay.setTextColor(mContext.getResources().getColor(R.color.colorText));
                            viewHolder.itemView.setBackgroundResource(R.drawable.bg_calendar_select_mid);
                            viewHolder.tvState.setText("");
                            break;
                        case DateInfoBean.TYPE_INTERVAL_END:
                            //结束
                            viewHolder.tvDay.setTextColor(mContext.getResources().getColor(R.color.colorWhite));
                            viewHolder.itemView.setBackgroundResource(R.drawable.bg_calendar_select);
//                            viewHolder.tvState.setText("结束");
                            break;

                    }
                } else {
                    //正常日期
                    viewHolder.itemView.setBackgroundColor(Color.TRANSPARENT);
                    viewHolder.viewDay.setBackgroundColor(Color.TRANSPARENT);
                    if (bean.isWeekend()) {
                        viewHolder.tvDay.setTextColor(mContext.getResources().getColor(R.color.colorWeekend));
                    } else {
                        viewHolder.tvDay.setTextColor(mContext.getResources().getColor(R.color.colorText));
                    }
                    viewHolder.tvState.setTextColor(mContext.getResources().getColor(R.color.colorText));

                }
                if (!mIsFutureEnable) {
                    if (bean.getYear() >= Calendar.getInstance().get(Calendar.YEAR)
                            && bean.getMonth() >= (Calendar.getInstance().get(Calendar.MONTH) + 1)
                            && bean.getDate() > Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) {
                        viewHolder.itemView.setEnabled(false);
                        viewHolder.tvDay.setTextColor(mContext.getResources().getColor(R.color.colorLine));
                    }
                }
                viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (bean.getType() == DateInfoBean.TYPE_DATE_NORMAL) {
                            if (mListener != null) {
                                mListener.onClickDay(v, bean, i);
                            }
                        }
                    }
                });
        }

    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position != -1 ? mList.get(position).getType() : -1;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle;
        View viewDay;
        TextView tvDay;
        TextView tvState;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            viewDay = itemView.findViewById(R.id.view_content);
            tvDay = itemView.findViewById(R.id.tv_day);
            tvState = itemView.findViewById(R.id.tv_state);
        }
    }

    public interface OnClickDayListener {
        void onClickDay(View view, DateInfoBean bean, int position);

    }
}
