package view;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.szpt.hasee.szpt.R;

import java.text.SimpleDateFormat;
import java.util.Date;
/**
 * Created by hasee on 2016/11/28.
 */
    public class ReFlashListView extends ListView implements AbsListView.OnScrollListener {
        View header;// 顶部布局文件；
        View footer;// 底部布局；

        int lastVisibleItem;// 最后一个可见的item；
        int totalItemCount;// 总数量；
        boolean isLoading;// 正在加载底部数据；

        int headerHeight;// 顶部布局文件的高度；
        int firstVisibleItem;// 当前第一个可见的item的位置；
        int scrollState;// listview 当前滚动状态；
        boolean isRemark;// 标记，当前是在listview最顶端摁下的；
        int startY;// 摁下时的Y值；
        //on move
        int state;// 当前的状态；
        final int NONE = 0;// 正常状态；
        final int PULL = 1;// 提示下拉状态；
        final int RELESE = 2;// 提示释放状态；
        final int REFLASHING = 3;// 刷新状态；
        IReflashListener iReflashListener;//刷新数据的接口

        public ReFlashListView(Context context) {
            super(context);
            initView(context);
        }

        public ReFlashListView(Context context, AttributeSet attrs) {
            super(context, attrs);
            initView(context);
        }

        public ReFlashListView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            initView(context);
        }
        /**
         * 初始化界面，添加顶部布局文件到 listview
         *
         * @param context
         */
        private void initView(Context context) {
            LayoutInflater inflater = LayoutInflater.from(context);
            header = inflater.inflate(R.layout.header, null);
            measureView(header);
            headerHeight = header.getMeasuredHeight();
            Log.i("tag", "headerHeight = " + headerHeight);
            topPadding(-headerHeight);
            //底部布局
            footer = inflater.inflate(R.layout.footer_layout, null);
            footer.findViewById(R.id.load_layout).setVisibility(View.GONE);

            this.addHeaderView(header);
            this.addFooterView(footer);
            this.setOnScrollListener(this);

        }

        /**
         * 通知父布局，占用的宽，高；
         *
         * @param view
         */
        private void measureView(View view) {
            ViewGroup.LayoutParams p=view.getLayoutParams();
            if(p==null){
                p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            int width = ViewGroup.getChildMeasureSpec(0, 0, p.width);
            int height;
            int tempHeight = p.height;
            if(tempHeight>0){
                height = MeasureSpec.makeMeasureSpec(tempHeight,
                        MeasureSpec.EXACTLY);
            }else {
                height = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            }
            view.measure(width, height);
        }


        /**
         * 设置header 布局 上边距；
         *
         * @param topPadding
         */
        private void topPadding(int topPadding) {
            header.setPadding(header.getPaddingLeft(),topPadding,getPaddingRight(),getPaddingBottom());
            header.invalidate();
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            this.scrollState = scrollState;
            if (totalItemCount == lastVisibleItem
                    && scrollState == SCROLL_STATE_IDLE) {
                if (!isLoading) {
                    isLoading = true;
                    footer.findViewById(R.id.load_layout).setVisibility(
                            View.VISIBLE);
                    // 加载更多
                    iReflashListener.onLastflash();
                }
            }
        }

        @Override
        public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            this.firstVisibleItem=firstVisibleItem;
            this.lastVisibleItem = firstVisibleItem + visibleItemCount;
            this.totalItemCount = totalItemCount;

        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (firstVisibleItem == 0) {
                        isRemark = true;
                        startY = (int) ev.getY();
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    onMove(ev);
                    break;
                case MotionEvent.ACTION_UP:
                    if (state == RELESE) {
                        state = REFLASHING;
                        // 加载最新数据；
                        reflashViewByState();
                        iReflashListener.onReflash();
                    } else if (state == PULL) {
                        state = NONE;
                        isRemark = false;
                        reflashViewByState();
                    }
                    break;
            }
            return super.onTouchEvent(ev);
        }

        /**
         * 判断移动过程操作；
         *
         * @param ev
         */
        private void onMove(MotionEvent ev) {
            if (!isRemark) {
                return;
            }
            int tempY = (int) ev.getY();
            int space = tempY - startY;
            int topPadding = space - headerHeight;
            switch (state) {
                case NONE:
                    if (space > 0) {
                        state = PULL;
                        reflashViewByState();
                    }
                    break;
                case PULL:
                    topPadding(topPadding);
                    if (space > headerHeight + 20
                            && scrollState == SCROLL_STATE_TOUCH_SCROLL) {
                        state = RELESE;
                        reflashViewByState();
                    }
                    break;
                case RELESE:
                    topPadding(topPadding);
                    if (space < headerHeight + 20) {
                        state = PULL;
                        reflashViewByState();
                    } else if (space <= 0) {
                        state = NONE;
                        isRemark = false;
                        reflashViewByState();
                    }
                    break;
            }


        }
        /**
         * 根据当前状态，改变界面显示；
         */
        private void reflashViewByState() {
            TextView tip = (TextView) header.findViewById(R.id.tip);
            ImageView arrow = (ImageView) header.findViewById(R.id.arrow);
            ProgressBar progress = (ProgressBar) header.findViewById(R.id.progress);
            RotateAnimation anim = new RotateAnimation(0, 180,
                    RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                    RotateAnimation.RELATIVE_TO_SELF, 0.5f);
            anim.setDuration(500);
            anim.setFillAfter(true);
            RotateAnimation anim1 = new RotateAnimation(180, 0,
                    RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                    RotateAnimation.RELATIVE_TO_SELF, 0.5f);
            anim1.setDuration(500);
            anim1.setFillAfter(true);
            switch (state) {
                case NONE:
                    arrow.clearAnimation();
                    topPadding(-headerHeight);
                    break;

                case PULL:
                    arrow.setVisibility(View.VISIBLE);
                    progress.setVisibility(View.GONE);
                    tip.setText("下拉可以刷新！");
                    arrow.clearAnimation();
                    arrow.setAnimation(anim1);
                    break;
                case RELESE:
                    arrow.setVisibility(View.VISIBLE);
                    progress.setVisibility(View.GONE);
                    tip.setText("松开可以刷新！");
                    arrow.clearAnimation();
                    arrow.setAnimation(anim);
                    break;
                case REFLASHING:
                    topPadding(42);
                    arrow.setVisibility(View.GONE);
                    progress.setVisibility(View.VISIBLE);
                    tip.setText("正在刷新...");
                    arrow.clearAnimation();
                    break;
            }
        }
        /**
         * 获取完数据；
         */
        public void reflashComplete() {
            state = NONE;
            isRemark = false;
            reflashViewByState();
            TextView lastupdatetime = (TextView) header
                    .findViewById(R.id.lastupdate_time);
            SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 hh:mm:ss");
            Date date = new Date(System.currentTimeMillis());
            String time = format.format(date);
            lastupdatetime.setText(time);
            //
            isLoading = false;
            footer.findViewById(R.id.load_layout).setVisibility(
                            View.GONE);
        }
        public void setInterface(IReflashListener iReflashListener){
            this.iReflashListener = iReflashListener;
        }
        /**
         * 刷新数据接口
         * @author Administrator
         */
        public interface IReflashListener{
            void onReflash();
            void onLastflash();
        }

    }
