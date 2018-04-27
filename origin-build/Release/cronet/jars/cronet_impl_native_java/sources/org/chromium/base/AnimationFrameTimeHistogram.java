package org.chromium.base;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeAnimator;
import android.animation.TimeAnimator.TimeListener;
import android.util.Log;
import org.chromium.base.annotations.MainDex;

@MainDex
public class AnimationFrameTimeHistogram {
    private static final int MAX_FRAME_TIME_NUM = 600;
    private static final String TAG = "AnimationFrameTimeHistogram";
    private final String mHistogramName;
    private final Recorder mRecorder = new Recorder();

    /* renamed from: org.chromium.base.AnimationFrameTimeHistogram$1 */
    class AnonymousClass1 extends AnimatorListenerAdapter {
        private final AnimationFrameTimeHistogram mAnimationFrameTimeHistogram = new AnimationFrameTimeHistogram(this.val$histogramName);
        final /* synthetic */ String val$histogramName;

        AnonymousClass1(String str) {
            this.val$histogramName = str;
        }

        public void onAnimationStart(Animator animation) {
            this.mAnimationFrameTimeHistogram.startRecording();
        }

        public void onAnimationEnd(Animator animation) {
            this.mAnimationFrameTimeHistogram.endRecording();
        }

        public void onAnimationCancel(Animator animation) {
            this.mAnimationFrameTimeHistogram.endRecording();
        }
    }

    private static class Recorder implements TimeListener {
        static final /* synthetic */ boolean $assertionsDisabled = (!AnimationFrameTimeHistogram.class.desiredAssertionStatus());
        private final TimeAnimator mAnimator;
        private int mFrameTimesCount;
        private long[] mFrameTimesMs;

        private Recorder() {
            this.mAnimator = new TimeAnimator();
            this.mAnimator.setTimeListener(this);
        }

        private void startRecording() {
            if ($assertionsDisabled || !this.mAnimator.isRunning()) {
                this.mFrameTimesCount = 0;
                this.mFrameTimesMs = new long[AnimationFrameTimeHistogram.MAX_FRAME_TIME_NUM];
                this.mAnimator.start();
                return;
            }
            throw new AssertionError();
        }

        private boolean endRecording() {
            boolean succeeded = this.mAnimator.isStarted();
            this.mAnimator.end();
            return succeeded;
        }

        private long[] getFrameTimesMs() {
            return this.mFrameTimesMs;
        }

        private int getFrameTimesCount() {
            return this.mFrameTimesCount;
        }

        private void cleanUp() {
            this.mFrameTimesMs = null;
        }

        public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {
            if (this.mFrameTimesCount == this.mFrameTimesMs.length) {
                this.mAnimator.end();
                cleanUp();
                Log.w(AnimationFrameTimeHistogram.TAG, "Animation frame time recording reached the maximum number. It's eitherthe animation took too long or recording end is not called.");
            } else if (deltaTime > 0) {
                long[] jArr = this.mFrameTimesMs;
                int i = this.mFrameTimesCount;
                this.mFrameTimesCount = i + 1;
                jArr[i] = deltaTime;
            }
        }
    }

    private native void nativeSaveHistogram(String str, long[] jArr, int i);

    public static AnimatorListener getAnimatorRecorder(String histogramName) {
        return new AnonymousClass1(histogramName);
    }

    public AnimationFrameTimeHistogram(String histogramName) {
        this.mHistogramName = histogramName;
    }

    public void startRecording() {
        this.mRecorder.startRecording();
    }

    public void endRecording() {
        if (this.mRecorder.endRecording()) {
            nativeSaveHistogram(this.mHistogramName, this.mRecorder.getFrameTimesMs(), this.mRecorder.getFrameTimesCount());
        }
        this.mRecorder.cleanUp();
    }
}
