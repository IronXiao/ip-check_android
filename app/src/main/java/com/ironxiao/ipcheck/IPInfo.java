package com.ironxiao.ipcheck;

public class IPInfo {
    public String ip;
    public boolean isCdnIp;
    public int rtt = Integer.MAX_VALUE;
    public long speed = 0;
    public Object tag;

    public IPInfo(String ip, boolean isCdnIp) {
        this.ip = ip;
        this.isCdnIp = isCdnIp;
    }

    public IPInfo(String ip, int rtt) {
        this.ip = ip;
        this.rtt = rtt;
    }

    public IPInfo(String ip, long speed, Object tag) {
        this.ip = ip;
        this.speed = speed;
        this.tag = tag;
    }
}
