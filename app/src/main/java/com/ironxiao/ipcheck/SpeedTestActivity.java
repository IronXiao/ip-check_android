package com.ironxiao.ipcheck;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;

import android.text.Selection;
import android.text.method.ScrollingMovementMethod;

import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Dns;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;

public class SpeedTestActivity extends Activity {
    private final String TAG = "XL_DBG";

    private SQLiteDatabase mSqlDb;
    private static final String DEFAULT_IP_LIST_FILE = "ip.db";

    private ArrayList<String> fullIpList = new ArrayList<>();
    private ArrayList<String> cdnIpList = new ArrayList<>();
    private ArrayList<String> rttIpList = new ArrayList<>();
    private ArrayList<IpInfo> betterIpList = new ArrayList<>();
    // must use http!!!
    private static final String CDN_URL = "http://%s/cdn-cgi/trace";
    // use https!!!
    private static final String RTT_URL = "https://%s/cdn-cgi/trace";
    private TextView logView;

    private ExecutorService cdnExecutor;
    private ExecutorService rttExecutor;


    private int maxIPForCdnCheck,
            maxThreadNumForCdnCheck,
            maxIPForRttCheck,
            maxThreadNumForRttCheck,
            maxRetryForRtt,
            maxPassValueForRtt,
            maxIPForSpdCheck,
            minPassValueForSpd,
            maxCountBetterIp;


    private String cdnHost, rttHost, spdLink;

    private String[] ipBlackList;

    private HandlerThread mHandlerThread;
    private TaskHandler mHandler;


