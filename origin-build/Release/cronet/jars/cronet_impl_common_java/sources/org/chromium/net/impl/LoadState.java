package org.chromium.net.impl;

public interface LoadState {
    public static final int CONNECTING = 12;
    public static final int DOWNLOADING_PROXY_SCRIPT = 7;
    public static final int ESTABLISHING_PROXY_TUNNEL = 10;
    public static final int IDLE = 0;
    public static final int READING_RESPONSE = 16;
    public static final int RESOLVING_HOST = 11;
    public static final int RESOLVING_HOST_IN_PROXY_SCRIPT = 9;
    public static final int RESOLVING_PROXY_FOR_URL = 8;
    public static final int SENDING_REQUEST = 14;
    public static final int SSL_HANDSHAKE = 13;
    public static final int THROTTLED = 1;
    public static final int WAITING_FOR_APPCACHE = 6;
    public static final int WAITING_FOR_AVAILABLE_SOCKET = 3;
    public static final int WAITING_FOR_CACHE = 5;
    public static final int WAITING_FOR_DELEGATE = 4;
    public static final int WAITING_FOR_RESPONSE = 15;
    public static final int WAITING_FOR_STALLED_SOCKET_POOL = 2;
}
