package com.ironxiao.ipcheck;

public class Constant {
    // cdn ip 数量限制
    public static final String KEY_MAX_NUM_IP_FOR_CDN_CHECK = "max_num_for_cdn";
    public static final int DEF_MAX_NUM_IP_FOR_CDN_CHECK = 100;

    // cdn 测试线程数限制
    public static final String KEY_MAX_NUM_THREAD_FOR_CDN_CHECK = "max_num_for_cdn_thread";
    public static final int DEF_MAX_NUM_THREAD_FOR_CDN_CHECK = 100;
    // rtt ip 数量限制
    public static final String KEY_MAX_NUM_IP_FOR_RTT_CHECK = "max_num_for_rtt";
    public static final int DEF_MAX_NUM_IP_FOR_RTT_CHECK = 100;


    // rtt 测试线程限制
    public static final String KEY_MAX_NUM_THREAD_FOR_RTT_CHECK = "max_num_for_rtt_thread";
    public static final int DEF_MAX_NUM_THREAD_FOR_RTT_CHECK = 20;

    // 单个ip rtt 测试次数
    public static final String KEY_MAX_NUM_RETRY_FOR_RTT_CHECK = "max_num_for_rtt_retry";
    public static final int DEF_MAX_NUM_RETRY_FOR_RTT_CHECK = 10;

    // 测速ip 数量限制
    public static final String KEY_MAX_NUM_IP_FOR_SPEED_CHECK = "max_num_for_spd";
    public static final int DEF_MAX_NUM_IP_FOR_SPEED_CHECK = 100;

    // rtt 测试通过的最高平均延迟
    public static final String KEY_MAX_VALUE_FOR_RTT_PASS = "max_value_for_rtt";
    public static final int DEF_MAX_VALUE_FOR_RTT_PASS = 1000;

    // 测速通过的最低速度
    public static final String KEY_MIN_VALUE_FOR_SPD_PASS = "min_value_for_spd";
    public static final int DEF_MIN_VALUE_FOR_SPD_PASS = 12800;

    // ip 黑名单
    public static final String KEY_BLACK_IP_LIST = "black_ip_list";
    public static final String DEF_BLACK_IP_LIST = "na";

    // cdn 测试host
    public static final String KEY_TEST_CND_HOST = "cdn_host";
    public static final String DEF_TEST_CND_HOST = "icook.tw";

    // rtt test host
    public static final String KEY_TEST_RTT_HOST = "rtt_host";
    public static final String DEF_TEST_RTT_HOST = "www.cloudflare.com";

    // speed test host
    public static final String KEY_TEST_SPD_LINK = "spd_link";
    public static final String DEF_TEST_SPD_LINK = "cloudflaremirrors.com/archlinux/iso/latest/archlinux-x86_64.iso";


    // min amount better ip
    public static final String KEY_MAX_BETTER_IP_COUNT = "better_ip_count";
    public static final int DEF_MAX_BETTER_IP_COUNT = 1;

}
