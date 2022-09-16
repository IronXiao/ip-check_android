package com.ironxiao.ipcheck;

import java.util.concurrent.Callable;

public class SpeedCheckTask implements Callable<IPInfo> {
    private String ip;
    private long timeout;
    private String host;
    private String path;

    public SpeedCheckTask(String ip, long timeout, String host, String path) {
        this.ip = ip;
        this.timeout = timeout;
        this.host = host;
        this.path = path;
    }

    @Override
    public IPInfo call() {
        long speed = TestUtils.getSpeed(this.ip, this.timeout, this.host, this.path);
        return new IPInfo(this.ip, speed, null);
    }
}
