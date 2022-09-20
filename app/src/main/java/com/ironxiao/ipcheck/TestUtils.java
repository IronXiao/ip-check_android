package com.ironxiao.ipcheck;

import android.net.TrafficStats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class TestUtils {
    // cdn must use http!!!
    private static final String CDN_URL = "http://%s/cdn-cgi/trace";
    // rtt use https!!!
    private static final String RTT_URL = "https://%s/cdn-cgi/trace";

    private static final String IP_REGEX = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";

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

    public static boolean downloadFile(String url, File saveFile) {
        boolean success = true;
        Response response = null;
        ResponseBody body = null;
        Source source = null;
        BufferedSink bs = null;
        try {
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(new RetryInterceptor(3))
                    .connectTimeout(3, TimeUnit.SECONDS).build();
            Request request = new Request.Builder().url(url).build();
            Call call = client.newCall(request);
            response = call.execute();
            int code = response.code();
            body = response.body();
            if (code == 200) {
                assert body != null;
                source = body.source();
                bs = Okio.buffer(Okio.sink(saveFile));
                while ((source.read(bs.buffer(), 1024)) != -1) {
                }
                bs.writeAll(source);
                bs.flush();
            }
        } catch (Exception e) {
            success = false;
        } finally {
            if (bs != null) {
                try {
                    bs.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (source != null) {
                try {
                    source.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (body != null)
                body.close();

            if (response != null)
                response.close();
        }
        return success;
    }


    public static ArrayList<String> readIpsFromZipFile(File zipFile) {
        HashSet<String> tmp = new HashSet<>();
        try {
            ZipFile zipSrc = new ZipFile(zipFile);
            Enumeration<? extends ZipEntry> srcEntries = zipSrc.entries();
            while (srcEntries.hasMoreElements()) {
                ZipEntry entry = srcEntries.nextElement();
                if (entry.getName().endsWith(".txt")) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(zipSrc.getInputStream(entry)));
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (isIp(line)) {
                            tmp.add(line);
                        }
                    }
                    br.close();
                }
            }
            zipSrc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ArrayList<String> ips = new ArrayList<>();
        Iterator<String> it = tmp.iterator();
        while (it.hasNext()) {
            ips.add(it.next());
        }
        return ips;
    }


    private static boolean isIp(String ipStr) {
        return ipStr != null && ipStr.matches(IP_REGEX);
    }
}