    private static final int MSG_MAIN_TASK_START = 0;
    private static final int MSG_VALID_CHECK_TASK_FINISHED = 1;
    private static final int MSG_RTT_CHECK_TASK_FINISHED = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed_test);
        logView = findViewById(R.id.log);
        logView.setMovementMethod(ScrollingMovementMethod.getInstance());
        logView.setText("欢迎来到测试页面... ...");
        initValuesFromIntent();
        mSqlDb = getIpDataBase();
        mHandlerThread = new HandlerThread(SpeedTestActivity.class.getSimpleName());
        mHandlerThread.start();
        mHandler = new TaskHandler(mHandlerThread.getLooper(), new WeakReference<>(this));
        mHandler.sendEmptyMessageDelayed(MSG_MAIN_TASK_START, 500);
    }


    private static class TaskHandler extends Handler {
        private final WeakReference<SpeedTestActivity> activityWeakReference;

        public TaskHandler(@NonNull Looper looper, WeakReference<SpeedTestActivity> activityWeakReference) {
            super(looper);
            this.activityWeakReference = activityWeakReference;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            SpeedTestActivity activity = this.activityWeakReference.get();
            if (activity == null) return;
            switch (msg.what) {
                case MSG_MAIN_TASK_START:
                    activity.startTest();
                    break;
                case MSG_VALID_CHECK_TASK_FINISHED:
                    activity.filterIpByRtt();
                    break;
                case MSG_RTT_CHECK_TASK_FINISHED:
                    activity.filterIpBySpeed();
                    break;
                default:
                    break;
            }
        }
    }


    private void printLog2Screen(String msg) {
        logView.post(new Runnable() {
            @Override
            public void run() {
                synchronized (logView) {
                    logView.append("\n" + msg);
                    Editable editable = logView.getEditableText();
                    Selection.setSelection(editable, editable.length());
                }
            }
        });
    }


    private boolean isExecutorFinished(ThreadPoolExecutor executor) {
        return executor.getQueue().size() == 0 && executor.getActiveCount() == 0;
    }

    private void waitForTaskFinished(ThreadPoolExecutor executor) {
        while (!isExecutorFinished(executor)) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {

            }
        }
    }


    private void startTest() {
        fullIpList = chooseIpFromDatabase(maxIPForCdnCheck, ipBlackList);
        filterIpsByCdn();
    }

    private void initValuesFromIntent() {
        printLog2Screen("\n开始读取测试配置... ...");
        Intent it = getIntent();

        String ipBlackStr = it.getStringExtra(Constant.KEY_BLACK_IP_LIST);
        printLog2Screen("默认ip 黑名单表达式: " + ipBlackStr);

        maxIPForCdnCheck = it.getIntExtra(Constant.KEY_MAX_NUM_IP_FOR_CDN_CHECK, Constant.DEF_MAX_NUM_IP_FOR_CDN_CHECK);
        printLog2Screen("默认cdn ip 检查数量: " + maxIPForCdnCheck);

        cdnHost = it.getStringExtra(Constant.KEY_TEST_CND_HOST);
        cdnHost = cdnHost == null ? Constant.DEF_TEST_CND_HOST : cdnHost;
        printLog2Screen("默认cdn 测试host: " + cdnHost);

        maxThreadNumForCdnCheck = it.getIntExtra(Constant.KEY_MAX_NUM_THREAD_FOR_CDN_CHECK, Constant.DEF_MAX_NUM_THREAD_FOR_CDN_CHECK);
        printLog2Screen("cdn ip 检查并发线程数: " + maxThreadNumForCdnCheck);

        maxIPForRttCheck = it.getIntExtra(Constant.KEY_MAX_NUM_IP_FOR_RTT_CHECK, Constant.DEF_MAX_NUM_IP_FOR_RTT_CHECK);
        printLog2Screen("默认rtt ip 检查数量: " + maxIPForRttCheck);

        maxThreadNumForRttCheck = it.getIntExtra(Constant.KEY_MAX_NUM_THREAD_FOR_RTT_CHECK, Constant.DEF_MAX_NUM_THREAD_FOR_RTT_CHECK);
        printLog2Screen("默认rtt 检查并发线程数: " + maxThreadNumForRttCheck);

        rttHost = it.getStringExtra(Constant.KEY_TEST_RTT_HOST);
        rttHost = rttHost == null ? Constant.DEF_TEST_RTT_HOST : rttHost;
        printLog2Screen("默认rtt 测试host: " + rttHost);

        maxRetryForRtt = it.getIntExtra(Constant.KEY_MAX_NUM_RETRY_FOR_RTT_CHECK, Constant.DEF_MAX_NUM_RETRY_FOR_RTT_CHECK);
        printLog2Screen("默认rtt 检查重试次数: " + maxRetryForRtt);

        maxPassValueForRtt = it.getIntExtra(Constant.KEY_MAX_VALUE_FOR_RTT_PASS, Constant.DEF_MAX_VALUE_FOR_RTT_PASS);
        printLog2Screen("默认rtt 通过阈值: " + maxPassValueForRtt);


        maxIPForSpdCheck = it.getIntExtra(Constant.KEY_MAX_NUM_IP_FOR_SPEED_CHECK, Constant.DEF_MAX_NUM_IP_FOR_SPEED_CHECK);
        printLog2Screen("默认期望的测速ip数量: " + maxIPForSpdCheck);

        spdLink = it.getStringExtra(Constant.KEY_TEST_SPD_LINK);
        spdLink = spdLink == null ? Constant.DEF_TEST_SPD_LINK : spdLink;
        printLog2Screen("默认测速地址: " + spdLink);

        if (ipBlackStr != null) {
            ipBlackList = ipBlackStr.split(",");
        } else {
            ipBlackList = new String[]{Constant.DEF_BLACK_IP_LIST};
        }
        minPassValueForSpd = it.getIntExtra(Constant.KEY_MIN_VALUE_FOR_SPD_PASS, Constant.DEF_MIN_VALUE_FOR_SPD_PASS);
        printLog2Screen("默认测速通过阈值: " + minPassValueForSpd);

        maxCountBetterIp = it.getIntExtra(Constant.KEY_MAX_BETTER_IP_COUNT, Constant.DEF_MAX_BETTER_IP_COUNT);
        printLog2Screen("默认最小Better 数量: " + maxCountBetterIp);
        printLog2Screen("测试配置读取完成\n");
    }

    private synchronized void addIp2Cdn(String ip) {
        cdnIpList.add(ip);
    }


    private class CdnIpCheckTask implements Runnable {

        private final String ip;

        public CdnIpCheckTask(String ip) {
            this.ip = ip;
        }

        @Override
        public void run() {
            boolean result = isValidCdnIp(this.ip, cdnHost, 3);
            if (result)
                addIp2Cdn(this.ip);
        }
    }

    private synchronized void addIp2Rtt(String ip) {
        rttIpList.add(ip);
    }

    private class RttCheckTask implements Runnable {

        private final String ip;


        public RttCheckTask(String ip) {
            this.ip = ip;
        }

        @Override
        public void run() {
            int rtt = getAvgRTT(this.ip, rttHost, maxRetryForRtt);
            String msg = this.ip + " rtt is " + (rtt == Integer.MAX_VALUE ? "invalid" : rtt);
            printLog2Screen(msg);
            if (rtt < maxPassValueForRtt)
                addIp2Rtt(ip);
        }
    }

    private synchronized void removeItemInIpList(String ip) {
        boolean success = fullIpList.remove(ip);
        Log.d(TAG, "removeItemInIpList returns " + success + " for " + ip);
    }

    private void filterIpsByCdn() {
        printLog2Screen("正在进行cdn 检测... ...");
        cdnIpList.clear();
        cdnExecutor = Executors.newFixedThreadPool(maxThreadNumForCdnCheck);
        int beforeNum = fullIpList.size();
        for (String ip : fullIpList) {
            CdnIpCheckTask t = new CdnIpCheckTask(ip);
            cdnExecutor.submit(t);
        }
        cdnExecutor.shutdown();
        waitForTaskFinished((ThreadPoolExecutor) cdnExecutor);
        int afterNum = cdnIpList.size();
        printLog2Screen("cdn ip 检测完毕，通过数为" + afterNum + "/" + beforeNum);
        mHandler.sendEmptyMessageDelayed(MSG_VALID_CHECK_TASK_FINISHED, 1000);
    }

    private void filterIpByRtt() {
        cdnIpList = chooseIpsByCount(cdnIpList, maxIPForRttCheck);
        if (cdnIpList.size() == 0) {
            printLog2Screen("没有筛选到ip 用于RTT 测试, 重新优选！");
            mHandler.sendEmptyMessageDelayed(MSG_MAIN_TASK_START, 500);
            return;
        }
        rttIpList.clear();
        int beforeNum = cdnIpList.size();
        printLog2Screen("开始测试RTT, 数量为" + beforeNum);
        rttExecutor = Executors.newFixedThreadPool(maxThreadNumForRttCheck);
        for (String ip : cdnIpList) {
            RttCheckTask t = new RttCheckTask(ip);
            rttExecutor.submit(t);
        }
        rttExecutor.shutdown();
        waitForTaskFinished((ThreadPoolExecutor) rttExecutor);
        int afterNum = rttIpList.size();
        printLog2Screen("ip rtt检测完毕，可用数为" + afterNum + "/" + beforeNum);
        mHandler.sendEmptyMessage(MSG_RTT_CHECK_TASK_FINISHED);
    }

    private void filterIpBySpeed() {
        if (rttIpList.size() == 0) {
            printLog2Screen("没有筛选到ip 用于速度测试, 重新优选！");
            mHandler.sendEmptyMessageDelayed(MSG_MAIN_TASK_START, 500);
            return;
        }
        printLog2Screen("正在对ip 进行速度测试，数量为 " + rttIpList.size());
        betterIpList.clear();
        int index = spdLink.indexOf("/");
        String host = spdLink.substring(0, index);
        String file = spdLink.substring(index);
        for (String ip : rttIpList) {
            long speed = getSpeed(ip, 10000, host, file);
            if (speed > minPassValueForSpd) {
                betterIpList.add(new IpInfo(ip, speed));
                if (betterIpList.size() >= maxCountBetterIp) {
                    break;
                }
            }
        }
        if (betterIpList.size() == 0) {
            printLog2Screen("没有筛选到优质ip, 重试... ...");
            mHandler.sendEmptyMessageDelayed(MSG_MAIN_TASK_START, 500);
            return;
        }
        printLog2Screen("优选ip 完成:");
        for (IpInfo ipInfo : betterIpList) {
            printLog2Screen(ipInfo.toString());
        }
    }

    private ArrayList<String> chooseIpsByCount(ArrayList<String> infos, int size) {
        if (size < 1) size = 1;
        if (size >= infos.size()) return infos;
        ArrayList<String> result = new ArrayList<>();
        Random random = new Random();
        int n = 0;
        while (n < size) {
            int index = random.nextInt(infos.size());
            String info = infos.get(index);
            result.add(info);
            infos.remove(index);
            n++;
        }
        return result;
    }


    private boolean isValidCdnIp(String ip, String host, int timeout) {
        String url = String.format(CDN_URL, ip);
        String checkStr = "h=" + host;
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .addInterceptor(new RetryIntercepter(3))
                .readTimeout(timeout, TimeUnit.SECONDS)
                .callTimeout(timeout, TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Host", host)
                .build();
        TrafficStats.setThreadStatsTag(ip2int(ip));
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                return response.body().string().contains(checkStr);
            }
        } catch (Exception e) {
        }
        return false;
    }

    // host = www.cloudflare.com
    /*
     * 获取平均RTT
     */
    private int getAvgRTT(String ip, String host, int retryCount) {
        // 至少测试一次
        if (retryCount < 0)
            retryCount = 1;
        String url = String.format(RTT_URL, host);
        OkHttpClient client = new OkHttpClient.Builder().dns(new CustomDns(host, ip))
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .callTimeout(3, TimeUnit.SECONDS)
                .addInterceptor(new RetryIntercepter(3))
                .build();
        int time = 0;
        long totalTime = 0;
        long start;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Host", host)
                .build();
        TrafficStats.setThreadStatsTag(ip2int(ip));
        Response response = null;
        do {
            try {
                start = System.currentTimeMillis();
                response = client.newCall(request).execute();
                int code = response.code();
                response.close();
                if (code == 200) {
                    totalTime += System.currentTimeMillis() - start;
                } else {
                    return Integer.MAX_VALUE;
                }
            } catch (Exception e) {
//                if (response != null) {
//                    response.close();
//                }
                return Integer.MAX_VALUE;
            } finally {
//                if (response == null) {
//                    response.close();
//                }
            }
            time++;
        } while (time < retryCount);
        return (int) (totalTime / retryCount);
    }

    private int ip2int(String ip) {
        String[] nums = ip.split("[.]");
        StringBuilder intStr = new StringBuilder();
        for (String s : nums) {
            intStr.append(s);
        }
        return Integer.parseInt(intStr.toString());
    }

    // ip should be like:
    // https://cloudflaremirrors.com/archlinux/iso/latest/archlinux-x86_64.iso
    private long getSpeed(String ip, long timeout, String host, String path) {
        String url = "https://" + host + path;
        long speed = 0;
        try {
            OkHttpClient client = new OkHttpClient.Builder().
                    dns(new CustomDns(host, ip))
                    .addInterceptor(new RetryIntercepter(3))
                    .connectTimeout(3, TimeUnit.SECONDS)
                    .build();
            Request request = new Request.Builder().url(url).build();
            TrafficStats.setThreadStatsTag(ip2int(ip));
            Call call = client.newCall(request);
            long start = System.currentTimeMillis();
            Response response = call.execute();
            int code = response.code();
            if (code == 200) {
                ResponseBody body = response.body();
                BufferedSource source = body.source();
                byte[] bytes = new byte[1024];
                long end = System.currentTimeMillis();
                long totalRead = 0;
                long read = 0;
                while ((read = source.read(bytes)) != -1 && end - start < timeout) {
                    totalRead += read;
                    end = System.currentTimeMillis();
                }
                speed = (totalRead * 1000) / ((end - start) * 1024);
                printLog2Screen(ip + " 下载速度: " + speed + " kB/s");
                source.close();
                response.close();
            }
        } catch (Exception e) {

        }
        return speed;

    }

    // 把指定域名解析到指定ip
    private class CustomDns implements Dns {
        private String toIp;
        private String host;

        public CustomDns(String host, String toIp) {
            super();
            this.host = host;
            this.toIp = toIp;
        }

        private byte[] convertIp2bytes(String ip) {
            String[] elements = ip.split("[.]");
            byte[] ipbytes = new byte[4];
            for (int i = 0; i < elements.length; i++) {
                ipbytes[i] = (byte) Integer.parseInt(elements[i]);
            }
            return ipbytes;
        }

        @Override
        public List<InetAddress> lookup(String hostname) throws UnknownHostException {
            if (this.host.equals(hostname)) {
                InetAddress byAddress = InetAddress.getByAddress(hostname, convertIp2bytes(this.toIp));
                return Collections.singletonList(byAddress);
            } else {
                return SYSTEM.lookup(hostname);
            }
        }
    }

    private static class IpInfo {
        // ip
        public String ip;
        // 测试下载测试文件的网速，单位kB/s
        public long speed = 0;

        public IpInfo(String ip, long speed) {
            this.ip = ip;
            this.speed = speed;
        }

        @Override
        public String toString() {
            return this.ip + " speed is " + this.speed + " kB/s";
        }

    }

    public SQLiteDatabase getIpDataBase() {
        printLog2Screen("\n检查加载ip 列表... ...");
        File ipDbFile = new File(getFilesDir(), DEFAULT_IP_LIST_FILE);
        if (!ipDbFile.exists()) {
            try {
                FileOutputStream out = new FileOutputStream(ipDbFile);
                InputStream in = getAssets().open(DEFAULT_IP_LIST_FILE);
                byte[] buffer = new byte[1024];
                int readBytes = 0;
                while ((readBytes = in.read(buffer)) != -1)
                    out.write(buffer, 0, readBytes);
                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        printLog2Screen("ip 列表加载成功\n");
        return SQLiteDatabase.openOrCreateDatabase(ipDbFile, null);
    }


    private String[] convertFilter(String[] filter) {
        String[] result = new String[filter.length];
        for (int i = 0; i < filter.length; i++) {
            result[i] = filter[i].endsWith("%") ? filter[i] : filter[i] + "%";
        }
        return result;
    }

    // filter should be like {"104%", "107%"}
    private ArrayList<String> chooseIpFromDatabase(int count, String[] blackFilter) {
        // 至少筛选1个ip
        if (count < 1) count = 1;
        printLog2Screen("随机挑选" + count + "个ip中 ... ...");
        long start = System.currentTimeMillis();
        String selection = null;
        String[] selectionArgs = null;
        if (blackFilter != null && blackFilter.length > 0) {
            selectionArgs = convertFilter(blackFilter);
            selection = "ip not like ?";
            int n = blackFilter.length - 1;
            while (n > 0) {
                selection += " and ip not like ?";
                n--;
            }
        }
        Cursor cursor = mSqlDb.query("cf_ips", new String[]{"ip"}, selection, selectionArgs, null, null, "RANDOM() limit " + count);
        ArrayList<String> ips = new ArrayList<>();
        while (cursor.moveToNext()) {
            String item = cursor.getString(cursor.getColumnIndexOrThrow("ip"));
            ips.add(item);
        }
        cursor.close();
        long end = System.currentTimeMillis();
        printLog2Screen("挑选" + ips.size() + "个ip 耗时" + (end - start) + "ms");
        return ips;
    }

    void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(0, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(0, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (cdnExecutor != null) {
            shutdownAndAwaitTermination(cdnExecutor);
        }
        if (rttExecutor != null) {
            shutdownAndAwaitTermination(rttExecutor);
        }

        mHandler.removeMessages(MSG_MAIN_TASK_START);
        mHandler.removeMessages(MSG_VALID_CHECK_TASK_FINISHED);
        mHandler.removeMessages(MSG_RTT_CHECK_TASK_FINISHED);
        mHandlerThread.quit();
        mHandlerThread = null;
        mSqlDb.close();
    }


}