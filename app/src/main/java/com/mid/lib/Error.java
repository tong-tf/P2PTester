package com.mid.lib;

public enum Error {
    ERROR_NONE,     // operation ok
    ERROR_RETRY,   // should call again
    ERROR_FAULT    // someting happen, a new try
}
