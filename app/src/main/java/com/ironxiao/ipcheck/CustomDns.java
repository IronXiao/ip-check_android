package com.ironxiao.ipcheck;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

import okhttp3.Dns;

public class CustomDns implements Dns {
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