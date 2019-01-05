package com.shangyun.p2ptester;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.p2p.pppp_api.PPCS_APIs;
import com.shangyun.p2ptester.task.BaseTask;
import com.shangyun.p2ptester.task.ReadWriteTesterTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ReadWriteTesterFragment extends Fragment implements OnClickListener {

    public static final String TAG = "";
    private final String DATA_NAME = "Connection_data";
    private final String DID = "did";
    private final String Mode = "mode";
    private final String INIT_STRING = "init_string";
    private int CH_CMD = 0;
    private int CH_DATA = 0;
    ;
    private EditText editDID;
    private Spinner spinnerMode;
    private EditText editInitstring;
    private ViewGroup editLayout;
    private ReadWriteTesterTask mRWTTask;
    private TextView logText;
    private ScrollView scrollView;
    private ImageButton popupBtn;
    private Button loginBtn;
    private String did;
    private String initString;
    private boolean isRuunning;
    private Handler handler;
    private View parent;
    private List<AccountObject> accountList;
    private MyPopupWindows mPopupWindows;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        parent = inflater.inflate(R.layout.fragment_rwtester, null);
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

    private void initViews() {
        editDID = (EditText) parent.findViewById(R.id.didEdit);
        editInitstring = (EditText) parent.findViewById(R.id.initEdit);
        spinnerMode = (Spinner) parent.findViewById(R.id.spinnerMode);
        logText = (TextView) parent.findViewById(R.id.logText);
        scrollView = (ScrollView) parent.findViewById(R.id.scrollView1);
        popupBtn = (ImageButton) parent.findViewById(R.id.popup_btn);
        loginBtn = (Button) parent.findViewById(R.id.login_btn);
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
                        break;
                }
            }
        };
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.popup_btn:
                // show drop-down box
                showPopup();
                break;
            case R.id.login_btn:
                if (isRuunning) {
                    Toast.makeText(getActivity(), "P2P OnReadWriting...", Toast.LENGTH_SHORT).show();
                } else {
                    startTest();
                }

                break;
            default:
                break;
        }

    }

    private void startIt() {

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
                //editInitstring.setText("");
            }

            @Override
            public void onClick(int position) {
                AccountObject ao = accountList.get(position);
                editDID.setText(ao.getDid());
                editDID.setSelection(ao.getDid().length());
                editInitstring.setText(ao.getInitString());


                logText.setText("");
                showApi();

            }
        });
        mPopupWindows.ShowWindow(editLayout);
    }

    private void startTest() {
        did = editDID.getText().toString().trim();
        initString = editInitstring.getText().toString().trim();

        if (did.equals("") || initString.equals("")) {
            Toast.makeText(getActivity(), "请输入正确的信息！", 0).show();
            return;
        }
        logText.setText("");
        showApi();

        isRuunning = true;

        byte mode = (byte) spinnerMode.getSelectedItemPosition();
        mRWTTask = new ReadWriteTesterTask(getActivity(), handler, did, mode);

        if (ListenFragment.isListening) {
            //Compare with saved initString, prompt if different
            SharedPreferences preferences = getActivity().getSharedPreferences(ListenFragment.INIT_PRE_NAME, Context.MODE_PRIVATE);
            String save_init = preferences.getString(ListenFragment.INIT_PRE_KEY, "");
            if (!initString.equalsIgnoreCase(save_init)) {
                Toast.makeText(getActivity(), "InitString与listen界面不一致", 0).show();
                return;
            }
        } else {
            mRWTTask.deinitAll();
            mRWTTask.initAll(initString);
        }

        new Thread(new Runnable() {

            @Override
            public void run() {
                mRWTTask.readWriteTest();
                isRuunning = false;
            }
        }).start();
        ;


        // add datas
        addNewAccountObject(did, initString);
        // save datas to local
        saveAccountList(accountList);
    }


    public void addNewAccountObject(String did, String initString) {
        AccountObject ao = new AccountObject();
        ao.setDid(did);
        ao.setInitString(initString);

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

        return jsonObject;
    }

    // JsonObject To AccountObjectd
    public AccountObject getAccountObject(JSONObject jo) throws JSONException {
        AccountObject ao = new AccountObject();
        ao.setDid(jo.getString(DID));
        ao.setInitString(jo.getString(INIT_STRING));

        return ao;
    }

    ;

    public class AccountObject {
        private String did;
        private String initString;


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

    }


}
