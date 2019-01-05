package com.shangyun.p2ptester.task;

import android.os.Handler;

import com.p2p.pppp_api.PPCS_APIs;
import com.p2p.pppp_api.st_PPCS_Session;
import com.shangyun.p2ptester.P2P_StringEncDec;

import java.math.BigDecimal;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectTask extends BaseTask {

    private byte mMode = 0;
    private int mConnectCount;
    private int mDelaytimes;

    /**
     * wakeup
     ***/
    private String mWakeupKey;
    private List<String> mIPs;
    private int mLastSleepLogin = -100;
    private int[] mLastLogin = {ERROR_UnKnown, ERROR_UnKnown, ERROR_UnKnown};

    private int mHandleSession = -99;
    private int sumCount = 0;
    private int CountOK = 0;
    private int p2pCount = 0;
    private int rlyCount = 0;
    private float max_time;
    private float min_time;
    private float sum_time;

    // Connect Thread Number
    private int THREAD_NUM = 3;
    // 63->Lan, 60->P2P, 94->RLY
    private byte[] thread_mode = {63, 60, 94};
    private List<Thread> threads = new ArrayList<Thread>();
    private Lock lock = new ReentrantLock();

    public ConnectTask(Handler handler, String did, byte mode, int counts, int delaytimes) {
        super(handler, did);
        mMode = mode;
        mConnectCount = counts;
        mDelaytimes = delaytimes;
    }

    public ConnectTask(Handler handler, String did, byte mode, int counts, int delaytimes,
                       String wakeupKey, List<String> ips) {
        super(handler, did);

        mMode = mode;
        mConnectCount = counts;
        mDelaytimes = delaytimes;
        mWakeupKey = wakeupKey;
        mIPs = ips;

    }

    public void stopConnect() {
        isStop = true;
        if (mWakeupKey != null) {
            if (mLastSleepLogin != -100) {
                PPCS_APIs.PPCS_Connect_Break();
            }

        } else {
            PPCS_APIs.PPCS_Connect_Break();
        }
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

    public void connectDevCount() {
        isStop = false;
        byte[] CMD_Enc = new byte[60];
        Arrays.fill(CMD_Enc, (byte) 0);

        networkDetect();

        if (mMode == 4) {
            return;
        }


        System.out.println("mConnectCount:" + mConnectCount);

        if (mWakeupKey != null) {
            // cryptographic query instruction
            String CMD = "DID=" + mDID + "&";

            if (0 > P2P_StringEncDec.iPN_StringEnc(mWakeupKey.getBytes(), CMD.getBytes(), CMD_Enc, CMD_Enc.length)) {
                updateStatus("***WakeUp Query Cmd StringEncode failed!\n");
                return;
            }
        }

        while (mConnectCount > 0 && !isStop) {
            mHandleSession = -99;
            long startSec = getTimes();

            System.out.println("mDID:" + mDID);
            System.out.println("mMode:" + mMode);

            if (mWakeupKey != null) {
                mLastSleepLogin = wakeupQuery(CMD_Enc);
                System.out.println("lastSleepLogin:" + mLastSleepLogin);
            }

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            java.sql.Date curDate = new java.sql.Date(System.currentTimeMillis());
            String getTime = formatter.format(curDate);
            if (mMode != 7) {
                mHandleSession = PPCS_APIs.PPCS_Connect(mDID, mMode, UDP_Port);
                System.out.println("m_handleSession:" + mHandleSession);
            } else {
                mHandleSession = ceateThreadToConnect();
                System.out.println("m_handleSession:" + mHandleSession);
            }


            long endSec = getTimes();


            mConnectCount--;
            sumCount++;


            String str1 = "", str2 = "", str3 = "";

            String count = sumCount + "";
            if (sumCount < 10) {
                count = "0" + sumCount;
            }

            if (mWakeupKey != null) {
                str1 = String.format("%s - LastSleepLogin=%s", count, mLastSleepLogin);
                str1 += "(" + mLastLogin[0] + "," + mLastLogin[1] + "," + mLastLogin[2] + ")";
            } else {
                str1 = String.format("%s - ", count);
            }
            str1 = String.format("[%s] %s", getTime, str1);

            if (mHandleSession >= 0) {
                float end_start = ((float) (endSec - startSec)) / 1000;
                if (max_time < end_start) {
                    max_time = end_start;
                }
                if (min_time > end_start || min_time == 0) {
                    min_time = end_start;
                }
                sum_time += end_start;
                System.out.println(startSec + "/" + endSec + "/" + end_start);
                System.out.println(max_time + "/" + min_time + "/" + end_start);

                st_PPCS_Session SInfo = new st_PPCS_Session();

                if (PPCS_APIs.PPCS_Check(mHandleSession, SInfo) == PPCS_APIs.ERROR_PPCS_SUCCESSFUL) {

                    CountOK++;

                    str2 = String.format("Remote Address=%s:%d", SInfo.getRemoteIP(), SInfo.getRemotePort());
                    System.out.println(str2);

                    str3 = String.format("Mode= %s", (SInfo.getMode() == 0) ? "P2P" : "RLY");
                    System.out.println(str3);
                    if (SInfo.getMode() == 0) {
                        p2pCount++;
                    } else {
                        rlyCount++;
                    }

                    updateStatus(str1 + str2 + "," + str3 + ",Time=" + end_start + "(Sec) \n");

                } else {
                    updateStatus("Remote Address = Unknow (remote close)\n");
                }
                PPCS_APIs.PPCS_Close(mHandleSession);

            } else {
                if (mHandleSession == PPCS_APIs.ERROR_PPCS_USER_CONNECT_BREAK) {
                    updateStatus("Connect break is called ! \n");
                } else {

                    //updateStatus("[Failed]");

                    String err = getErrorMessage(mHandleSession);
                    String text = String.format("Connect failed(%d) : %s", mHandleSession, err);
                    updateStatus(str1 + text + "\n");

                }

            }


            System.out.println("connectDevCount mConnectCount:" + mConnectCount);

            if (mConnectCount > 0) {
                try {
                    Thread.sleep(mDelaytimes * 1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        if (!isStop) {

            float averge_time;
            float averge_time3 = 0;

            System.out.println("connectDevCount CountOK:" + CountOK);

            if (CountOK > 0) {
                averge_time = sum_time / CountOK;
                BigDecimal b = new BigDecimal(averge_time);
                averge_time3 = b.setScale(3, BigDecimal.ROUND_HALF_UP).floatValue();
            }

            updateStatus("Total Connection times: " + sumCount + ",Success: " + CountOK + "(" + (float) CountOK / sumCount * 100
                    + "%" + "," + " max=" + (float) max_time + "sec" + ", averge=" + averge_time3 + "sec" + ", min="
                    + min_time + "sec" + ")" + ", P2P :" + p2pCount + "(" + (float) p2pCount / sumCount * 100 + "%"
                    + ")" + ", RLY: " + rlyCount + "(" + (float) rlyCount / sumCount * 100 + "%" + ") \n");
        }

    }

    private int ceateThreadToConnect() {
        // TODO Auto-generated method stub
        System.out.println("ceateThreadToConnect start:" + mHandleSession);
        threads.clear();
        for (byte mode : thread_mode) {

            Thread thread = new Thread(new ThreadConnect(mode), mode + "");
            thread.start();
            threads.add(thread);
        }

        try {
            for (Thread t : threads) {
                t.join();
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }

        System.out.println("ceateThreadToConnect ret:" + mHandleSession);
        return mHandleSession;
    }

    private int wakeupQuery(byte[] CMD) {

        int tryCount = 3;
        int timeout_ms = 2000;
        int timeOutCount = 0;

        int flag[] = {0, 0, 0};

        byte[] recvBuf = new byte[256];
        byte[] Message = new byte[128];

        //while (tryCount > timeOutCount) {

        for (int i = 0; i < mIPs.size(); i++) {
            String ip = mIPs.get(i);

            DatagramSocket socket = null;

            try {
                // 1.define the server's address, port number, and data
                InetAddress address = InetAddress.getByName(ip);
                int port = 12305;
                // 2.creates datagrams that contain the data information sent
                DatagramPacket packet = new DatagramPacket(CMD, CMD.length, address, port);
                // 3.create DatagramSocket object
                socket = new DatagramSocket();
                socket.setSoTimeout(timeout_ms);
                // 4.send datagrams to the server
                socket.send(packet);

                /*
                 * receive server response data
                 */
                // 1.creates datagrams to receive data from server-side responses
                DatagramPacket packet2 = new DatagramPacket(recvBuf, recvBuf.length);
                // 2.receive server response data
                socket.receive(packet2);
                // 3.read data
                if (0 > P2P_StringEncDec.iPN_StringDnc(mWakeupKey.getBytes(), recvBuf, Message, Message.length)) {
                    System.out.println("WakeUp_Query-iPN_StringDnc failed.\n");
                    return getMinNumFromLastLogin(mLastLogin);
                }

                int lastLogin = ERROR_UnKnown;

                System.out.println("WakeUp_Query receive:" + new String(Message));

                String ret = GetStringItem(new String(Message), "LastLogin", "&");
                if (ret == null)
                    System.out.println("can not get LastLogin Item!\n");
                else
                    lastLogin = Integer.parseInt(ret);

                flag[i] = 1;
                mLastLogin[i] = lastLogin;

            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    socket.close();
                }
            }
        }
        //}

        int MinLastLogin = getMinNumFromLastLogin(mLastLogin);

        return MinLastLogin;
    }

    class ThreadConnect implements Runnable {
        private byte type;
        private int session = -1;


        public ThreadConnect(byte type) {
            if (63 != type || 60 != type || 94 != type) {
                System.out.println("type=" + type);
            }
            this.type = type;
        }

        @Override
        public void run() {
            //super.run();

            if (0 > mHandleSession) {
                System.out.println("connect start:" + new Date(System.currentTimeMillis()));

                try {
                    if (type == 63) {
                        Thread.sleep(200);
                    } else if (type == 94) {
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    return;
                }


                System.out.println("PPCS_Connect(" + mDID + ", " + type + ", 0)...");
                session = PPCS_APIs.PPCS_Connect(mDID, type, 0);//(byte)mType

                System.out.println("connect end:" + new Date(System.currentTimeMillis()));

                if (session < 0) {
                    System.out.println("PPCS_Connect ret=" + session);
                    lock.lock();
                    if (type == 60 && session != -19) {
                        mHandleSession = session;
                    }
                    lock.unlock();
                } else {
                    PPCS_APIs.PPCS_Connect_Break();

                    lock.lock();
					
					/*st_PPCS_Session SInfo=new st_PPCS_Session();
					if(PPCS_APIs.PPCS_Check(session, SInfo)==PPCS_APIs.ERROR_PPCS_SUCCESSFUL){
						String str;
						str=String.format("  ----Session Ready: -%s----", (SInfo.getMode()==0) ? "P2P" : "RLY");
						System.out.println(str);
						str=String.format("  Socket: %d", SInfo.getSkt());  System.out.println(str);
						str=String.format("  Remote Addr: %s:%d", SInfo.getRemoteIP(),SInfo.getRemotePort());  System.out.println(str);
						str=String.format("  My Lan Addr: %s:%d", SInfo.getMyLocalIP(),SInfo.getMyLocalPort());System.out.println(str);
						str=String.format("  My Wan Addr: %s:%d", SInfo.getMyWanIP(),SInfo.getMyWanPort());  System.out.println(str);
						str=String.format("  Connection time: %d", SInfo.getConnectTime());  System.out.println(str);
						str=String.format("  DID: %s", SInfo.getDID());  System.out.println(str);
						str=String.format("  I am : %s", (SInfo.getCorD() ==0)? "Client":"Device");  System.out.println(str);						
					}*/


                    System.out.println("PPCS_Connect ret=" + session);

                    if (0 > mHandleSession) {
                        mHandleSession = session;
                        for (Thread thread : threads) {
                            byte mode = (byte) Integer.parseInt(thread.getName());

                            if (mode != mMode) {
                                thread.interrupt();
                            }
                        }
                    } else {
                        PPCS_APIs.PPCS_Close(session);
                        System.out.println("PPCS_Close(" + session + ")!!");
                    }

                    lock.unlock();
                }
            }

        }
    }

}
