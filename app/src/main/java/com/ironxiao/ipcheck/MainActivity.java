package com.ironxiao.ipcheck;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends Activity {
    private EditText blkList, cdnIpNum, cdnHost, cdnTdNum, rttIpNum, rttHost, rttTdNum, rttRetry,
            rttPassValue, spdIpNum, spdLink, spdPassValue, btNum;

    private int maxIPForCdnCheck,
            maxThreadNumForCdnCheck,
            maxIPForRttCheck,
            maxThreadNumForRttCheck,
            maxRetryForRtt,
            maxPassValueForRtt,
            maxIPForSpdCheck,
            minPassValueForSpd,
            maxCountBetterIp;


    private String cdnHostStr, rttHostStr, spdLinkStr, ipBlackListStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
        initValuesFromDefaults();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    private void findViews() {
        blkList = findViewById(R.id.et_ip_blk);
        cdnIpNum = findViewById(R.id.et_cdn_num);
        cdnHost = findViewById(R.id.et_cdn_host);
        cdnTdNum = findViewById(R.id.et_cdn_td_num);
        rttIpNum = findViewById(R.id.et_rtt_num);
        rttHost = findViewById(R.id.et_rtt_host);
        rttTdNum = findViewById(R.id.et_rtt_td_num);
        rttRetry = findViewById(R.id.et_rtt_try);
        rttPassValue = findViewById(R.id.et_rtt_ok_value);
        spdIpNum = findViewById(R.id.et_bt_ip_num);
        spdLink = findViewById(R.id.et_spd_link);
        spdPassValue = findViewById(R.id.et_spd_pass_value);
        btNum = findViewById(R.id.et_min_bt_num);
    }


    private void initValuesFromDefaults() {
        ipBlackListStr = Constant.DEF_BLACK_IP_LIST;
        blkList.setText(ipBlackListStr);
        maxIPForCdnCheck = Constant.DEF_MAX_NUM_IP_FOR_CDN_CHECK;
        cdnIpNum.setText(String.valueOf(maxIPForCdnCheck));
        cdnHostStr = Constant.DEF_TEST_CND_HOST;
        cdnHost.setText(cdnHostStr);
        maxThreadNumForCdnCheck = Constant.DEF_MAX_NUM_THREAD_FOR_CDN_CHECK;
        cdnTdNum.setText(String.valueOf(maxThreadNumForCdnCheck));


        maxIPForRttCheck = Constant.DEF_MAX_NUM_IP_FOR_RTT_CHECK;
        rttIpNum.setText(String.valueOf(maxIPForRttCheck));
        rttHostStr = Constant.DEF_TEST_RTT_HOST;
        rttHost.setText(rttHostStr);
        maxThreadNumForRttCheck = Constant.DEF_MAX_NUM_THREAD_FOR_RTT_CHECK;
        rttTdNum.setText(String.valueOf(maxThreadNumForRttCheck));
        maxRetryForRtt = Constant.DEF_MAX_NUM_RETRY_FOR_RTT_CHECK;
        rttRetry.setText(String.valueOf(maxRetryForRtt));
        maxPassValueForRtt = Constant.DEF_MAX_VALUE_FOR_RTT_PASS;
        rttPassValue.setText(String.valueOf(maxPassValueForRtt));


        maxIPForSpdCheck = Constant.DEF_MAX_NUM_IP_FOR_SPEED_CHECK;
        spdIpNum.setText(String.valueOf(maxIPForSpdCheck));
        spdLinkStr = Constant.DEF_TEST_SPD_LINK;
        spdLink.setText(spdLinkStr);
        minPassValueForSpd = Constant.DEF_MIN_VALUE_FOR_SPD_PASS;
        spdPassValue.setText(String.valueOf(minPassValueForSpd));
        maxCountBetterIp = Constant.DEF_MAX_BETTER_IP_COUNT;
        btNum.setText(String.valueOf(maxCountBetterIp));
    }

    private void readInfo() {
        ipBlackListStr = blkList.getText().toString();
        cdnHostStr = cdnHost.getText().toString();
        rttHostStr = rttHost.getText().toString();
        spdLinkStr = spdLink.getText().toString();
        try {
            maxIPForCdnCheck = Integer.parseInt(cdnIpNum.getText().toString());
            maxThreadNumForCdnCheck = Integer.parseInt(cdnTdNum.getText().toString());
            maxIPForRttCheck = Integer.parseInt(rttIpNum.getText().toString());
            maxThreadNumForRttCheck = Integer.parseInt(rttTdNum.getText().toString());
            maxRetryForRtt = Integer.parseInt(rttRetry.getText().toString());
            maxPassValueForRtt = Integer.parseInt(rttPassValue.getText().toString());
            maxIPForSpdCheck = Integer.parseInt(spdIpNum.getText().toString());
            minPassValueForSpd = Integer.parseInt(spdPassValue.getText().toString());
            maxCountBetterIp = Integer.parseInt(btNum.getText().toString());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    public void onButtonClick(View view) {
        startActivity(buildIntent());
    }


    private Intent buildIntent() {
        readInfo();
        Intent it = new Intent(this, SpeedTestActivity.class);
        it.putExtra(Constant.KEY_BLACK_IP_LIST, ipBlackListStr);
        it.putExtra(Constant.KEY_MAX_NUM_IP_FOR_CDN_CHECK, maxIPForCdnCheck);
        it.putExtra(Constant.KEY_TEST_CND_HOST, cdnHostStr);
        it.putExtra(Constant.KEY_MAX_NUM_THREAD_FOR_CDN_CHECK, maxThreadNumForCdnCheck);

        it.putExtra(Constant.KEY_MAX_NUM_IP_FOR_RTT_CHECK, maxIPForRttCheck);
        it.putExtra(Constant.KEY_TEST_RTT_HOST, rttHostStr);
        it.putExtra(Constant.KEY_MAX_NUM_THREAD_FOR_RTT_CHECK, maxThreadNumForRttCheck);
        it.putExtra(Constant.KEY_MAX_NUM_RETRY_FOR_RTT_CHECK, maxRetryForRtt);
        it.putExtra(Constant.KEY_MAX_VALUE_FOR_RTT_PASS, maxPassValueForRtt);

        it.putExtra(Constant.KEY_MAX_NUM_IP_FOR_SPEED_CHECK, maxIPForSpdCheck);
        it.putExtra(Constant.KEY_TEST_SPD_LINK, spdLinkStr);
        it.putExtra(Constant.KEY_MIN_VALUE_FOR_SPD_PASS, minPassValueForSpd);
        it.putExtra(Constant.KEY_MAX_BETTER_IP_COUNT, maxCountBetterIp);

        return it;
    }

}