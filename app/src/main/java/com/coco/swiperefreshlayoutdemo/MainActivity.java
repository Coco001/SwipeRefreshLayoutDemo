package com.coco.swiperefreshlayoutdemo;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.squareup.picasso.Picasso;
import com.youth.banner.Banner;
import com.youth.banner.BannerConfig;
import com.youth.banner.Transformer;
import com.youth.banner.loader.ImageLoader;

import java.io.IOException;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


/**
 * 当前类注释:下拉刷新，上拉加载更多组件实例
 */

public class MainActivity extends AppCompatActivity {
    private PullToRefreshListView mListView;
    private static final String TAG = "MAIN";
    private PullAdapter mPullAdapter;
    private List<String> mTitles;
    private Banner mBanner;
    private TPINewsData mNewsData = new TPINewsData();
    ArrayList<String> imagesUrl = new ArrayList<>();

    private Handler newHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    Log.d(TAG,"init FINISH");
                    mNewsData = (TPINewsData) msg.obj;
                    initEvent();
                    break;
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
    }

    private void initEvent() {
        List<TPINewsData.TPINewsData_Data.TPINewsData_Data_ListNewsData> news = mNewsData.data.news;
        int size = news.size();
        for (int i = 0; i < size; i++) {
            //设置图片地址构成的集合
            imagesUrl.add(news.get(i).listimage);
        }
        setBanner();
        mPullAdapter = new PullAdapter();
        mListView.setAdapter(mPullAdapter);

        mListView.setIsRefreshHead(true);
        mListView.setIsRefreshTail(true);
        mListView.setListener(new PullToRefreshListView.OnRefreshDataListener() {
            @Override
            public void onRefresh() {//进行加载数据
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(500);
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
                            Thread.sleep(500);
                            newHandler.sendEmptyMessage(2);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
    }

    private void initbannerData() {
        HttpUtil.sendOkHttpRequest("http://192.168.1.201:8080/zhbj/10007/list_1.json", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
               throw new  RuntimeException("acess net fail");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String s = response.body().string();
                Log.d(TAG,"request data success!");
                parseDataWithFastJson(s);
            }
        });
    }

    public class GlideImageLoader extends ImageLoader {
        @Override
        public void displayImage(Context context, Object path, ImageView imageView) {
            //Picasso 加载图片简单用法
            Picasso.with(context).load((String) path).into(imageView);
        }
    }

    //使用fastjson解析数据
    private void parseDataWithFastJson(String s) {
        TPINewsData data = JSON.parseObject(s, TPINewsData.class);
        Message message = new Message();
        message.what = 0;
        message.obj = data;
        newHandler.sendMessage(message);
    }

    private void initData() {
        initTitles();
        initbannerData();
    }

    private void initView() {
        setContentView(R.layout.main_layout);
        LinearLayout root = (LinearLayout) findViewById(R.id.ll_root);
        mListView = (PullToRefreshListView)findViewById(R.id.mylist);
        View view = getLayoutInflater().inflate(R.layout.banner_item, null);
        DisplayMetrics screenSize = ScreenUtil.getScreenSize(this);
        int widthPixels = screenSize.widthPixels;
        int heightPixels = screenSize.heightPixels;

        view.setLayoutParams(new AbsListView.LayoutParams(widthPixels, heightPixels/3));
        mBanner = (Banner) view.findViewById(R.id.mybanner);
        mListView.addHeaderView(view);
    }

    private void setBanner() {
        //设置banner样式
        mBanner.setBannerStyle(BannerConfig.CIRCLE_INDICATOR_TITLE_INSIDE);
        //设置图片加载器
        mBanner.setImageLoader(new GlideImageLoader());

        mBanner.setImages(imagesUrl);
        //设置banner动画效果
        mBanner.setBannerAnimation(Transformer.Accordion);
        //设置标题集合（当banner样式有显示title时）
        String[] titles = new String[]{"砍价我最行", "人脉总动员", "人脉总动员","想不到你是这样的app",
                "砍价我最行", "人脉总动员", "人脉总动员","想不到你是这样的app",
                "砍价我最行", "人脉总动员",};
        mBanner.setBannerTitles(Arrays.asList(titles));
        //设置自动轮播，默认为true
        mBanner.isAutoPlay(true);
        //设置轮播时间
        mBanner.setDelayTime(1500);
        //设置指示器位置（当banner模式中有指示器时）
        mBanner.setIndicatorGravity(BannerConfig.RIGHT);
        //banner设置方法全部调用完毕时最后调用
        mBanner.start();
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
            Log.d(TAG,mTitles.size()+"");
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
