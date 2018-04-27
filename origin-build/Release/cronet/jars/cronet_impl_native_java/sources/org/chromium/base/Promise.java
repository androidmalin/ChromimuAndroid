package org.chromium.base;

import android.os.Handler;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.LinkedList;
import java.util.List;

public class Promise<T> {
    static final /* synthetic */ boolean $assertionsDisabled = (!Promise.class.desiredAssertionStatus() ? true : $assertionsDisabled);
    private static final int FULFILLED = 1;
    private static final int REJECTED = 2;
    private static final int UNFULFILLED = 0;
    private final List<Callback<T>> mFulfillCallbacks = new LinkedList();
    private final Handler mHandler = new Handler();
    private final List<Callback<Exception>> mRejectCallbacks = new LinkedList();
    private Exception mRejectReason;
    private T mResult;
    private int mState = 0;
    private final Thread mThread = Thread.currentThread();
    private boolean mThrowingRejectionHandler;

    public interface AsyncFunction<A, R> {
        Promise<R> apply(A a);
    }

    public interface Function<A, R> {
        R apply(A a);
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface PromiseState {
    }

    public static class UnhandledRejectionException extends RuntimeException {
        public UnhandledRejectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static /* synthetic */ void lambda$then$2(org.chromium.base.Promise.AsyncFunction r1, org.chromium.base.Promise r2, java.lang.Object r3) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception: Unknown instruction: 'invoke-custom' in method: org.chromium.base.Promise.lambda$then$2(org.chromium.base.Promise$AsyncFunction, org.chromium.base.Promise, java.lang.Object):void, dex: 
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:282)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: 'invoke-custom'
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:590)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:74)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 5 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: org.chromium.base.Promise.lambda$then$2(org.chromium.base.Promise$AsyncFunction, org.chromium.base.Promise, java.lang.Object):void");
    }

    private <S> void postCallbackToLooper(org.chromium.base.Callback<S> r1, S r2) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception: Unknown instruction: 'invoke-custom' in method: org.chromium.base.Promise.postCallbackToLooper(org.chromium.base.Callback, java.lang.Object):void, dex: 
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:282)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: 'invoke-custom'
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:590)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:74)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 5 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: org.chromium.base.Promise.postCallbackToLooper(org.chromium.base.Callback, java.lang.Object):void");
    }

    public <R> org.chromium.base.Promise<R> then(org.chromium.base.Promise.AsyncFunction<T, R> r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception: Unknown instruction: 'invoke-custom' in method: org.chromium.base.Promise.then(org.chromium.base.Promise$AsyncFunction):org.chromium.base.Promise<R>, dex: 
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:282)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: 'invoke-custom'
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:590)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:74)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 5 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: org.chromium.base.Promise.then(org.chromium.base.Promise$AsyncFunction):org.chromium.base.Promise<R>");
    }

    public <R> org.chromium.base.Promise<R> then(org.chromium.base.Promise.Function<T, R> r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception: Unknown instruction: 'invoke-custom' in method: org.chromium.base.Promise.then(org.chromium.base.Promise$Function):org.chromium.base.Promise<R>, dex: 
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:282)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: 'invoke-custom'
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:590)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:74)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 5 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: org.chromium.base.Promise.then(org.chromium.base.Promise$Function):org.chromium.base.Promise<R>");
    }

    public void then(org.chromium.base.Callback<T> r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception: Unknown instruction: 'invoke-custom' in method: org.chromium.base.Promise.then(org.chromium.base.Callback):void, dex: 
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:282)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: 'invoke-custom'
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:590)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:74)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 5 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: org.chromium.base.Promise.then(org.chromium.base.Callback):void");
    }

    private static /* synthetic */ void lambda$then$0(Exception reason) {
        throw new UnhandledRejectionException("Promise was rejected without a rejection handler.", reason);
    }

    public void then(Callback<T> onFulfill, Callback<Exception> onReject) {
        checkThread();
        thenInner(onFulfill);
        exceptInner(onReject);
    }

    public void except(Callback<Exception> onReject) {
        checkThread();
        exceptInner(onReject);
    }

    private void thenInner(Callback<T> onFulfill) {
        if (this.mState == 1) {
            postCallbackToLooper(onFulfill, this.mResult);
        } else if (this.mState == 0) {
            this.mFulfillCallbacks.add(onFulfill);
        }
    }

    private void exceptInner(Callback<Exception> onReject) {
        if (!$assertionsDisabled && this.mThrowingRejectionHandler) {
            throw new AssertionError("Do not add an exception handler to a Promise you have called the single argument Promise.then(Callback) on.");
        } else if (this.mState == 2) {
            postCallbackToLooper(onReject, this.mRejectReason);
        } else if (this.mState == 0) {
            this.mRejectCallbacks.add(onReject);
        }
    }

    private static /* synthetic */ void lambda$then$1(Promise promise, Function function, Object result) {
        try {
            promise.fulfill(function.apply(result));
        } catch (Exception e) {
            promise.reject(e);
        }
    }

    public void fulfill(T result) {
        checkThread();
        if ($assertionsDisabled || this.mState == 0) {
            this.mState = 1;
            this.mResult = result;
            for (Callback<T> callback : this.mFulfillCallbacks) {
                postCallbackToLooper(callback, result);
            }
            this.mFulfillCallbacks.clear();
            return;
        }
        throw new AssertionError();
    }

    public void reject(Exception reason) {
        checkThread();
        if ($assertionsDisabled || this.mState == 0) {
            this.mState = 2;
            this.mRejectReason = reason;
            for (Callback<Exception> callback : this.mRejectCallbacks) {
                postCallbackToLooper(callback, reason);
            }
            this.mRejectCallbacks.clear();
            return;
        }
        throw new AssertionError();
    }

    public void reject() {
        reject(null);
    }

    public boolean isFulfilled() {
        checkThread();
        if (this.mState == 1) {
            return true;
        }
        return $assertionsDisabled;
    }

    public boolean isRejected() {
        checkThread();
        return this.mState == 2 ? true : $assertionsDisabled;
    }

    public T getResult() {
        if ($assertionsDisabled || isFulfilled()) {
            return this.mResult;
        }
        throw new AssertionError();
    }

    public static <T> Promise<T> fulfilled(T result) {
        Promise<T> promise = new Promise();
        promise.fulfill(result);
        return promise;
    }

    private void checkThread() {
        if (!$assertionsDisabled && this.mThread != Thread.currentThread()) {
            throw new AssertionError("Promise must only be used on a single Thread.");
        }
    }
}
