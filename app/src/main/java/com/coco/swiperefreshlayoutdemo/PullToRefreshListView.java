package com.coco.swiperefreshlayoutdemo;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 当前类注释:下拉刷新，上拉加载更多组件
 */
public class PullToRefreshListView extends ListView {
    private static final int UP_REFRESH = 1;           // 下拉-默认为初始状态  准备下拉刷新
    private static final int RELEASE_TO_REFRESH = 2;   // 释放刷新
    private static final int REFRESHING = 3;           // 正在刷新
    private static final int DOWN_REFRESH = 4;         // 上滑加载
    private static final int RELEASE_DOWN_REFRESH = 5;   // 释放加载
    private OnRefreshDataListener mListener;  //刷新数据的监听回调
    private OnScrollListener mOnScrollListener;

    private LinearLayout mHeadView;//下拉刷新的的头部view
    private ImageView mHeadViewImage;
    private ProgressBar mHeadViewProgress;
    private TextView mHeadViewText;
    private TextView mHeadViewLastUpdated;
    private int mHeadState;
    private int mCurrentScrollState;
    private int mHeadViewHeight;
    private int mTopPadding;

    private LinearLayout mTailView;//下拉刷新的的头部view
    private ProgressBar mTailViewProgress;
    private TextView mTailViewText;
    private int mTailState;
    private int mTailViewHeight;
    private int mTailPadding;
    private View myBanner;     //轮播图的view
    private RotateAnimation mFlipAnimation;
    private RotateAnimation mReverseFlipAnimation;
    private int mLastMotionY;
    private boolean isEnablePullRefresh;   //是否启用下拉刷新
    private boolean isLoadingMore;         //是否启用加载更多
    private boolean isEnableLoadingMore;   //是否启用下拉刷新
    private int listViewLocation;          //list组件在屏幕中的位置

    public PullToRefreshListView(Context context) {
        this(context, null);
    }

