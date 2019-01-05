package com.shangyun.p2ptester;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.p2p.pppp_api.PPCS_APIs;
import com.shangyun.p2ptester.task.BaseTask;
import com.shangyun.p2ptester.task.ConnectTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


public class ConnectionFragment extends Fragment implements OnClickListener {
    public static final String TAG = "";
    public static final String INIT_PRE_NAME = "INIT_NAME";
    public static final String INIT_PRE_KEY = "INIT_KEY";
    private static final String HOST = "";
    private static final int PORT = 12305;
    private final String DATA_NAME = "Connection_data";
    private final String DID = "did";
    private final String Mode = "mode";
    private final String INIT_STRING = "init_string";
    private final String WAKEUP = "wakeup";
    private final String IP1 = "ip";
    private final String IP2 = "ip2";
    private final String IP3 = "ip3";
    private Socket socket = null;
    private String did;
    private String initString;
    private String detimes;
    private String wakeupkey;
    private String ip1;
    private String ip2;
    private String ip3;
    private Handler handler;
    private View parent;
    private EditText editDID;
    private EditText editInitstring;
    private EditText editCount;
    private EditText editDelaySec;
    private EditText editWakeupkey;
    private EditText editip1;
    private EditText editip2;
    private EditText editip3;
    private Spinner spinnerMode;
    private ArrayAdapter adapterMode;
    private ViewGroup editLayout;
    private TextView logText;
    private ScrollView scrollView;
    private ImageButton popupBtn;
    private Button wakeupTestBtn;
    private Button loginBtn;
    private LinearLayout layout_base;
    private LinearLayout layout_wakeup;
    private TextView tx_base;
    private TextView tx_wakeup;
    private boolean isConnecting;
    private ConnectTask mConnectTask;
    private List<AccountObject> accountList;
    private MyPopupWindows mPopupWindows;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        parent = inflater.inflate(R.layout.fragment_connectiontester, null);
        initViews();
        initDatas();
        showApi();
        return parent;
    }

    private void showApi() {
        // API version
        String apiver;
        int n = PPCS_APIs.ms_verAPI;
        apiver = String.format("API ver: %d.%d.%d.%d\n", (n >> 24) & 0xff, (n >> 16) & 0xff, (n >> 8) & 0xff, n & 0xff);
        log(apiver);
        initHandler();
    }

    private byte smallToByte(String small) {
        byte v = 0;

        int i = Integer.valueOf(small, 16).byteValue();

        v = (byte) i;

        return v;
    }

    private void log(String msg) {
        Log.i(TAG, msg);

        logText.append(msg);
        scrollView.fullScroll(View.FOCUS_DOWN);
    }

    private void initHandler() {
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case BaseTask.MSG_LOG:
                        String text = (String) msg.obj;
                        log(text);
                        break;
                    default:
                        ;
                }
            }
        };
    }

    public void initViews() {
        loginBtn = (Button) parent.findViewById(R.id.login_btn);
        editDID = (EditText) parent.findViewById(R.id.didEdit);
        spinnerMode = (Spinner) parent.findViewById(R.id.spinnerMode);
        editInitstring = (EditText) parent.findViewById(R.id.initEdit);
        editCount = (EditText) parent.findViewById(R.id.initCount);
        logText = (TextView) parent.findViewById(R.id.logText);

        editDelaySec = (EditText) parent.findViewById(R.id.initDelay);

        editWakeupkey = (EditText) parent.findViewById(R.id.editWakeupKey);
        editip1 = (EditText) parent.findViewById(R.id.editip1);
        editip2 = (EditText) parent.findViewById(R.id.editip2);
        editip3 = (EditText) parent.findViewById(R.id.editip3);

        layout_base = (LinearLayout) parent.findViewById(R.id.layout_base);
        layout_wakeup = (LinearLayout) parent.findViewById(R.id.layout_wakeup);

        tx_base = (TextView) parent.findViewById(R.id.tx_base);
        tx_wakeup = (TextView) parent.findViewById(R.id.tx_wakeup);
        tx_base.setOnClickListener(this);
        tx_wakeup.setOnClickListener(this);

        scrollView = (ScrollView) parent.findViewById(R.id.scrollView1);
        popupBtn = (ImageButton) parent.findViewById(R.id.popup_btn);
        editLayout = (ViewGroup) parent.findViewById(R.id.edit_layout);

        loginBtn.setOnClickListener(this);
        popupBtn.setOnClickListener(this);
    }

    public void initDatas() {
        accountList = getAccountList();
        if (accountList == null || accountList.isEmpty()) {
            popupBtn.setEnabled(false);
        } else {
            popupBtn.setEnabled(true);
        }
    }

    // Multiple connections
    public void connectCount() {

        did = editDID.getText().toString().trim();
        initString = editInitstring.getText().toString().trim();

        int icount = 1;
        String scount = editCount.getText().toString();
        if (scount.length() > 0) {
            icount = Integer.parseInt(scount);
        }

        if (icount < 1) {
            icount = 1;
            editCount.setText("1");
        }
        System.out.println("icount:" + icount);

        int idetimes = 3;
        detimes = editDelaySec.getText().toString().trim();
        if (detimes.length() > 0) {
            idetimes = Integer.parseInt(detimes);
        }


        if (did.equals("") || initString.equals("")) {
            Toast.makeText(getActivity(), "请输入正确的信息！", 0).show();
            return;
        }

        boolean testWakewp = false;

        wakeupkey = editWakeupkey.getText().toString().trim();
        ip1 = editip1.getText().toString().trim();
        ip2 = editip2.getText().toString().trim();
        ip3 = editip3.getText().toString().trim();


        if (wakeupkey.length() < 1) {
            if (ip1.length() > 1 || ip2.length() > 1 || ip3.length() > 1) {
                Toast.makeText(getActivity(), "请输入唤醒Key！", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            if (ip1.length() < 1 && ip2.length() < 1 && ip3.length() < 1) {
                Toast.makeText(getActivity(), "请输入至少一个IP！", Toast.LENGTH_SHORT).show();
                return;
            } else {
                testWakewp = true;
            }
        }
        if (testWakewp) {
            if (wakeupkey.length() < 16) {
                Toast.makeText(getActivity(), "WakeupKey位16位！请检查重新输入！", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        logText.setText("");
        showApi();

        isConnecting = true;
        loginBtn.setText("Stop");


        //P2PDev.deinitAll();
        //P2PDev.initAll(initString);
        int index = spinnerMode.getSelectedItemPosition();
        byte mode = 0;
        switch (index) {
            case 0:
                mode = 0;
                break;
            case 1:
                mode = 1;
                break;
            case 2:
                mode = 30;
                break;
            case 3:
                mode = 31;
                break;
            case 4:
                mode = 4;
                break;
            case 5:
                mode = 0x7E;
                break;
            case 6:
                mode = 94;
                break;
            case 7:
                mode = 7;
                break;
            default:
                break;
        }

        if (testWakewp) {
            List<String> ips = new ArrayList<String>();
            if (ip1.length() > 0) {
                ips.add(ip1);
            }
            if (ip2.length() > 0) {
                ips.add(ip2);
            }
            if (ip3.length() > 0) {
                ips.add(ip3);
            }
            mConnectTask = new ConnectTask(handler, did, mode, icount, idetimes, wakeupkey, ips);
        } else {
            mConnectTask = new ConnectTask(handler, did, mode, icount, idetimes);
        }

        if (ListenFragment.isListening) {
            //compare with saved initString, prompt if different
            SharedPreferences preferences = getActivity().getSharedPreferences(ListenFragment.INIT_PRE_NAME, Context.MODE_PRIVATE);
            String save_init = preferences.getString(ListenFragment.INIT_PRE_KEY, "");
            if (!initString.equalsIgnoreCase(save_init)) {
                Toast.makeText(getActivity(), "InitString与listen界面不一致", 0).show();
                return;
            }
        } else {
            mConnectTask.deinitAll();
            mConnectTask.initAll(initString);
        }

        new Thread(new Runnable() {

            @Override
            public void run() {
                mConnectTask.connectDevCount();
                isConnecting = false;
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        loginBtn.setText("Start");
                    }
                });
            }
        }).start();
        ;


        // add datas
        addNewAccountObject(did, initString, wakeupkey, ip1, ip2, ip3);
        // save datas to local
        saveAccountList(accountList);

    }

    public void showPopup() {
        accountList = getAccountList();
        if (accountList == null || accountList.size() < 1) {
            return;
        }
        System.out.println("zzz accountList:" + accountList.size());

        String[] datas = new String[accountList.size()];
        for (int i = 0; i < accountList.size(); i++) {
            datas[i] = accountList.get(i).getDid();
        }
        mPopupWindows = new MyPopupWindows(getActivity(), datas);
        mPopupWindows.setOnPopupClickListener(new MyPopupWindows.OnPopupClickListener() {

            @Override
            public void onDelete(int position) {
                if (accountList == null || accountList.isEmpty()) {
                    return;
                }
                if (position >= accountList.size()) {
                    return;
                }
                accountList.remove(position);
                saveAccountList(accountList);

                editDID.setText("");
                spinnerMode.setSelection(0);
				/*editInitstring.setText("");
				editWakeupkey.setText("");
				editip1.setText("");
				editip2.setText("");
				editip3.setText("");*/
            }

            @Override
            public void onClick(int position) {
                AccountObject ao = accountList.get(position);
                editDID.setText(ao.getDid());
                editDID.setSelection(ao.getDid().length());
                editInitstring.setText(ao.getInitString());
                editWakeupkey.setText(ao.getWakeupKey());
                editip1.setText(ao.getIp1());
                editip2.setText(ao.getIp2());
                editip3.setText(ao.getIp3());

                logText.setText("");
                showApi();

            }
        });
        mPopupWindows.ShowWindow(editLayout);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.login_btn:
                // connect button
                if (loginBtn.getText().equals("Stop")) {
                    mConnectTask.stopConnect();
                    isConnecting = false;
                    loginBtn.setText("Start");
                } else {
                    connectCount();
                }

                break;
            case R.id.popup_btn:
                // show drop-down box
                showPopup();
                break;
            case R.id.tx_base:
                if (layout_base.getVisibility() == View.VISIBLE) {
                    layout_base.setVisibility(View.GONE);
                    Drawable drawable = getResources().getDrawable(R.drawable.arrow_up);
                    if (drawable != null) {
                        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
                    }
                    tx_base.setCompoundDrawables(drawable, null, null, null);
                } else {
                    layout_base.setVisibility(View.VISIBLE);
                    Drawable drawable = getResources().getDrawable(R.drawable.arrow_down);
                    if (drawable != null) {
                        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
                    }
                    tx_base.setCompoundDrawables(drawable, null, null, null);
                }
                break;
            case R.id.tx_wakeup:
                if (layout_wakeup.getVisibility() == View.VISIBLE) {
                    layout_wakeup.setVisibility(View.GONE);
                    Drawable drawable = getResources().getDrawable(R.drawable.arrow_up);
                    if (drawable != null) {
                        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
                    }
                    tx_wakeup.setCompoundDrawables(drawable, null, null, null);
                } else {
                    layout_wakeup.setVisibility(View.VISIBLE);
                    Drawable drawable = getResources().getDrawable(R.drawable.arrow_down);
                    if (drawable != null) {
                        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
                    }
                    tx_wakeup.setCompoundDrawables(drawable, null, null, null);
                }
                break;
            default:
                break;

        }

    }

    public void addNewAccountObject(String did, String initString, String wakeup, String ip1, String ip2, String ip3) {
        AccountObject ao = new AccountObject();
        ao.setDid(did);
        ao.setInitString(initString);
        ao.setWakeupKey(wakeup);
        ao.setIp1(ip1);
        ao.setIp2(ip2);
        ao.setIp3(ip3);
        if (accountList == null) {
            accountList = new ArrayList<AccountObject>();
        } else {
            for (int i = 0; i < accountList.size(); i++) {
                // to determine whether did repeats, repeat to update the data
                if (accountList.get(i).getDid().equals(did)) {
                    accountList.set(i, ao);
                    return;
                }
            }
        }
        accountList.add(ao);
    }

    public void deleteAccountObject(int position) {
        accountList.remove(position);
    }


    public void saveDatas(String datas) {
        SharedPreferences.Editor mEditor = getActivity().getSharedPreferences(DATA_NAME, Context.MODE_PRIVATE).edit();
        mEditor.putString(DATA_NAME, datas);
        mEditor.commit();
    }


    public String readDatas() {
        SharedPreferences sp = getActivity().getSharedPreferences(DATA_NAME, Context.MODE_PRIVATE);
        return sp.getString(DATA_NAME, "");
    }


    public List<AccountObject> getAccountList() {
        String datas = readDatas();
        if (datas == null || datas.equals("")) {
            return null;
        }
        List<AccountObject> list = new ArrayList<AccountObject>();
        try {
            JSONArray jsonArray = new JSONArray(datas);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jo = jsonArray.getJSONObject(i);
                list.add(getAccountObject(jo));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }


    public void saveAccountList(List<AccountObject> list) {
        if (list == null || list.isEmpty()) {
            saveDatas("");
            return;
        }
        JSONArray jsonArray = new JSONArray();
        try {
            for (int i = 0; i < list.size(); i++) {
                JSONObject jo = getJsonObject(list.get(i));
                jsonArray.put(jo);
            }
        } catch (JSONException e) {
            Log.e("", "error:" + e.getLocalizedMessage());
            e.printStackTrace();
        }
        saveDatas(jsonArray.toString());
        if (list == null || list.isEmpty()) {
            popupBtn.setEnabled(false);
        } else {
            popupBtn.setEnabled(true);
        }
    }

    // AccountObject To JsonObject
    public JSONObject getJsonObject(AccountObject ao) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(DID, ao.getDid());
        jsonObject.put(INIT_STRING, ao.getInitString());
        jsonObject.put(WAKEUP, ao.getWakeupKey());
        jsonObject.put(IP1, ao.getIp1());
        jsonObject.put(IP2, ao.getIp2());
        jsonObject.put(IP3, ao.getIp3());
        return jsonObject;
    }

    // JsonObject To AccountObjectd
    public AccountObject getAccountObject(JSONObject jo) throws JSONException {
        AccountObject ao = new AccountObject();
        ao.setDid(jo.getString(DID));
        ao.setInitString(jo.getString(INIT_STRING));
        ao.setWakeupKey(jo.getString(WAKEUP));
        ao.setIp1(jo.getString(IP1));
        ao.setIp2(jo.getString(IP2));
        ao.setIp3(jo.getString(IP3));
        return ao;
    }

    ;

    public class AccountObject {
        private String did;
        private String mode;
        private String initString;
        private String wakeupKey;
        private String ip1;
        private String ip2;
        private String ip3;

        public String getDid() {
            return did;
        }

        public void setDid(String did) {
            this.did = did;
        }

        public String getInitString() {
            return initString;
        }

        public void setInitString(String initString) {
            this.initString = initString;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getWakeupKey() {
            return wakeupKey;
        }

        public void setWakeupKey(String wakeupKey) {
            this.wakeupKey = wakeupKey;
        }

        public String getIp1() {
            return ip1;
        }

        public void setIp1(String ip1) {
            this.ip1 = ip1;
        }

        public String getIp2() {
            return ip2;
        }

        public void setIp2(String ip2) {
            this.ip2 = ip2;
        }

        public String getIp3() {
            return ip3;
        }

        public void setIp3(String ip3) {
            this.ip3 = ip3;
        }


    }

}
