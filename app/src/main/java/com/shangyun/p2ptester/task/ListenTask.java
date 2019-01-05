package com.shangyun.p2ptester.task;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Handler;

import com.p2p.pppp_api.PPCS_APIs;
import com.p2p.pppp_api.st_PPCS_Session;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListenTask extends BaseTask {

    private Context mContext;

    private String mAPILicense = "";
    private int mListenCount;

    private int SessionHandle = 0;
    private float sum_time;
    private float max_time;
    private float min_time = 99999;

    // define for Read/Write test mode
    private int TEST_WRITE_SIZE = 1004;  // (251 * 4), 125 is a prime number
    private int TOTAL_WRITE_SIZE = (4 * 1024 * TEST_WRITE_SIZE);
    private int TEST_NUMBER_OF_CHANNEL = 8;
    private List<RWInfo> rwInfos = new ArrayList<ListenTask.RWInfo>();

    public ListenTask(Context context, Handler handler, String did, String sAPILicense, int listenCount) {
        super(handler, did);
        // TODO Auto-generated constructor stub
        mContext = context;
        mAPILicense = sAPILicense;
        mListenCount = listenCount;
    }

    public int loginStatusCheck() {
        byte[] status = new byte[1];
        int sta = -1;
        int ret = PPCS_APIs.PPCS_LoginStatus_Check(status);
        if (ret == PPCS_APIs.ERROR_PPCS_SUCCESSFUL) {
            sta = status[0];
            System.out.println("---loginStatus:" + sta);

        }

        return sta;
    }

    String getErrorMessage(int err) {

        switch (err) {
            case 0:
                return "ERROR_PPCS_SUCCESSFUL";
            case -1:
                return "ERROR_PPCS_NOT_INITIALIZED";
            case -2:
                return "ERROR_PPCS_ALREADY_INITIALIZED";
            case -3:
                return "ERROR_PPCS_TIME_OUT";
            case -4:
                return "ERROR_PPCS_INVALID_ID";
            case -5:
                return "ERROR_PPCS_INVALID_PARAMETER";
            case -6:
                return "ERROR_PPCS_DEVICE_NOT_ONLINE";
            case -7:
                return "ERROR_PPCS_FAIL_TO_RESOLVE_NAME";
            case -8:
                return "ERROR_PPCS_INVALID_PREFIX";
            case -9:
                return "ERROR_PPCS_ID_OUT_OF_DATE";
            case -10:
                return "ERROR_PPCS_NO_RELAY_SERVER_AVAILABLE";
            case -11:
                return "ERROR_PPCS_INVALID_SESSION_HANDLE";
            case -12:
                return "ERROR_PPCS_SESSION_CLOSED_REMOTE";
            case -13:
                return "ERROR_PPCS_SESSION_CLOSED_TIMEOUT";
            case -14:
                return "ERROR_PPCS_SESSION_CLOSED_CALLED";
            case -15:
                return "ERROR_PPCS_REMOTE_SITE_BUFFER_FULL";
            case -16:
                return "ERROR_PPCS_USER_LISTEN_BREAK";
            case -17:
                return "ERROR_PPCS_MAX_SESSION";
            case -18:
                return "ERROR_PPCS_UDP_PORT_BIND_FAILED";
            case -19:
                return "ERROR_PPCS_USER_CONNECT_BREAK";
            case -20:
                return "ERROR_PPCS_SESSION_CLOSED_INSUFFICIENT_MEMORY";
            case -21:
                return "ERROR_PPCS_INVALID_APILICENSE";
            case -22:
                return "ERROR_PPCS_FAIL_TO_CREATE_THREAD";
            default:
                return "Unknow, something is wrong!";
        }
    }

    public void stopListen() {
        PPCS_APIs.PPCS_Listen_Break();
        isStop = true;
    }

    public void listenDevCount() {
        isStop = false;
        int TimeOut_sec = 60;
        byte bEnableInternet = 1; // allow clients from Internet to connect, LAN always enabled!
        int UDP_Port = 0;
        int counts = 0;
        boolean isCounts = true;
        // updateStatus("Start Listen");
        updateStatus("PPCS_Listen" + "('" + mDID + "'," + TimeOut_sec + "," + UDP_Port + "," + bEnableInternet + "," + "'"
                + mAPILicense + "') \n");
        //updateStatus("Listen Result:");
        while (mListenCount > 0 && !isStop) {

            System.out.println("---------mListenCount--------------" + mListenCount);
            counts++;

            long startSec = getTimes();
            SessionHandle = PPCS_APIs.PPCS_Listen(mDID, TimeOut_sec, UDP_Port, bEnableInternet, mAPILicense);
            System.out.println("SessionHandle::" + SessionHandle);

            long endSec = getTimes();
            float end_start = ((float) (endSec - startSec)) / 1000;
            if (max_time < end_start) {
                max_time = end_start;
            }
            if (min_time > end_start) {
                min_time = end_start;
            }
            sum_time += end_start;

            String count = counts + "";
            if (counts < 10) {
                count = "0" + counts;
            }

            if (SessionHandle >= 0) {
                st_PPCS_Session SInfo = new st_PPCS_Session();
                if (PPCS_APIs.PPCS_Check(SessionHandle, SInfo) == PPCS_APIs.ERROR_PPCS_SUCCESSFUL) {
                    String str;
                    //updateStatus("[Success]");
                    str = String.format("DID: %s", SInfo.getDID());
                    System.out.println(str);

                    String str1 = String.format("Remote Address=%s:%d", SInfo.getRemoteIP(), SInfo.getRemotePort());
                    System.out.println(str1);
                    //updateStatus(str);
                    String str2 = String.format("Mode= %s", (SInfo.getMode() == 0) ? "P2P" : "RLY");
                    System.out.println(str2);
                    updateStatus(count + " - " + str1 + "," + str2 + ",Time=" + end_start + "(Sec) \n");

                    listenRW(SessionHandle);

                }
            } else {
                if (SessionHandle == PPCS_APIs.ERROR_PPCS_USER_LISTEN_BREAK) {
                    updateStatus(count + " - " + "Listen break is called ! \n");
                } else {

                    String err = getErrorMessage(SessionHandle);
                    String text = String.format("%1s - Listen failed(%d) : %2s", count, SessionHandle, err);
                    updateStatus(text + "\n");
                }
            }
            mListenCount--;
            if (SessionHandle >= 0) {
                PPCS_APIs.PPCS_Close(SessionHandle);
            }
        }

    }

    private void listenRW(int session) {
        // TODO Auto-generated method stub
        // Read Mode from Client
        byte[] pReadBuffer = new byte[1];
        int[] ReadSize = new int[1];
        ReadSize[0] = 1;
        int ret = PPCS_APIs.PPCS_Read(session, CH_CMD, pReadBuffer, ReadSize, 50000);
        if (0 > ret) {
            System.out.println("PPCS_Read: Channel=" + CH_CMD + ",ret=" + ret);
            if (ret == PPCS_APIs.ERROR_PPCS_TIME_OUT) {
                updateStatus("fail to read Test Mode!!\n");
            } else if (ret == PPCS_APIs.ERROR_PPCS_SESSION_CLOSED_REMOTE) {
                updateStatus("Remote site call close!!\n");
            }
            return;
        }

        int Mode = pReadBuffer[0];
        System.out.println("listenTask mode:" + Mode);
        if (Mode != -1) {
            // gThread_Exit = 1; // Exit the LoginStatus_Check thread
        }

        // Select the test options according to the Mode
        switch (Mode) {
            case 0:
                ft_Test(); // File transfer test
                break;
            case 1:
                RW_Test();// Bidirectional read write test
                break;
            case 2:
                pkt_Test(); // PktRecv/PktSend test
                break;
            default:
                String text = String.format("the Mode(%d) Unknown!!", Mode);
                updateStatus(text + "\n");
                break;
        }


        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void ft_Test() {
        System.out.println("ft_Test");

        AssetManager asset = mContext.getAssets();
        InputStream inputStream = null;
        try {
            inputStream = asset.open("minion.mp4");
            int SizeCounter = 0;

            long start_time = getTimes();

            while (true) {

                int[] wsize = new int[1];
                //wsize[0] = 1024;

                int ret = PPCS_APIs.PPCS_Check_Buffer(SessionHandle, CH_DATA, wsize, null);
                if (0 > ret) {
                    String text = String.format("PPCS_Check_Buffer ret=%d %s \n", ret, getErrorMessage(ret));
                    updateStatus(text + "\n");
                    break;
                }

                int bufferringSize = wsize[0];
                System.out.println("PPCS_Check_Buffer size:" + bufferringSize);
                if (bufferringSize > 256 * 1024) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    continue;
                } else if (bufferringSize > 64 * 1024) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    continue;
                }

                byte[] buffer = new byte[1024];
                Arrays.fill(buffer, (byte) 0);

                int len = inputStream.read(buffer);
                if (len != -1) {

                    int writed = PPCS_APIs.PPCS_Write(SessionHandle, CH_DATA, buffer, len);
                    SizeCounter += len;
                    System.out.println("PPCS_Write ret:" + writed + ",SizeCounter:" + SizeCounter);
                    if (SizeCounter % (1024 * 1024) == 0) {
                        updateStatus("* ");
                    }
                } else {

                    long end_time = getTimes();
                    double time = (end_time - start_time) * 1.0 / 1000;
                    int speed = (int) (SizeCounter / 1024 / time);

                    String timeValue = String.format("%.2f Sec", time);

                    updateStatus("\nFile Transfer Done!! Write Size = " + SizeCounter + " byte,Time = " + timeValue + ",Speed = " + speed + "kByte/sec\n");
                    break;
                }
            }


        } catch (IOException e) {
            // TODO Auto-generated catch block
            updateStatus("file not found\n");
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    private void pkt_Test() {
        System.out.println("listenTask pkt_Test");
        for (int i = 0; i < 1000; i++) {

            byte[] pktBuf = new byte[1024];
            Arrays.fill(pktBuf, (byte) (i % 100));

            int ret = PPCS_APIs.PPCS_PktSend(SessionHandle, CH_DATA, pktBuf, pktBuf.length);
            //System.out.println("--i:" + i + ",PPCS_PktSend ret:"+ret + ",data:"+pktBuf[0]);
            if (ret < 0) {
                if (PPCS_APIs.ERROR_PPCS_SESSION_CLOSED_TIMEOUT == ret) {
                    updateStatus("Session Closed TimeOUT!!\n");
                    break;
                } else if (PPCS_APIs.ERROR_PPCS_SESSION_CLOSED_REMOTE == ret) {
                    updateStatus("Session Remote Close!!\n");
                    break;
                }
            }

            if (i % 100 == 99) {
                String log = String.format("----->Send %d packets. (1 packets=%d byte) \n", i + 1, pktBuf.length);
                updateStatus(log);
            }

            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void RW_Test() {
        // TODO Auto-generated method stub
        System.out.println("RW_Test");

        try {

            List<Thread> writeThreads = new ArrayList<Thread>();
            List<Thread> readThreads = new ArrayList<Thread>();

            for (int i = 0; i < TEST_NUMBER_OF_CHANNEL; i++) {

                rwInfos.add(new RWInfo());

                Thread w = new Thread(new ThreadWrite(i));
                w.start();
                writeThreads.add(w);

                Thread r = new Thread(new ThreadRead(i));
                r.start();
                readThreads.add(r);

                Thread.sleep(20);

            }

            for (int i = 0; i < TEST_NUMBER_OF_CHANNEL; i++) {
                Thread w = writeThreads.get(i);
                w.join();

                Thread r = readThreads.get(i);
                r.join();

            }

            for (int i = 0; i < TEST_NUMBER_OF_CHANNEL; i++) {

                RWInfo info = rwInfos.get(i);
                if (info != null) {
                    String readLog = String.format("\nThreadRead  Channel %d Exit - TotalSize: %d byte , Time:%d sec, Speed:%d KByte/sec \n", i, info.totalRead, info.readTimes / 1000, info.totalRead / info.readTimes);
                    updateStatus(readLog);

                    String writeLog = String.format("ThreadWrite Channel %d Exit - TotalSize: %d byte, Time:%d sec, Speed:%d KByte/sec \n", i, info.totalWrite, info.writeTimes / 1000, info.totalWrite / info.writeTimes);
                    updateStatus(writeLog);
                }
            }

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }

    }


    private class ThreadWrite implements Runnable {

        private int channel;

        public ThreadWrite(int channel) {
            // TODO Auto-generated constructor stub
            this.channel = channel;
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            byte[] buffer = new byte[TEST_WRITE_SIZE];
            for (int i = 0; i < buffer.length; i++) {
                buffer[i] = (byte) (i % 251);
            }
            updateStatus(String.format("ThreadWrite Channel %d running...\n", channel));

            long startTime = getTimes();
            int totalSize = 0;

            int Check_ret = 0;
            int ret = 0;
            int[] wsize = new int[1];
            //wsize[0] = 0;
            //while ((Check_ret = PPCS_APIs.PPCS_Check_Buffer(SessionHandle, (byte)channel, wsize, null)) == PPCS_APIs.ERROR_PPCS_SUCCESSFUL)
            while (true) {
                Check_ret = PPCS_APIs.PPCS_Check_Buffer(SessionHandle, (byte) channel, wsize, null);
                if (0 > Check_ret) {
                    System.out.println("PPCS_Check_Buffer CH=" + channel + ", ret=" + Check_ret);
                    updateStatus(String.format("PPCS_Check_Buffer CH=%d, ret=%d [%s]\n", channel, Check_ret, getErrorMessage(Check_ret)));
                    break;
                }
                int writeSize = wsize[0];
                //System.out.println("ThreadWrite writeSize:"+writeSize + ",totalSize:"+totalSize);

                if (writeSize < 256 * 1024 && totalSize < TOTAL_WRITE_SIZE) {
                    ret = PPCS_APIs.PPCS_Write(SessionHandle, (byte) channel, buffer, TEST_WRITE_SIZE);
                    if (0 > ret) {
                        if (PPCS_APIs.ERROR_PPCS_SESSION_CLOSED_TIMEOUT == ret) {
                            updateStatus(String.format("ThreadWrite CH=%d, ret=%d, Session Closed TimeOUT!!\n", channel, ret));
                        } else if (PPCS_APIs.ERROR_PPCS_SESSION_CLOSED_REMOTE == ret) {
                            updateStatus(String.format("ThreadWrite CH=%d, ret=%d, Session Remote Close!!\n", channel, ret));
                        } else {
                            updateStatus(String.format("ThreadWrite CH=%d, ret=%d [%s]\n", channel, ret, getErrorMessage(ret)));
                        }
                        continue;
                    }
                    totalSize += ret;
                } else if (0 == writeSize) {
                    System.out.println("writeSize break");
                    break;
                } else {
                    try {
                        Thread.sleep(2);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

            }

            long endTime = getTimes();

            RWInfo info = rwInfos.get(channel);
            info.writeTimes = endTime - startTime;
            info.totalWrite = totalSize;
        }

    }

    private class ThreadRead implements Runnable {

        private int channel;

        public ThreadRead(int channel) {
            // TODO Auto-generated constructor stub
            this.channel = channel;
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            int TotalSize = 0;
            int timeout_ms = 200;
            long startTime = getTimes();
            updateStatus(String.format("ThreadRead Channel %d running...\n", channel));
            while (true) {

                byte[] pReadBuffer = new byte[1];
                int[] ReadSize = new int[1];
                ReadSize[0] = 1;
                int ret = PPCS_APIs.PPCS_Read(SessionHandle, (byte) channel, pReadBuffer, ReadSize, timeout_ms);
                if (ret < 0 && ret != PPCS_APIs.ERROR_PPCS_TIME_OUT) {
                    if (TotalSize == TOTAL_WRITE_SIZE) break;
                    String log = String.format("\n PPCS_Read ret=%d, CH=%d, ReadSize=%d byte, TotalSize=%d byte\n", ret, channel, ReadSize[0], TotalSize);
                    updateStatus(log);
                    break;
                }

                int readSize = ReadSize[0];
                int zz = pReadBuffer[0];
                if (zz < 0) {//if the write is negative, add 256 to the positive number
                    zz = pReadBuffer[0] & 0xff;
                }

                if (readSize > 0 && TotalSize % 251 != zz) {
                    String log = String.format("\n PPCS_Read ret=%d, Channel:%d Error!! ReadSize=%d, TotalSize=%d, zz=%d\n", ret, channel, readSize, TotalSize, zz);
                    updateStatus(log);
                    break;
                } else if (TotalSize % (1 * 1024 * 1024) == 1 * 1024 * 1024 - 1) {
                    updateStatus(channel + " ");
                }

                TotalSize += readSize;
                if (TotalSize == TOTAL_WRITE_SIZE) break;
            }

            long endTime = getTimes();
            RWInfo info = rwInfos.get(channel);
            info.readTimes = endTime - startTime;
            info.totalRead = TotalSize;
        }

    }


    private class RWInfo {

        public int totalRead;

        public int totalWrite;

        public long readTimes;

        public long writeTimes;

    }


}