    public PullToRefreshListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        initAniamtion();
        initView(context);
        initEvent();
        resetHeaderPadding();
    }

    private void initEvent() {
        setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (!isEnableLoadingMore) {//没有启动加载更多的功能
                    return;
                }

                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    if (getLastVisiblePosition() == getAdapter().getCount() - 1) {
                        mTailViewText.setText("正在加载最新数据....");
                        onLoading();
                    }
                }

                mCurrentScrollState = scrollState;
                if (mOnScrollListener != null) {
                    mOnScrollListener.onScrollStateChanged(view, scrollState);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (mCurrentScrollState == SCROLL_STATE_TOUCH_SCROLL && mHeadState != REFRESHING) {
                    if (firstVisibleItem == 0) {//显示的是listview的第一条数据
                        if ((mHeadView.getBottom() >= mHeadViewHeight) && mHeadState != RELEASE_TO_REFRESH) {
                            mHeadViewText.setText("松开即可刷新");
                            mHeadViewImage.clearAnimation();
                            mHeadViewImage.startAnimation(mFlipAnimation);
                            mHeadState = RELEASE_TO_REFRESH;
                        } else if (mHeadView.getBottom() < mHeadViewHeight && mHeadState != UP_REFRESH) {
                            mHeadViewText.setText("下拉可以刷新");
                            mHeadViewImage.clearAnimation();
                            mHeadViewImage.startAnimation(mReverseFlipAnimation);
                            mHeadState = UP_REFRESH;
                        }
                    }
                }

                if (mOnScrollListener != null) {
                    mOnScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
                }
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                if (!isEnablePullRefresh || mCurrentScrollState == REFRESHING || !isBannerShow()) {
                    break;
                }

                int offsetY = (int) event.getY();
                int deltY = Math.round(offsetY - mLastMotionY);
                mLastMotionY = offsetY;
                deltY = deltY / 2;

                if (getFirstVisiblePosition() == 0 && mHeadState != REFRESHING) {
                    mTopPadding += deltY;
                    if (mTopPadding < -mHeadViewHeight) {
                        mTopPadding = -mHeadViewHeight;
                    }
                    resetHeaderPadding();
                }
                break;
            case MotionEvent.ACTION_UP:
                //当手指抬开得时候 进行判断下拉的距离 ，如果>=临界值，那么进行刷新，否则回归原位
                if (!isVerticalScrollBarEnabled()) {
                    setVerticalScrollBarEnabled(true);
                }
                if (getFirstVisiblePosition() == 0 && mHeadState != REFRESHING) {
                    if (mHeadView.getBottom() >= mHeadViewHeight && mHeadState == RELEASE_TO_REFRESH) {
                        prepareForRefresh();
                    } else {
                        resetHeader();
                    }
                }
                if (getLastVisiblePosition() == getAdapter().getCount() - 1 && mTailState != DOWN_REFRESH) {
                    if (mTailView.getBottom() >= mTailViewHeight && mTailState == RELEASE_DOWN_REFRESH) {
                        prepareForLoadingMore();
                    } else {
                        resetTail();
                    }
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    public void prepareForLoadingMore() {
        if (mTailState != DOWN_REFRESH) {
            mTailState = DOWN_REFRESH;
            mTailPadding = 0;
            resetTailPadding();
            mTailViewText.setText("正在加载数据");
            onLoading();
        }
    }

    public void prepareForRefresh() {
        if (mHeadState != REFRESHING) {
            mHeadState = REFRESHING;
            mTopPadding = 0;
            resetHeaderPadding();
            mHeadViewImage.clearAnimation();
            mHeadViewImage.setVisibility(View.GONE);
            mHeadViewProgress.setVisibility(View.VISIBLE);
            mHeadViewText.setText("正在刷新数据...");
            onRefresh();
        }
    }

    private void resetHeader() {
        mHeadState = UP_REFRESH;
        mTopPadding = -mHeadViewHeight;
        resetHeaderPadding();
        mHeadViewImage.clearAnimation();
        mHeadViewImage.setVisibility(View.VISIBLE);
        mHeadViewProgress.setVisibility(View.GONE);
        mHeadViewText.setText("下拉可以刷新");
        mHeadViewLastUpdated.setText(getCurrentFormatData());
    }

    private void resetTail() {
        mTailState = DOWN_REFRESH;
        mTailPadding = -mTailViewHeight;
        resetTailPadding();
        mTailViewText.setText("上拉加载更多");
    }

    private void resetHeaderPadding() {
        mHeadView.setPadding(0, mTopPadding, 0, 0);
    }

    private void resetTailPadding() {
        mTailView.setPadding(0, mTailPadding, 0, 0);
    }

    //刷新数据成功，处理结果
    public void refreshStateFinish() {
        if (isLoadingMore) {
            isLoadingMore = false;
            resetTail();
        } else {
            resetHeader();
        }
    }

    public void setListener(OnRefreshDataListener listener) {
        mListener = listener;
    }

    public interface OnRefreshDataListener {
        void onRefresh();

        void onLoading();
    }

    public void onRefresh() {
        if (mListener != null) {
            mListener.onRefresh();
        }
    }

    public void onLoading() {
        if (mListener != null) {
            mListener.onLoading();
        }
    }

    //用户自己选择是否启动加载更多的功能
    public void setIsRefreshTail(boolean isLoadingMore) {
        isEnableLoadingMore = isLoadingMore;
    }

    //用户自己选择是否启动下拉刷新的功能
    public void setIsRefreshHead(boolean isPullRefresh) {
        isEnablePullRefresh = isPullRefresh;
    }

    private void initTailView(Context context) {
        mTailView = (LinearLayout) View.inflate(context, R.layout.my_tail_view, null);//上滑刷新的view
        mTailViewProgress = (ProgressBar) mTailView.findViewById(R.id.load_next_page_progress);
        mTailViewText = (TextView) mTailView.findViewById(R.id.load_next_page_text);
        mTailState = DOWN_REFRESH;
        mTailViewProgress.setMinimumHeight(50);//设置上滑的最小高度为50
        setFadingEdgeLength(0);
        setFooterDividersEnabled(false);
        addFooterView(mTailView);//把mTailView加入到listview的尾部
        mTailView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        mTailViewHeight = mTailView.getMeasuredHeight();
        mTailPadding = -mTailViewHeight;
    }

    private void initHeadView(Context context) {
        mHeadView = (LinearLayout) View.inflate(context, R.layout.my_head_view, null);//下拉刷新的view
        mHeadViewText = (TextView) mHeadView.findViewById(R.id.pull_to_refresh_text);
        mHeadViewImage = (ImageView) mHeadView.findViewById(R.id.pull_to_refresh_image);
        mHeadViewProgress = (ProgressBar) mHeadView.findViewById(R.id.pull_to_refresh_progress);
        mHeadViewLastUpdated = (TextView) mHeadView.findViewById(R.id.pull_to_refresh_updated_at);
        mHeadViewLastUpdated.setText(getCurrentFormatData());
        mHeadState = UP_REFRESH;
        mHeadViewImage.setMinimumHeight(50); //设置下拉最小的高度为50
        setFadingEdgeLength(0);
        setHeaderDividersEnabled(false);
        addHeaderView(mHeadView);//把mHeadView加入到listview的头部
        mHeadView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        mHeadViewHeight = mHeadView.getMeasuredHeight();
        mTopPadding = -mHeadViewHeight;
    }

    @Override
    public void addHeaderView(View v) {
        if (isEnablePullRefresh) {
            myBanner = v;
            mHeadView.addView(v);
        } else {
            super.addHeaderView(v);
        }
    }

    private void initView(Context context) {
        initHeadView(context);
        initTailView(context);
    }

    private void initAniamtion() {
        mFlipAnimation = new RotateAnimation(0, -180, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mFlipAnimation.setInterpolator(new LinearInterpolator());
        mFlipAnimation.setDuration(250);
        mFlipAnimation.setFillAfter(true);
        mReverseFlipAnimation = new RotateAnimation(-180, 0, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mReverseFlipAnimation.setInterpolator(new LinearInterpolator());
        mReverseFlipAnimation.setDuration(250);
        mReverseFlipAnimation.setFillAfter(true);
    }

    //设置时间格式
    private String getCurrentFormatData() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(new Date());
    }

    //对轮播图的判断
    private boolean isBannerShow() {
        if (null == myBanner) {
            return true;
        }
        int[] location = new int[2];
        if (0 == listViewLocation) {
            this.getLocationOnScreen(location);
            listViewLocation = location[1];
        }
        myBanner.getLocationOnScreen(location);
        if (location[1] < listViewLocation) {
            return false;
        }
        return true;
    }
}
