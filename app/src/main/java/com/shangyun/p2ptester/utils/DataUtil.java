package com.shangyun.p2ptester.utils;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class DataUtil {
    private final static String TAG = "rwtest";

    /**
     * 将一个整数转换成4个字节的字符序列   12354 ->  byte[] = {'4', '3', '2', '1'}
     *
     * @param v 要转换的整数
     * @return 字节表示方法
     */
    public static byte[] int4byte(int v) {
        byte[] rv = new byte[4];
        for (int i = 3; i >= 0; i--) {
            rv[i] = (byte) (v % 10 + '0');
            v /= 10;
        }
        return rv;
    }

    /**
     * 将byte数组转换成整数
     *
     * @param b 要转换的数组，长度大于4
     * @return 转换后的整数，
     */
    public static int byte2int(byte[] b) {
        if (b.length < 0) {
            return 0;
        } else {
            return (b[0] & 0xff) << 24 | (b[1] & 0xff) << 16 | (b[2] & 0xff) << 8 | (b[3] & 0xff);
        }
    }

    /**
     * ‘1234’ -> 1234
     *
     * @param b 长度为4个的字节
     * @return
     */
    public static int byte4int(byte[] b) {
        int rv = 0, v = 0;
        return 1000 * (b[0] - '0') + 100 * (b[1] - '0') + 10 * (b[2] - '0') + (b[3] - '0');
    }

    public static byte[] buildData() {
        JSONObject js = new JSONObject();
        try {
            js.put("key", "PPCS-017130-FETFS");
            js.put("type", "video intercom");
            js.put("operation", "phone monitoring");
            js.put("paramter", "null");
            js.put("channels", "null");
            js.put("results", "null");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "jobject " + js.toString());
        byte buffer1[] = js.toString().getBytes();
        int len = buffer1.length + 8;
        byte buffer[] = new byte[len];
        byte[] out = int4byte(buffer1.length);
        for (int i = 0; i < 4; i++) {
            buffer[2 + i] = out[i];
        }
        buffer[0] = '#';
        buffer[1] = '#';
        buffer[6] = '$';
        buffer[7] = '$';
        System.arraycopy(buffer1, 0, buffer, 8, buffer1.length);
        return buffer;
    }


}
