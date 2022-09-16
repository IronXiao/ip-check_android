package com.ironxiao.ipcheck;

import java.util.concurrent.Callable;

public class CdnIpCheckTask implements Callable<IPInfo> {

    private String ip;
    private String host;
    private int timeout;

    public CdnIpCheckTask(String ip, String host, int timeout) {
        this.ip = ip;
        this.host = host;
        this.timeout = timeout;
    }

    @Override
    public IPInfo call() {
        boolean isCdnIp = TestUtils.isValidCdnIp(this.ip, this.host, this.timeout);
        return new IPInfo(this.ip, isCdnIp);
    }
}
