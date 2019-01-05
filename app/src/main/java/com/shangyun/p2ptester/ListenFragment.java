package com.shangyun.p2ptester;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.p2p.pppp_api.PPCS_APIs;
import com.shangyun.p2ptester.task.ListenTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ListenFragment extends Fragment implements OnClickListener {

    public static final String TAG = ListenFragment.class.getName();
    public static final String INIT_PRE_NAME = "INIT_NAME";
    public static final String INIT_PRE_KEY = "INIT_KEY";
    public static final int MSG_LOG = 0;
    public static final int MSG_LISTEN_FINISH = 1;
    public static final int MSG_DELAY = 2;
    public static boolean isListening;
    private final String DATA_NAME = "Listen_data";
    private final String DID = "did";
    private final String API_LICENSE = "api_license";
    private final String CRC_KEY = "crc_key";
    private final String INIT_STRING = "init_string";
    protected ListenTask mListenTask;
    private View parent;
    private String Dev_DID = "";
    private Handler handler;
    private Button loginBtn;
    private EditText mDidEdit;
    private EditText mApiEdit;
    private EditText mCrcEdit;
    private EditText mInitEdit;
    private EditText mCountEdit;
    private ViewGroup editLayout;
    private TextView logText;
    private ScrollView scrollView1;
    private ImageButton popupBtn;
    private int mLastLoginStatus = -1;
    private int statusErrorCount = 0;

    private List<AccountObject> accountList;
    private MyPopupWindows mPopupWindows;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // TODO Auto-generated method stub

        parent = inflater.inflate(R.layout.fragment_listentester, null);
        initViews();
        initDatas();
        mDidEdit.clearFocus();
        showApi();

        Editor editor = getActivity().getSharedPreferences(INIT_PRE_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(INIT_PRE_KEY, "").commit();

        return parent;
    }

    private void showApi() {
        //API version
        String apiver;
        int n = PPCS_APIs.ms_verAPI;
        apiver = String.format("API ver: %d.%d.%d.%d\n", (n >> 24) & 0xff,
                (n >> 16) & 0xff, (n >> 8) & 0xff, n & 0xff);
        log(apiver);
        initHandler();
    }

    private void log(String msg) {
        Log.i(TAG, msg);
        logText.append(msg);

        scrollView1.fullScroll(View.FOCUS_DOWN);
    }

    private void initHandler() {
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_LOG:
                        String text = (String) msg.obj;
                        log(text);
                        System.out.println("text:" + text);
                        break;
                    case MSG_DELAY:
                        if (isListening) {
                            int status = mListenTask.loginStatusCheck();

                            if (status == 0) {
                                statusErrorCount++;
                                if (statusErrorCount == 3) {
                                    log("No Server Response...\n");
                                } else {
                                    handler.sendEmptyMessageDelayed(MSG_DELAY, 60000);
                                }

                            } else if (mLastLoginStatus != status) {

                                statusErrorCount = 0;
                                log("Got Server Response...\n");
                                handler.sendEmptyMessageDelayed(MSG_DELAY, 60000);
                            }

                            mLastLoginStatus = status;
                        }
                        break;

                    default:
                        break;
                }
            }
        };
    }

    public void initViews() {
        loginBtn = (Button) parent.findViewById(R.id.login_btn);
        mDidEdit = (EditText) parent.findViewById(R.id.didEdit);
        mApiEdit = (EditText) parent.findViewById(R.id.apiEdit);
        mCrcEdit = (EditText) parent.findViewById(R.id.crcEdit);
        mInitEdit = (EditText) parent.findViewById(R.id.initEdit);
        mCountEdit = (EditText) parent.findViewById(R.id.countEdit);
        logText = (TextView) parent.findViewById(R.id.logText);

        scrollView1 = (ScrollView) parent.findViewById(R.id.scrollView1);
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

    public void listen() {
        String apils_crcKey;
        String did = mDidEdit.getText().toString().trim();
        String apiLicense = mApiEdit.getText().toString().trim();
        String crcKey = mCrcEdit.getText().toString().trim();
        String initString = mInitEdit.getText().toString().trim();


        if (crcKey.equals("")) {
            apils_crcKey = apiLicense;
        } else {
            apils_crcKey = apiLicense + ":" + crcKey;
        }


        if (did.equals("") || apiLicense.equals("") || initString.equals("")) {
            Toast.makeText(getActivity(), "请输入正确的信息！", 0).show();
            return;
        }


        int litenCount = 1;

        String scount = mCountEdit.getText().toString();
        if (scount.length() > 0) {
            litenCount = Integer.parseInt(scount);
        }
        if (litenCount < 1) {
            litenCount = 1;
            mCountEdit.setText("1");
        }
        System.out.println("litenCount:" + litenCount);

        isListening = true;

        mListenTask = new ListenTask(getActivity(), handler, did, apils_crcKey, litenCount);

        SharedPreferences preferences = getActivity().getSharedPreferences(INIT_PRE_NAME, Context.MODE_PRIVATE);
        String save_init = preferences.getString(INIT_PRE_KEY, "");

        System.out.println("save_init:" + save_init);
        System.out.println("initString:" + initString);

        if (!initString.equalsIgnoreCase(save_init)) {

            mListenTask.deinitAll();
            mListenTask.initAll(initString);
            preferences.edit().putString(INIT_PRE_KEY, initString).commit();
        }

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {

                handler.sendEmptyMessageDelayed(MSG_DELAY, 10000);

                mListenTask.networkDetect();
                mListenTask.listenDevCount();
                isListening = false;
                mLastLoginStatus = -1;
            }
        });
        t.start();

        //add datas
        addNewAccountObject(did, apiLicense, crcKey, initString);
        //save datas to local
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
				/*if(accountList==null||accountList.isEmpty()){
					popupBtn.setEnabled(false);
				}else{
					popupBtn.setEnabled(true);
				}*/
                mDidEdit.setText("");
                mApiEdit.setText("");
                mCrcEdit.setText("");
                //mInitEdit.setText("");
            }

            @Override
            public void onClick(int position) {
                AccountObject ao = accountList.get(position);
                mDidEdit.setText(ao.getDid());
                mDidEdit.setSelection(ao.getDid().length());
                Dev_DID = mDidEdit.getText().toString().trim();
                mApiEdit.setText(ao.getApiLicense());
                mCrcEdit.setText(ao.getCrcKey());
                mInitEdit.setText(ao.getInitString());
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

                if (isListening) {
                    Toast.makeText(getActivity(), "P2P OnListening...", Toast.LENGTH_SHORT).show();
                } else {
                    logText.setText("");
                    showApi();
                    listen();
                }
                break;
            case R.id.popup_btn:
                // show drop-down box
                showPopup();
                break;
        }

    }

    public void addNewAccountObject(String did, String apiLicense, String crcKey, String initString) {
        AccountObject ao = new AccountObject();
        ao.setDid(did);
        ao.setApiLicense(apiLicense);
        ao.setCrcKey(crcKey);
        ao.setInitString(initString);
        if (accountList == null) {
            accountList = new ArrayList<AccountObject>();
        } else {
            for (int i = 0; i < accountList.size(); i++) {
                //To determine whether did repeats, repeat to update the data
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
        System.out.println("saveAccountList:" + list.size());
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

    //AccountObject To JsonObject
    public JSONObject getJsonObject(AccountObject ao) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(DID, ao.getDid());
        jsonObject.put(API_LICENSE, ao.getApiLicense());
        jsonObject.put(CRC_KEY, ao.getCrcKey());
        jsonObject.put(INIT_STRING, ao.getInitString());
        return jsonObject;
    }

    //JsonObject To AccountObjectd
    public AccountObject getAccountObject(JSONObject jo) throws JSONException {
        AccountObject ao = new AccountObject();
        ao.setDid(jo.getString(DID));
        ao.setApiLicense(jo.getString(API_LICENSE));
        ao.setCrcKey(jo.getString(CRC_KEY));
        ao.setInitString(jo.getString(INIT_STRING));
        return ao;
    }

    ;

    public class AccountObject {
        private String did;
        private String apiLicense;
        private String crcKey;
        private String initString;

        public String getDid() {
            return did;
        }

        public void setDid(String did) {
            this.did = did;
        }

        public String getApiLicense() {
            return apiLicense;
        }

        public void setApiLicense(String apiLicense) {
            this.apiLicense = apiLicense;
        }

        public String getCrcKey() {
            return crcKey;
        }

        public void setCrcKey(String crcKey) {
            this.crcKey = crcKey;
        }

        public String getInitString() {
            return initString;
        }

        public void setInitString(String initString) {
            this.initString = initString;
        }

    }


}
