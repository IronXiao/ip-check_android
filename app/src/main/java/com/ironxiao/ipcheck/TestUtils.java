package com.ironxiao.ipcheck;

import android.net.TrafficStats;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class TestUtils {
    // cdn must use http!!!
    private static final String CDN_URL = "http://%s/cdn-cgi/trace";
    // rtt use https!!!
    private static final String RTT_URL = "https://%s/cdn-cgi/trace";

    private static int getIntTag() {
        return 110;
    }

    public static boolean isValidCdnIp(String ip, String host, int timeout) {
        boolean isCdnIp = false;
        String url = String.format(CDN_URL, ip);
        String checkStr = "h=" + host;
        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(timeout, TimeUnit.SECONDS).addInterceptor(new RetryInterceptor(3)).readTimeout(timeout, TimeUnit.SECONDS).callTimeout(timeout, TimeUnit.SECONDS).build();
        Request request = new Request.Builder().url(url).addHeader("Host", host).build();
        TrafficStats.setThreadStatsTag(getIntTag());
        Response response = null;
        try {
            response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                assert response.body() != null;
                isCdnIp = response.body().string().contains(checkStr);
            }
            response.body().close();
            response.close();
        } catch (Exception e) {
            if (response != null) {
                response.body().close();
                response.close();
            }
        }
        return isCdnIp;
    }


    // host = www.cloudflare.com
    /*
     * 获取平均RTT
     */
    public static int getAvgRTT(String ip, String host, int maxRetry) {
        // 至少测试一次
        if (maxRetry < 0) maxRetry = 1;
        String url = String.format(RTT_URL, host);
        OkHttpClient client = new OkHttpClient.Builder().dns(new CustomDns(host, ip)).connectTimeout(3, TimeUnit.SECONDS).readTimeout(3, TimeUnit.SECONDS).callTimeout(3, TimeUnit.SECONDS).addInterceptor(new RetryInterceptor(3)).build();
        int retryCount = 0;
        long totalTime = 0;
        long start;
        Request request = new Request.Builder().url(url).addHeader("Host", host).build();
        TrafficStats.setThreadStatsTag(getIntTag());
        do {
            Response response = null;
            try {
                start = System.currentTimeMillis();
                response = client.newCall(request).execute();
                long end = System.currentTimeMillis();
                int code = response.code();
                response.body().close();
                response.close();
                if (code == 200) {
                    totalTime += end - start;
                } else {
                    return Integer.MAX_VALUE;
                }
            } catch (Exception e) {
                if (response != null) {
                    response.body().close();
                    response.close();
                }
                return Integer.MAX_VALUE;
            }
            retryCount++;
        } while (retryCount < maxRetry);
        return (int) (totalTime / maxRetry);
    }

    // ip should be like:
    // https://cloudflaremirrors.com/archlinux/iso/latest/archlinux-x86_64.iso
    public static long getSpeed(String ip, long timeout, String host, String path) {
        String url = "https://" + host + path;
        long speed = 0;
        Response response = null;
        ResponseBody body = null;
        InputStream source = null;
        try {
            OkHttpClient client = new OkHttpClient.Builder().dns(new CustomDns(host, ip)).addInterceptor(new RetryInterceptor(3)).connectTimeout(3, TimeUnit.SECONDS).build();
            Request request = new Request.Builder().url(url).build();
            TrafficStats.setThreadStatsTag(getIntTag());
            Call call = client.newCall(request);
            response = call.execute();
            int code = response.code();
            body = response.body();
            if (code == 200) {
                long start = System.currentTimeMillis();
                assert body != null;
                source = body.byteStream();
                byte[] bytes = new byte[1024];
                long end = System.currentTimeMillis();
                long totalRead = 0;
                long read;
                while ((read = source.read(bytes)) != -1 && end - start < timeout) {
                    totalRead += read;
                    end = System.currentTimeMillis();
                }
                speed = (totalRead * 1000) / ((end - start) * 1024);
            }
        } catch (Exception e) {
        } finally {
            if (source != null) {
                try {
                    source.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (body != null) body.close();

            if (response != null) response.close();
        }
        System.gc();
        return speed;
    }

    public static ArrayList<String> resizeIpListBySize(ArrayList<String> ipList, int size) {
        if (size < 1) size = 1;
        if (size >= ipList.size()) return ipList;
        ArrayList<String> result = new ArrayList<>();
        Random random = new Random();
        int n = 0;
        while (n < size) {
            int index = random.nextInt(ipList.size());
            String info = ipList.get(index);
            result.add(info);
            ipList.remove(index);
            n++;
        }
        return result;
    }
}
