package org.chromium.net;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.text.TextUtils;
import java.io.IOException;
import org.chromium.base.ApplicationStatus;
import org.chromium.base.ContextUtils;
import org.chromium.base.Log;
import org.chromium.base.ThreadUtils;
import org.chromium.base.VisibleForTesting;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;

@JNINamespace("net::android")
public class HttpNegotiateAuthenticator {
    static final /* synthetic */ boolean $assertionsDisabled = (!HttpNegotiateAuthenticator.class.desiredAssertionStatus() ? true : $assertionsDisabled);
    private static final String TAG = "net_auth";
    private final String mAccountType;
    private Bundle mSpnegoContext;

    @VisibleForTesting
    class GetAccountsCallback implements AccountManagerCallback<Account[]> {
        private final RequestData mRequestData;

        public GetAccountsCallback(RequestData requestData) {
            this.mRequestData = requestData;
        }

        public void run(AccountManagerFuture<Account[]> future) {
            Exception e;
            try {
                Account[] accounts = (Account[]) future.getResult();
                if (accounts.length == 0) {
                    Log.w(HttpNegotiateAuthenticator.TAG, "ERR_MISSING_AUTH_CREDENTIALS: No account provided for the kerberos authentication. Please verify the configuration policies and that the CONTACTS runtime permission is granted. ", new Object[0]);
                    HttpNegotiateAuthenticator.this.nativeSetResult(this.mRequestData.nativeResultObject, NetError.ERR_MISSING_AUTH_CREDENTIALS, null);
                } else if (accounts.length > 1) {
                    Log.w(HttpNegotiateAuthenticator.TAG, "ERR_MISSING_AUTH_CREDENTIALS: Found %d accounts eligible for the kerberos authentication. Please fix the configuration by providing a single account.", Integer.valueOf(accounts.length));
                    HttpNegotiateAuthenticator.this.nativeSetResult(this.mRequestData.nativeResultObject, NetError.ERR_MISSING_AUTH_CREDENTIALS, null);
                } else if (HttpNegotiateAuthenticator.this.lacksPermission(ContextUtils.getApplicationContext(), "android.permission.USE_CREDENTIALS", true)) {
                    Log.e(HttpNegotiateAuthenticator.TAG, "ERR_MISCONFIGURED_AUTH_ENVIRONMENT: USE_CREDENTIALS permission not granted. Aborting authentication.", new Object[0]);
                    HttpNegotiateAuthenticator.this.nativeSetResult(this.mRequestData.nativeResultObject, NetError.ERR_MISCONFIGURED_AUTH_ENVIRONMENT, null);
                } else {
                    this.mRequestData.account = accounts[0];
                    this.mRequestData.accountManager.getAuthToken(this.mRequestData.account, this.mRequestData.authTokenType, this.mRequestData.options, true, new GetTokenCallback(this.mRequestData), new Handler(ThreadUtils.getUiThreadLooper()));
                }
            } catch (OperationCanceledException e2) {
                e = e2;
                Log.w(HttpNegotiateAuthenticator.TAG, "ERR_UNEXPECTED: Error while attempting to retrieve accounts.", e);
                HttpNegotiateAuthenticator.this.nativeSetResult(this.mRequestData.nativeResultObject, -9, null);
            } catch (AuthenticatorException e3) {
                e = e3;
                Log.w(HttpNegotiateAuthenticator.TAG, "ERR_UNEXPECTED: Error while attempting to retrieve accounts.", e);
                HttpNegotiateAuthenticator.this.nativeSetResult(this.mRequestData.nativeResultObject, -9, null);
            } catch (IOException e4) {
                e = e4;
                Log.w(HttpNegotiateAuthenticator.TAG, "ERR_UNEXPECTED: Error while attempting to retrieve accounts.", e);
                HttpNegotiateAuthenticator.this.nativeSetResult(this.mRequestData.nativeResultObject, -9, null);
            }
        }
    }

    @VisibleForTesting
    class GetTokenCallback implements AccountManagerCallback<Bundle> {
        private final RequestData mRequestData;

        public GetTokenCallback(RequestData requestData) {
            this.mRequestData = requestData;
        }

        public void run(AccountManagerFuture<Bundle> future) {
            Exception e;
            try {
                Bundle result = (Bundle) future.getResult();
                if (result.containsKey("intent")) {
                    final Context appContext = ContextUtils.getApplicationContext();
                    appContext.registerReceiver(new BroadcastReceiver() {
                        public void onReceive(Context context, Intent intent) {
                            appContext.unregisterReceiver(this);
                            GetTokenCallback.this.mRequestData.accountManager.getAuthToken(GetTokenCallback.this.mRequestData.account, GetTokenCallback.this.mRequestData.authTokenType, GetTokenCallback.this.mRequestData.options, true, new GetTokenCallback(GetTokenCallback.this.mRequestData), null);
                        }
                    }, new IntentFilter("android.accounts.LOGIN_ACCOUNTS_CHANGED"));
                    return;
                }
                HttpNegotiateAuthenticator.this.processResult(result, this.mRequestData);
            } catch (OperationCanceledException e2) {
                e = e2;
                Log.w(HttpNegotiateAuthenticator.TAG, "ERR_UNEXPECTED: Error while attempting to obtain a token.", e);
                HttpNegotiateAuthenticator.this.nativeSetResult(this.mRequestData.nativeResultObject, -9, null);
            } catch (AuthenticatorException e3) {
                e = e3;
                Log.w(HttpNegotiateAuthenticator.TAG, "ERR_UNEXPECTED: Error while attempting to obtain a token.", e);
                HttpNegotiateAuthenticator.this.nativeSetResult(this.mRequestData.nativeResultObject, -9, null);
            } catch (IOException e4) {
                e = e4;
                Log.w(HttpNegotiateAuthenticator.TAG, "ERR_UNEXPECTED: Error while attempting to obtain a token.", e);
                HttpNegotiateAuthenticator.this.nativeSetResult(this.mRequestData.nativeResultObject, -9, null);
            }
        }
    }

