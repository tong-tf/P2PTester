package com.p2p.pppp_api;

public class PPCS_APIs {
    public static final int ERROR_PPCS_SUCCESSFUL = 0;
    public static final int ERROR_PPCS_NOT_INITIALIZED = -1;
    public static final int ERROR_PPCS_ALREADY_INITIALIZED = -2;
    public static final int ERROR_PPCS_TIME_OUT = -3;
    public static final int ERROR_PPCS_INVALID_ID = -4;
    public static final int ERROR_PPCS_INVALID_PARAMETER = -5;
    public static final int ERROR_PPCS_DEVICE_NOT_ONLINE = -6;
    public static final int ERROR_PPCS_FAIL_TO_RESOLVE_NAME = -7;
    public static final int ERROR_PPCS_INVALID_PREFIX = -8;
    public static final int ERROR_PPCS_ID_OUT_OF_DATE = -9;
    public static final int ERROR_PPCS_NO_RELAY_SERVER_AVAILABLE = -10;
    public static final int ERROR_PPCS_INVALID_SESSION_HANDLE = -11;
    public static final int ERROR_PPCS_SESSION_CLOSED_REMOTE = -12;
    public static final int ERROR_PPCS_SESSION_CLOSED_TIMEOUT = -13;
    public static final int ERROR_PPCS_SESSION_CLOSED_CALLED = -14;
    public static final int ERROR_PPCS_REMOTE_SITE_BUFFER_FULL = -15;
    public static final int ERROR_PPCS_USER_LISTEN_BREAK = -16;
    public static final int ERROR_PPCS_MAX_SESSION = -17;
    public static final int ERROR_PPCS_UDP_PORT_BIND_FAILED = -18;
    public static final int ERROR_PPCS_USER_CONNECT_BREAK = -19;
    public static final int ERROR_PPCS_SESSION_CLOSED_INSUFFICIENT_MEMORY = -20;
    public static final int ERROR_PPCS_INVALID_APILICENSE = -21;
    public static final int ER_ANDROID_NULL = -5000;
    public static int ms_verAPI = 0;

    static {
        try {
            System.loadLibrary("PPCS_API");
            ms_verAPI = PPCS_GetAPIVersion();
        } catch (UnsatisfiedLinkError ule) {
            System.out.println("loadLibrary PPCS_API lib," + ule.getMessage());
        }
    }

    public native static int PPCS_GetAPIVersion();

    public native static int PPCS_QueryDID(String DeviceName, String DID, int DIDBufSize);

    public native static int PPCS_Initialize(byte[] Parameter);

    public native static int PPCS_DeInitialize();

    public native static int PPCS_NetworkDetect(st_PPCS_NetInfo NetInfo, int UDP_Port);

    public native static int PPCS_NetworkDetectByServer(st_PPCS_NetInfo NetInfo, int UDP_Port, String ServerString);

    public native static int PPCS_Share_Bandwidth(byte bOnOff);

    public native static int PPCS_Listen(String MyID, int TimeOut_sec, int UDP_Port, byte bEnableInternet, String APILicense);

    public native static int PPCS_Listen_Break();

    public native static int PPCS_LoginStatus_Check(byte[] bLoginStatus);

    public native static int PPCS_Connect(String TargetID, byte bEnableLanSearch, int UDP_Port);

    public native static int PPCS_ConnectByServer(String TargetID, byte bEnableLanSearch, int UDP_Port, String ServerString);

    public native static int PPCS_Connect_Break();

    public native static int PPCS_Check(int SessionHandle, st_PPCS_Session SInfo);

    public native static int PPCS_Close(int SessionHandle);

    public native static int PPCS_ForceClose(int SessionHandle);

    public native static int PPCS_Write(int SessionHandle, byte Channel, byte[] DataBuf, int DataSizeToWrite);

    public native static int PPCS_Read(int SessionHandle, byte Channel, byte[] DataBuf, int[] DataSize, int TimeOut_ms);

    public native static int PPCS_Check_Buffer(int SessionHandle, byte Channel, int[] WriteSize, int[] ReadSize);

    public native static int PPCS_PktSend(int SessionHandle, byte Channel, byte[] PktBuf, int PktSize);

    public native static int PPCS_PktRecv(int SessionHandle, byte Channel, byte[] PktBuf, int[] PktSize, int TimeOut_ms);
}
