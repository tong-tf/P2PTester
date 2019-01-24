package com.shangyun.p2ptester.utils;

public class ErrMsg {
    public static String getErrorMessage(int err) {

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
                return "Unknow, something else is happen!";
        }
    }
}
