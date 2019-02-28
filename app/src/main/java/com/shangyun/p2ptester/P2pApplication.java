package com.shangyun.p2ptester;


import android.app.Application;

public class P2pApplication extends Application {
    private int mHandleSession = -1;

    public void setmHandleSession(int s){ mHandleSession = s;}
    public int getmHandleSession() { return mHandleSession; }
}
