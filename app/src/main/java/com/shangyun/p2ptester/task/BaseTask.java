package com.shangyun.p2ptester.task;

import android.os.Handler;
import android.os.Message;

import com.p2p.pppp_api.PPCS_APIs;
import com.p2p.pppp_api.st_PPCS_NetInfo;

public class BaseTask {

    public static final int CODE_INFO_CONNECTING = 1;
    public static final int CODE_INFO_CONNECT_FAIL = 2;
    public static final int CODE_INFO_PPPP_CHECK_OK = 3;
    public static final int CODE_INFO_AV_ONLINENUM = 4;
    public static final int CODE_INFO_UPDATE_STAT = 5;
    public static final int MSG_LOG = 0;
    protected static final int ERROR_UnKnown = -99;
    protected static final int SERVER_NUM = 3;
    byte CH_CMD = 0;
    byte CH_DATA = 1;
    int UDP_Port = 0;
    Handler mHandler;
    String mDID = "";

    boolean isStop = false;

    public BaseTask(Handler handler, String did) {
        // TODO Auto-generated constructor stub
        mHandler = handler;
        mDID = did;
    }


    private byte[] intToByteArray_Little(int value) {
        return new byte[]{(byte) value, (byte) (value >>> 8), (byte) (value >>> 16), (byte) (value >>> 24)};
    }

    // get current time
    public long getTimes() {
        long gettime = System.currentTimeMillis();
        return gettime;
    }

    public int initAll(String strPara) {
        System.out.print("--- " + strPara);
        strPara = String.format(strPara + "\0");
        int nRet = PPCS_APIs.PPCS_Initialize(strPara.getBytes());
        System.out.println("--- init--- ret=" + nRet);
        return nRet;
    }

    public int deinitAll() {
        int nRet = PPCS_APIs.PPCS_DeInitialize();
        return nRet;
    }


    public void networkDetect() {
        st_PPCS_NetInfo NetInfo = new st_PPCS_NetInfo();
        System.out.println("---" + 1);
        PPCS_APIs.PPCS_NetworkDetect(NetInfo, 0);
        System.out.println("---" + 2);
        updateStatus("----------Start-NetInfo---------\n");
        String netInfo;
        netInfo = String.format("Internet Reachable      : %s\n", (NetInfo.getbFlagInternet() == 1) ? "YES" : "NO");
        System.out.println(netInfo);
        updateStatus(netInfo);

        netInfo = String.format("P2P Server IP resolved : %s\n", (NetInfo.getbFlagHostResolved() == 1) ? "YES" : "NO");
        System.out.println(netInfo);
        updateStatus(netInfo);

        netInfo = String.format("P2P Server Hello Ack    : %s\n", (NetInfo.getbFlagServerHello() == 1) ? "YES" : "NO");
        System.out.println(netInfo);
        updateStatus(netInfo);

        switch (NetInfo.getNAT_Type()) {
            case 0:
                netInfo = "Local NAT Type            : Unknow";
                break;
            case 1:
                netInfo = "Local NAT Type  : IP-Restricted Cone";
                break;
            case 2:
                netInfo = "Local NAT Type  : Port-Restricted Cone";
                break;
            case 3:
                netInfo = "Local NAT Type            : Symmetric";
                break;
        }
        System.out.println(netInfo);
        updateStatus(netInfo + "\n");

        updateStatus("My Wan IP :" + NetInfo.getMyWanIP() + "\n");
        System.out.println(NetInfo.getMyWanIP());

        updateStatus("My Lan   IP :" + NetInfo.getMyLanIP() + "\n");
        System.out.println(NetInfo.getMyLanIP());

        updateStatus("------------------------------\n");
    }

    int getMinNumFromLastLogin(int[] array) {
        if (array == null) {
            return ERROR_UnKnown;
        }
        int min = array[0];
        for (int i = 0; i < array.length; i++) {
            if (0 > array[i]) {
                if (min < array[i]) // min<0:-1,-99
                    min = array[i];// min:-1
            } else if (0 > min || min > array[i]) // LastLogin>=0, min: unknown
                min = array[i]; // min>=0
        }
        return min;
    }

    String GetStringItem(String srcStr, String itemName, String seperator) {

        String ret = null;

        if (srcStr == null || itemName == null || seperator == null)
            return ret;

        int index_item = srcStr.indexOf(itemName);
        if (index_item == -1) {
            return ret;
        }

        String src_item = srcStr.substring(index_item + itemName.length());

        int index_seperator = src_item.indexOf(seperator);
        if (index_seperator != -1) {
            ret = src_item.substring(1, index_seperator);
        }
        return ret;
    }


    // show connetion infos
    void updateStatus(String string) {
        Message msg = new Message();
        msg.what = MSG_LOG;
        msg.obj = string;
        mHandler.sendMessage(msg);
    }
}
