package com.ironxiao.ipcheck;

import java.util.concurrent.Callable;

public class RTTCheckTask implements Callable<IPInfo> {
    private String ip;
    private String host;
    private int maxRetry;

    public RTTCheckTask(String ip, String host, int maxRetry) {
        this.ip = ip;
        this.host = host;
        this.maxRetry = maxRetry;
    }

    @Override
    public IPInfo call() {
        int rtt = TestUtils.getAvgRTT(this.ip, this.host, this.maxRetry);
        return new IPInfo(this.ip, rtt);
    }
}
