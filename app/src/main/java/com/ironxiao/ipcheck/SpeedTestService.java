package com.ironxiao.ipcheck;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SpeedTestService {
    private static final String DEFAULT_IP_LIST_FILE = "ip.db";
    private static SpeedTestService sts;
    private Context mContext;
    private OnMessageListener mOnMessageListener;

    private SQLiteDatabase mIpDataBase;


    private SpeedTestConfig mConfig;
    List<Future<IPInfo>> mRunningTasks = new ArrayList<>();

    private boolean isRunning;
    private boolean stopped = true;

    public void setOnMessageListener(OnMessageListener onMessageListener) {
        this.mOnMessageListener = onMessageListener;
    }

    public void unRegisterOnMessageListener() {
        this.mOnMessageListener = null;
    }

    public void setConfig(SpeedTestConfig mConfig) {
        this.mConfig = mConfig;
    }

    private SpeedTestService(Context context) {
        this.mContext = context;
    }

    public static SpeedTestService getInstance(Context context) {
        synchronized (SpeedTestService.class) {
            if (sts == null) {
                sts = new SpeedTestService(context.getApplicationContext());
            }
            return sts;
        }
    }


    public interface OnMessageListener {
        void onMessage(String msg);
    }


    private SQLiteDatabase getIpDataBase() {
        transferMessage("\n检查加载ip 列表... ...");
        File ipDbFile = new File(mContext.getFilesDir(), DEFAULT_IP_LIST_FILE);
        if (!ipDbFile.exists()) {
            try {
                FileOutputStream out = new FileOutputStream(ipDbFile);
                InputStream in = mContext.getAssets().open(DEFAULT_IP_LIST_FILE);
                byte[] buffer = new byte[1024];
                int readBytes;
                while ((readBytes = in.read(buffer)) != -1)
                    out.write(buffer, 0, readBytes);
                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        transferMessage("ip 列表加载成功\n");
        return SQLiteDatabase.openOrCreateDatabase(ipDbFile, null);
    }

    private void transferMessage(String msg) {
        if (this.mOnMessageListener != null) {
            this.mOnMessageListener.onMessage(msg);
        }
    }

    // 将IP 黑名单转化为sql 支持的语句
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
        transferMessage("随机挑选" + count + "个ip中 ... ...");
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
        Cursor cursor = mIpDataBase.query("cf_ips", new String[]{"ip"}, selection,
                selectionArgs, null, null, "RANDOM() limit " + count);
        ArrayList<String> ips = new ArrayList<>();
        while (cursor.moveToNext()) {
            String item = cursor.getString(cursor.getColumnIndexOrThrow("ip"));
            ips.add(item);
        }
        cursor.close();
        long end = System.currentTimeMillis();
        transferMessage("挑选" + ips.size() + "个ip 耗时" + (end - start) + "ms");
        return ips;
    }

    public boolean allowStart() {
        return !isRunning && stopped;
    }


    public void start() throws ExecutionException, InterruptedException {
        if (mConfig == null) {
            throw new RuntimeException("Test config not set, please check!!!");
        }

        if (mOnMessageListener == null) {
            throw new RuntimeException("Test message callback not set, please check!!!");
        }

        if (isRunning) {
            throw new RuntimeException("new test should be started while no job running!");
        }

        if (!stopped) {
            throw new RuntimeException("last test not exit, please try again later!");
        }
        mIpDataBase = getIpDataBase();
        startInternal();
    }

    public void sendStopSignal() {
        transferMessage("正在准备退出测试...");
        this.isRunning = false;
        mIpDataBase.close();
        if (mRunningTasks.size() > 0) {
            for (Future<IPInfo> future : mRunningTasks) {
                future.cancel(false);
            }
        }
    }

    private void startInternal() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            isRunning = true;
            stopped = false;
            testInternal();
        });
        executorService.shutdown();
    }

    private void testInternal() {
        if (!isRunning) {
            transferMessage("停止测试 ... ... ... ...");
            stopped = true;
            return;
        }
        ArrayList<String> ipList = chooseIpFromDatabase(mConfig.getMaxIPForCdnCheck(),
                mConfig.getIpBlackList());
        try {
            ArrayList<String> cdnIpList = filterIpByCdn(ipList);
            if (cdnIpList.size() == 0) {
                transferMessage("没有筛选到ip 用于RTT测试, 重新优选！");
                testInternal();
                return;
            }
            cdnIpList = TestUtils.resizeIpListBySize(cdnIpList, mConfig.getMaxIPForRttCheck());
            ArrayList<String> rttIPList = filterIpByRtt(cdnIpList);
            if (rttIPList.size() == 0) {
                transferMessage("没有筛选到ip 用于速度测试, 重新优选！");
                testInternal();
                return;
            }
            rttIPList = TestUtils.resizeIpListBySize(rttIPList, mConfig.getMaxIPForSpdCheck());
            ArrayList<IPInfo> betterIpList = filterIpBySpeed(rttIPList);
            if (betterIpList.size() > 0) {
                for (IPInfo ipInfo : betterIpList) {
                    transferMessage(ipInfo.ip + "下载速度为" + ipInfo.speed + " kB/s");
                }

                if (betterIpList.size() >= mConfig.getMaxCountBetterIp()) {
                    //TODO completed
                    transferMessage("ip 优选完成！");
                    return;
                }
            }
            transferMessage("没有筛选到足够优选ip, 重新优选！");
            testInternal();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<String> filterIpByCdn(List<String> ipList) throws InterruptedException,
            ExecutionException {
        ArrayList<String> cdnIpList = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(mConfig.getMaxThreadNumForCdnCheck());
        CompletionService<IPInfo> cs = new ExecutorCompletionService<>(executorService);
        mRunningTasks.clear();
        for (String ip : ipList) {
            if (!isRunning) break;
            mRunningTasks.add(cs.submit(new CdnIpCheckTask(ip, mConfig.getCdnHost(), 3)));
        }
        executorService.shutdown();
        int totalTaskNum = mRunningTasks.size();
        transferMessage("正在进行cdn 检测, 总数为" + totalTaskNum);
        int finishedTaskNum = 0;
        Future<IPInfo> finishedTask;
        while (isRunning && finishedTaskNum < totalTaskNum && (finishedTask = cs.take()) != null) {
            try {
                IPInfo ipInfo = finishedTask.get();
                transferMessage(ipInfo.ip + "是否为cdn ip:" + ipInfo.isCdnIp);
                if (ipInfo.isCdnIp) {
                    cdnIpList.add(ipInfo.ip);
                }
            } catch (CancellationException e) {
            }
            finishedTaskNum++;
        }
        int cdnIpNum = cdnIpList.size();
        transferMessage("cdn ip 检测完毕，通过数为" + cdnIpNum + "/" + totalTaskNum);
        mRunningTasks.clear();
        return cdnIpList;
    }

    private ArrayList<String> filterIpByRtt(List<String> ipList) throws InterruptedException,
            ExecutionException {
        ArrayList<String> rttIpList = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(mConfig.getMaxThreadNumForRttCheck());
        CompletionService<IPInfo> cs = new ExecutorCompletionService<>(executorService);
        mRunningTasks.clear();
        for (String ip : ipList) {
            if (!isRunning) break;
            mRunningTasks.add(cs.submit(new RTTCheckTask(ip, mConfig.getRttHost(), mConfig.getMaxRetryForRtt())));
        }
        executorService.shutdown();
        int totalTaskNum = mRunningTasks.size();
        transferMessage("开始测试RTT, 数量为" + totalTaskNum);
        int finishedTaskNum = 0;
        Future<IPInfo> finishedTask;
        while (isRunning && finishedTaskNum < totalTaskNum && (finishedTask = cs.take()) != null) {
            try {
                IPInfo ipInfo = finishedTask.get();
                transferMessage(ipInfo.ip + " rtt " + ipInfo.rtt);
                if (ipInfo.rtt < mConfig.getMaxPassValueForRtt()) {
                    rttIpList.add(ipInfo.ip);
                }
            } catch (CancellationException e) {

            }
            finishedTaskNum++;
        }
        mRunningTasks.clear();
        int rttIpNum = rttIpList.size();
        transferMessage("ip rtt检测完毕，可用数为" + rttIpNum + "/" + totalTaskNum);
        return rttIpList;
    }

    private ArrayList<IPInfo> filterIpBySpeed(List<String> ipList) throws InterruptedException,
            ExecutionException, CancellationException {
        transferMessage("正在对ip 进行速度测试，数量为 " + ipList.size());
        ArrayList<IPInfo> betterIpList = new ArrayList<>();
        String spdLink = mConfig.getSpdLink();
        int index = spdLink.indexOf("/");
        String host = spdLink.substring(0, index);
        String file = spdLink.substring(index);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        CompletionService<IPInfo> cs = new ExecutorCompletionService<>(executorService);
        mRunningTasks.clear();
        for (String ip : ipList) {
            if (!isRunning) break;
            mRunningTasks.add(cs.submit(new SpeedCheckTask(ip, 10000, host, file)));
        }
        executorService.shutdown();
        int totalTaskNum = mRunningTasks.size();
        int finishedTaskNum = 0;
        Future<IPInfo> finishedTask;
        while (isRunning && finishedTaskNum < totalTaskNum && (finishedTask = cs.take()) != null) {
            try {
                IPInfo ipInfo = finishedTask.get();
                transferMessage(ipInfo.ip + "下载速度为" + ipInfo.speed + " kB/s");
                if (ipInfo.speed > mConfig.getMinPassValueForSpd()) {
                    betterIpList.add(ipInfo);
                }
            } catch (CancellationException e) {

            }
            finishedTaskNum++;
        }
        mRunningTasks.clear();
        transferMessage("测速结束，可用数为" + ipList.size() + "/" + betterIpList.size());
        return betterIpList;
    }

}