    static class RequestData {
        public Account account;
        public AccountManager accountManager;
        public String authTokenType;
        public long nativeResultObject;
        public Bundle options;

        RequestData() {
        }
    }

    @VisibleForTesting
    native void nativeSetResult(long j, int i, String str);

    protected HttpNegotiateAuthenticator(String accountType) {
        if ($assertionsDisabled || !TextUtils.isEmpty(accountType)) {
            this.mAccountType = accountType;
            return;
        }
        throw new AssertionError();
    }

    @CalledByNative
    @VisibleForTesting
    static HttpNegotiateAuthenticator create(String accountType) {
        return new HttpNegotiateAuthenticator(accountType);
    }

    @CalledByNative
    @VisibleForTesting
    void getNextAuthToken(long nativeResultObject, String principal, String authToken, boolean canDelegate) {
        if ($assertionsDisabled || principal != null) {
            Context applicationContext = ContextUtils.getApplicationContext();
            RequestData requestData = new RequestData();
            requestData.authTokenType = HttpNegotiateConstants.SPNEGO_TOKEN_TYPE_BASE + principal;
            requestData.accountManager = AccountManager.get(applicationContext);
            requestData.nativeResultObject = nativeResultObject;
            String[] features = new String[]{HttpNegotiateConstants.SPNEGO_FEATURE};
            requestData.options = new Bundle();
            if (authToken != null) {
                requestData.options.putString(HttpNegotiateConstants.KEY_INCOMING_AUTH_TOKEN, authToken);
            }
            if (this.mSpnegoContext != null) {
                requestData.options.putBundle(HttpNegotiateConstants.KEY_SPNEGO_CONTEXT, this.mSpnegoContext);
            }
            requestData.options.putBoolean(HttpNegotiateConstants.KEY_CAN_DELEGATE, canDelegate);
            Activity activity = ApplicationStatus.getLastTrackedFocusedActivity();
            if (activity == null) {
                requestTokenWithoutActivity(applicationContext, requestData, features);
                return;
            } else {
                requestTokenWithActivity(applicationContext, activity, requestData, features);
                return;
            }
        }
        throw new AssertionError();
    }

    private void processResult(Bundle result, RequestData requestData) {
        int status;
        this.mSpnegoContext = result.getBundle(HttpNegotiateConstants.KEY_SPNEGO_CONTEXT);
        switch (result.getInt(HttpNegotiateConstants.KEY_SPNEGO_RESULT, 1)) {
            case 0:
                status = 0;
                break;
            case 1:
                status = -9;
                break;
            case 2:
                status = -3;
                break;
            case 3:
                status = NetError.ERR_UNEXPECTED_SECURITY_LIBRARY_STATUS;
                break;
            case 4:
                status = NetError.ERR_INVALID_RESPONSE;
                break;
            case 5:
                status = NetError.ERR_INVALID_AUTH_CREDENTIALS;
                break;
            case 6:
                status = NetError.ERR_UNSUPPORTED_AUTH_SCHEME;
                break;
            case 7:
                status = NetError.ERR_MISSING_AUTH_CREDENTIALS;
                break;
            case 8:
                status = NetError.ERR_UNDOCUMENTED_SECURITY_LIBRARY_STATUS;
                break;
            case 9:
                status = NetError.ERR_MALFORMED_IDENTITY;
                break;
            default:
                status = -9;
                break;
        }
        nativeSetResult(requestData.nativeResultObject, status, result.getString("authtoken"));
    }

    private void requestTokenWithoutActivity(Context ctx, RequestData requestData, String[] features) {
        if (lacksPermission(ctx, "android.permission.GET_ACCOUNTS", true)) {
            Log.e(TAG, "ERR_MISCONFIGURED_AUTH_ENVIRONMENT: GET_ACCOUNTS permission not granted. Aborting authentication.", new Object[0]);
            nativeSetResult(requestData.nativeResultObject, NetError.ERR_MISCONFIGURED_AUTH_ENVIRONMENT, null);
            return;
        }
        requestData.accountManager.getAccountsByTypeAndFeatures(this.mAccountType, features, new GetAccountsCallback(requestData), new Handler(ThreadUtils.getUiThreadLooper()));
    }

    private void requestTokenWithActivity(Context ctx, Activity activity, RequestData requestData, String[] features) {
        String permission;
        boolean isPreM = VERSION.SDK_INT < 23 ? true : $assertionsDisabled;
        if (isPreM) {
            permission = "android.permission.MANAGE_ACCOUNTS";
        } else {
            permission = "android.permission.GET_ACCOUNTS";
        }
        if (lacksPermission(ctx, permission, isPreM)) {
            Log.e(TAG, "ERR_MISCONFIGURED_AUTH_ENVIRONMENT: %s permission not granted. Aborting authentication", permission);
            nativeSetResult(requestData.nativeResultObject, NetError.ERR_MISCONFIGURED_AUTH_ENVIRONMENT, null);
            return;
        }
        requestData.accountManager.getAuthTokenByFeatures(this.mAccountType, requestData.authTokenType, features, activity, null, requestData.options, new GetTokenCallback(requestData), new Handler(ThreadUtils.getUiThreadLooper()));
    }

    @VisibleForTesting
    boolean lacksPermission(Context context, String permission, boolean onlyPreM) {
        if ((!onlyPreM || VERSION.SDK_INT < 23) && context.checkPermission(permission, Process.myPid(), Process.myUid()) != 0) {
            return true;
        }
        return $assertionsDisabled;
    }
}
