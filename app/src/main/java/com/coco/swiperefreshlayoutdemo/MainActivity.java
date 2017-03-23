package com.coco.swiperefreshlayoutdemo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;


/**
 * 当前类注释:下拉刷新，上拉加载更多组件实例
 */

public class MainActivity extends AppCompatActivity {
    private PullToRefreshListView mListView;
    private PullAdapter mPullAdapter;
    private List<String> mTitles;

    private Handler newHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1://下滑刷新的事件处理
                    refreshTitles();
                    break;
                case 2://上拉加载的事件处理
                    moreTitles();
                    break;
            }
            mPullAdapter.notifyDataSetChanged();
            mListView.refreshStateFinish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initData();
        initEvent();
    }

    private void initEvent() {
        mListView.setIsRefreshHead(true);
        mListView.setIsRefreshTail(true);
        mListView.setListener(new PullToRefreshListView.OnRefreshDataListener() {
            @Override
            public void onRefresh() {//进行加载数据
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1500);
                            newHandler.sendEmptyMessage(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }

            @Override
            public void onLoading() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1500);
                            newHandler.sendEmptyMessage(2);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
    }

    private void initData() {
        initTitles();
        mPullAdapter = new PullAdapter();
        mListView.setAdapter(mPullAdapter);
    }

    private void initView() {
        setContentView(R.layout.main_layout);
        mListView = (PullToRefreshListView)findViewById(R.id.mylist);
    }

    private void initTitles() {
        mTitles = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            int index = i + 1;
            mTitles.add("当前是:" + index + "");
        }
    }

    private void refreshTitles() {
        List<String> newTitles = new ArrayList<String>();
        for (int i = 0; i < 5; i++) {
            int index = i + 1;
            newTitles.add("新数据是:" + index + "");
        }
        newTitles.addAll(mTitles);
        mTitles.removeAll(mTitles);
        mTitles.addAll(newTitles);
    }

    private void moreTitles() {
        List<String> newTitles = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            int index = i + 1;
            newTitles.add("更多数据是:" + index + "");
        }
        mTitles.addAll(newTitles);
    }

    class PullAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mTitles.size();
        }

        @Override
        public Object getItem(int position) {
            return mTitles.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView = new TextView(MainActivity.this);
            textView.setText(mTitles.get(position));
            textView.setTextSize(25);
            return textView;
        }
    }

}
