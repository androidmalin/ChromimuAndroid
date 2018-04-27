package org.chromium.net.urlconnection;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

class MessageLoop implements Executor {
    static final /* synthetic */ boolean $assertionsDisabled = (!MessageLoop.class.desiredAssertionStatus() ? true : $assertionsDisabled);
    private static final long INVALID_THREAD_ID = -1;
    private boolean mLoopFailed = $assertionsDisabled;
    private boolean mLoopRunning = $assertionsDisabled;
    private final BlockingQueue<Runnable> mQueue = new LinkedBlockingQueue();
    private long mThreadId = INVALID_THREAD_ID;

    MessageLoop() {
    }

    private boolean calledOnValidThread() {
        if (this.mThreadId == INVALID_THREAD_ID) {
            this.mThreadId = Thread.currentThread().getId();
            return true;
        } else if (this.mThreadId != Thread.currentThread().getId()) {
            return $assertionsDisabled;
        } else {
            return true;
        }
    }

    private Runnable take(boolean useTimeout, long timeoutNano) throws InterruptedIOException {
        Runnable task;
        if (useTimeout) {
            task = (Runnable) this.mQueue.poll(timeoutNano, TimeUnit.NANOSECONDS);
        } else {
            try {
                task = (Runnable) this.mQueue.take();
            } catch (InterruptedException e) {
                InterruptedIOException exception = new InterruptedIOException();
                exception.initCause(e);
                throw exception;
            }
        }
        if (task != null) {
            return task;
        }
        throw new SocketTimeoutException();
    }

    public void loop() throws IOException {
        loop(0);
    }

    public void loop(int timeoutMilli) throws IOException {
        Exception e;
        if ($assertionsDisabled || calledOnValidThread()) {
            long startNano = System.nanoTime();
            long timeoutNano = TimeUnit.NANOSECONDS.convert((long) timeoutMilli, TimeUnit.MILLISECONDS);
            if (this.mLoopFailed) {
                throw new IllegalStateException("Cannot run loop as an exception has occurred previously.");
            } else if (this.mLoopRunning) {
                throw new IllegalStateException("Cannot run loop when it is already running.");
            } else {
                this.mLoopRunning = true;
                while (this.mLoopRunning) {
                    if (timeoutMilli == 0) {
                        try {
                            take($assertionsDisabled, 0).run();
                        } catch (InterruptedIOException e2) {
                            e = e2;
                        } catch (RuntimeException e3) {
                            e = e3;
                        }
                    } else {
                        take(true, (timeoutNano - System.nanoTime()) + startNano).run();
                    }
                }
                return;
            }
        }
        throw new AssertionError();
        this.mLoopRunning = $assertionsDisabled;
        this.mLoopFailed = true;
        throw e;
    }

    public void quit() {
        if ($assertionsDisabled || calledOnValidThread()) {
            this.mLoopRunning = $assertionsDisabled;
            return;
        }
        throw new AssertionError();
    }

    public void execute(Runnable task) throws RejectedExecutionException {
        if (task == null) {
            throw new IllegalArgumentException();
        }
        try {
            this.mQueue.put(task);
        } catch (InterruptedException e) {
            throw new RejectedExecutionException(e);
        }
    }

    public boolean isRunning() {
        return this.mLoopRunning;
    }

    public boolean hasLoopFailed() {
        return this.mLoopFailed;
    }
}
