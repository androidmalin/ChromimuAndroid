package org.chromium.net.impl;


//chromium/src/out/Release/gen/components/cronet/android/load_states_list/java_cpp_template/org/chromium/net/impl/LoadState.java
public interface LoadState {
    public static int IDLE = 0;
    public static int THROTTLED = 1;
    public static int WAITING_FOR_STALLED_SOCKET_POOL = 2;
    public static int WAITING_FOR_AVAILABLE_SOCKET = 3;
    public static int WAITING_FOR_DELEGATE = 4;
    public static int WAITING_FOR_CACHE = 5;
    public static int WAITING_FOR_APPCACHE = 6;
    public static int DOWNLOADING_PROXY_SCRIPT = 7;
    public static int RESOLVING_PROXY_FOR_URL = 8;
    public static int RESOLVING_HOST_IN_PROXY_SCRIPT = 9;
    public static int ESTABLISHING_PROXY_TUNNEL = 10;
    public static int RESOLVING_HOST = 11;
    public static int CONNECTING = 12;
    public static int SSL_HANDSHAKE = 13;
    public static int SENDING_REQUEST = 14;
    public static int WAITING_FOR_RESPONSE = 15;
    public static int READING_RESPONSE = 16;
}
