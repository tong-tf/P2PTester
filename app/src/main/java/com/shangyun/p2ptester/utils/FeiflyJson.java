package com.shangyun.p2ptester.utils;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import static com.shangyun.p2ptester.utils.DataUtil.int4byte;

/*
            js.put("key", "PPCS-017130-FETFS");
            js.put("type", "video intercom");
            js.put("operation", "phone monitoring");
            js.put("paramter", "null");
            js.put("channels", "null");
            js.put("results", "null");
 */
public class FeiflyJson {
    private final static String TAG = "rwtest";
    private String key = "PPCS-017130-FETFS";
    private String type = "video intercom";
    private String operation = "phone monitoring";
    private String paramter = "null";
    private String channels = "null";
    private String results = "null";

    private FeiflyJson() {
    }

    private FeiflyJson(FeiflyJson other) {
        key = other.key;
        type = other.type;
        operation = other.operation;
        paramter = other.paramter;
        channels = other.channels;
        results = other.results;
    }

    public byte[] getData() {
        JSONObject js = new JSONObject();
        try {
            js.put("key", key);
            js.put("type", true);
            js.put("operation", operation);
            js.put("paramter", paramter);
            js.put("channels", channels);
            js.put("results", results);
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

    public String getKey() {
        return key;
    }

    public String getType() {
        return type;
    }

    public String getOperation() {
        return operation;
    }

    public String getParamter() {
        return paramter;
    }

    public String getChannels() {
        return channels;
    }

    public String getResults() {
        return results;
    }

    public static class Builder {
        private FeiflyJson target;

        public Builder() {
            target = new FeiflyJson();
        }

        public Builder setKey(String key) {
            target.key = key;
            return this;
        }

        public Builder setType(String type) {
            target.type = type;
            return this;
        }

        public Builder setOperation(String operation) {
            target.operation = operation;
            return this;
        }

        public Builder setParamter(String paramter) {
            target.paramter = paramter;
            return this;
        }

        public Builder setResults(String results) {
            target.results = results;
            return this;
        }

        public Builder setChannels(String channels) {
            target.channels = channels;
            return this;
        }

        public FeiflyJson build() {
            return new FeiflyJson(target);
        }
    }

}
