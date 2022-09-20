package com.ironxiao.ipcheck;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.Editable;
import android.text.Selection;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import java.util.concurrent.ExecutionException;

public class SpeedTestActivity extends Activity {
    private TextView logView;
    private SpeedTestService mSpeedTestService;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private static final long COMMON_DELAY = 1000;


    private final Runnable mSpeedTestTask = new Runnable() {
        @Override
        public void run() {
            if (mSpeedTestService.allowStart()) {
                try {
                    mSpeedTestService.start();
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                mHandler.postDelayed(mSpeedTestTask, COMMON_DELAY);
            }
        }
    };

    private final Runnable mFinishTask = new Runnable() {
        @Override
        public void run() {
            if (mSpeedTestService.allowStart()) {
                SpeedTestActivity.this.finish();
            } else {
                mHandler.postDelayed(mFinishTask, COMMON_DELAY);
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed_test);
        logView = findViewById(R.id.log);
        logView.setMovementMethod(ScrollingMovementMethod.getInstance());
        logView.setText("欢迎来到测试页面... ...");
        SpeedTestConfig config = generateConfigFromIntent();
        mSpeedTestService = SpeedTestService.getInstance(this);
        mSpeedTestService.setConfig(config);
        mSpeedTestService.setOnMessageListener(this::printLog2Screen);

        mHandlerThread = new HandlerThread(SpeedTestActivity.class.getSimpleName());
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mHandler.postDelayed(mSpeedTestTask, COMMON_DELAY);
    }

    private void printLog2Screen(String msg) {
        logView.post(() -> {
            synchronized (logView) {
                logView.append("\n" + msg);
                Editable editable = logView.getEditableText();
                Selection.setSelection(editable, editable.length());
            }
        });
    }

    private SpeedTestConfig generateConfigFromIntent() {
        printLog2Screen("\n开始读取测试配置... ...");
        Intent it = getIntent();

        String ipSource = it.getStringExtra(Constant.KEY_IP_SOURCE_LIST);
        printLog2Screen("ip来源为: " + ipSource);

        String ipBlackStr = it.getStringExtra(Constant.KEY_BLACK_IP_LIST);
        printLog2Screen("默认ip 黑名单表达式: " + ipBlackStr);

        int maxIPForCdnCheck = it.getIntExtra(Constant.KEY_MAX_NUM_IP_FOR_CDN_CHECK, Constant.DEF_MAX_NUM_IP_FOR_CDN_CHECK);
        printLog2Screen("默认cdn ip 检查数量: " + maxIPForCdnCheck);

        String cdnHost = it.getStringExtra(Constant.KEY_TEST_CND_HOST);
        cdnHost = cdnHost == null ? Constant.DEF_TEST_CND_HOST : cdnHost;
        printLog2Screen("默认cdn 测试host: " + cdnHost);

        int maxThreadNumForCdnCheck = it.getIntExtra(Constant.KEY_MAX_NUM_THREAD_FOR_CDN_CHECK, Constant.DEF_MAX_NUM_THREAD_FOR_CDN_CHECK);
        printLog2Screen("cdn ip 检查并发线程数: " + maxThreadNumForCdnCheck);

        int maxIPForRttCheck = it.getIntExtra(Constant.KEY_MAX_NUM_IP_FOR_RTT_CHECK, Constant.DEF_MAX_NUM_IP_FOR_RTT_CHECK);
        printLog2Screen("默认rtt ip 检查数量: " + maxIPForRttCheck);

        int maxThreadNumForRttCheck = it.getIntExtra(Constant.KEY_MAX_NUM_THREAD_FOR_RTT_CHECK, Constant.DEF_MAX_NUM_THREAD_FOR_RTT_CHECK);
        printLog2Screen("默认rtt 检查并发线程数: " + maxThreadNumForRttCheck);

        String rttHost = it.getStringExtra(Constant.KEY_TEST_RTT_HOST);
        rttHost = rttHost == null ? Constant.DEF_TEST_RTT_HOST : rttHost;
        printLog2Screen("默认rtt 测试host: " + rttHost);

        int maxRetryForRtt = it.getIntExtra(Constant.KEY_MAX_NUM_RETRY_FOR_RTT_CHECK, Constant.DEF_MAX_NUM_RETRY_FOR_RTT_CHECK);
        printLog2Screen("默认rtt 检查重试次数: " + maxRetryForRtt);

        int maxPassValueForRtt = it.getIntExtra(Constant.KEY_MAX_VALUE_FOR_RTT_PASS, Constant.DEF_MAX_VALUE_FOR_RTT_PASS);
        printLog2Screen("默认rtt 通过阈值: " + maxPassValueForRtt);


        int maxIPForSpdCheck = it.getIntExtra(Constant.KEY_MAX_NUM_IP_FOR_SPEED_CHECK, Constant.DEF_MAX_NUM_IP_FOR_SPEED_CHECK);
        printLog2Screen("默认期望的测速ip数量: " + maxIPForSpdCheck);

        String spdLink = it.getStringExtra(Constant.KEY_TEST_SPD_LINK);
        spdLink = spdLink == null ? Constant.DEF_TEST_SPD_LINK : spdLink;
        printLog2Screen("默认测速地址: " + spdLink);

        String[] ipBlackList;
        if (ipBlackStr != null) {
            ipBlackList = ipBlackStr.split(",");
        } else {
            ipBlackList = new String[]{Constant.DEF_BLACK_IP_LIST};
        }
        int minPassValueForSpd = it.getIntExtra(Constant.KEY_MIN_VALUE_FOR_SPD_PASS, Constant.DEF_MIN_VALUE_FOR_SPD_PASS);
        printLog2Screen("默认测速通过阈值: " + minPassValueForSpd);

        int maxCountBetterIp = it.getIntExtra(Constant.KEY_MAX_BETTER_IP_COUNT, Constant.DEF_MAX_BETTER_IP_COUNT);
        printLog2Screen("默认最小Better 数量: " + maxCountBetterIp);
        printLog2Screen("测试配置读取完成\n");
        return new SpeedTestConfig(ipSource, maxIPForCdnCheck, maxThreadNumForCdnCheck,
                maxIPForRttCheck, maxThreadNumForRttCheck, maxRetryForRtt, maxPassValueForRtt,
                maxIPForSpdCheck, minPassValueForSpd, maxCountBetterIp, cdnHost, rttHost, spdLink,
                ipBlackList);
    }

    @Override
    public void onBackPressed() {
        mSpeedTestService.sendStopSignal();
        mHandler.removeCallbacks(mFinishTask);
        mHandler.postDelayed(mFinishTask, COMMON_DELAY);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSpeedTestService.sendStopSignal();
        mHandler.removeCallbacks(mFinishTask);
        mHandler.postDelayed(mFinishTask, COMMON_DELAY);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandlerThread.quitSafely();
        try {
            mHandlerThread.join(1000);
            mHandlerThread = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mHandler.removeCallbacks(mSpeedTestTask);
        mHandler.removeCallbacks(mFinishTask);
        mHandler = null;
        mSpeedTestService.unRegisterOnMessageListener();
        System.gc();
    }
}