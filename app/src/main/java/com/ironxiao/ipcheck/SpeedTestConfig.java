package com.ironxiao.ipcheck;

public class SpeedTestConfig {
    private int maxIPForCdnCheck,
            maxThreadNumForCdnCheck,
            maxIPForRttCheck,
            maxThreadNumForRttCheck,
            maxRetryForRtt,
            maxPassValueForRtt,
            maxIPForSpdCheck,
            minPassValueForSpd,
            maxCountBetterIp;


    private String ipSource, cdnHost, rttHost, spdLink;

    private String[] ipBlackList;


    public SpeedTestConfig(String ipSource, int maxIPForCdnCheck, int maxThreadNumForCdnCheck, int maxIPForRttCheck,
                           int maxThreadNumForRttCheck, int maxRetryForRtt, int maxPassValueForRtt,
                           int maxIPForSpdCheck, int minPassValueForSpd, int maxCountBetterIp,
                           String cdnHost, String rttHost, String spdLink, String[] ipBlackList) {
        this.ipSource = ipSource;
        this.maxIPForCdnCheck = maxIPForCdnCheck;
        this.maxThreadNumForCdnCheck = maxThreadNumForCdnCheck;
        this.maxIPForRttCheck = maxIPForRttCheck;
        this.maxThreadNumForRttCheck = maxThreadNumForRttCheck;
        this.maxRetryForRtt = maxRetryForRtt;
        this.maxPassValueForRtt = maxPassValueForRtt;
        this.maxIPForSpdCheck = maxIPForSpdCheck;
        this.minPassValueForSpd = minPassValueForSpd;
        this.maxCountBetterIp = maxCountBetterIp;
        this.cdnHost = cdnHost;
        this.rttHost = rttHost;
        this.spdLink = spdLink;
        this.ipBlackList = ipBlackList;
    }

    public String getIpSource() {
        return ipSource;
    }

    public int getMaxIPForCdnCheck() {
        return maxIPForCdnCheck;
    }

    public int getMaxThreadNumForCdnCheck() {
        return maxThreadNumForCdnCheck;
    }

    public int getMaxIPForRttCheck() {
        return maxIPForRttCheck;
    }

    public int getMaxThreadNumForRttCheck() {
        return maxThreadNumForRttCheck;
    }

    public int getMaxRetryForRtt() {
        return maxRetryForRtt;
    }

    public int getMaxPassValueForRtt() {
        return maxPassValueForRtt;
    }

    public int getMaxIPForSpdCheck() {
        return maxIPForSpdCheck;
    }

    public int getMinPassValueForSpd() {
        return minPassValueForSpd;
    }

    public int getMaxCountBetterIp() {
        return maxCountBetterIp;
    }

    public String getCdnHost() {
        return cdnHost;
    }

    public String getRttHost() {
        return rttHost;
    }

    public String getSpdLink() {
        return spdLink;
    }

    public String[] getIpBlackList() {
        return ipBlackList;
    }
}
