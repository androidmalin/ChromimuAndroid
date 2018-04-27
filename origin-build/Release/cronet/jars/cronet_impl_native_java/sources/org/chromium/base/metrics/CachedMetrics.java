package org.chromium.base.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.chromium.base.library_loader.LibraryLoader;

public class CachedMetrics {

    private static abstract class CachedMetric {
        static final /* synthetic */ boolean $assertionsDisabled = (!CachedMetrics.class.desiredAssertionStatus());
        private static final List<CachedMetric> sMetrics = new ArrayList();
        protected boolean mCached;
        protected final String mName;

        protected abstract void commitAndClear();

        protected CachedMetric(String name) {
            this.mName = name;
        }

        protected final void addToCache() {
            if (!$assertionsDisabled && !Thread.holdsLock(sMetrics)) {
                throw new AssertionError();
            } else if (!this.mCached) {
                sMetrics.add(this);
                this.mCached = true;
            }
        }
    }

    public static class ActionEvent extends CachedMetric {
        private int mCount;

        public ActionEvent(String actionName) {
            super(actionName);
        }

        public void record() {
            synchronized (CachedMetric.sMetrics) {
                if (LibraryLoader.isInitialized()) {
                    recordWithNative();
                } else {
                    this.mCount++;
                    addToCache();
                }
            }
        }

        private void recordWithNative() {
            RecordUserAction.record(this.mName);
        }

        protected void commitAndClear() {
            while (this.mCount > 0) {
                recordWithNative();
                this.mCount--;
            }
        }
    }

    public static class BooleanHistogramSample extends CachedMetric {
        private final List<Boolean> mSamples = new ArrayList();

        public BooleanHistogramSample(String histogramName) {
            super(histogramName);
        }

        public void record(boolean sample) {
            synchronized (CachedMetric.sMetrics) {
                if (LibraryLoader.isInitialized()) {
                    recordWithNative(sample);
                } else {
                    this.mSamples.add(Boolean.valueOf(sample));
                    addToCache();
                }
            }
        }

        private void recordWithNative(boolean sample) {
            RecordHistogram.recordBooleanHistogram(this.mName, sample);
        }

        protected void commitAndClear() {
            for (Boolean sample : this.mSamples) {
                recordWithNative(sample.booleanValue());
            }
            this.mSamples.clear();
        }
    }

    public static class EnumeratedHistogramSample extends CachedMetric {
        private final int mMaxValue;
        private final List<Integer> mSamples = new ArrayList();

        public EnumeratedHistogramSample(String histogramName, int maxValue) {
            super(histogramName);
            this.mMaxValue = maxValue;
        }

        public void record(int sample) {
            synchronized (CachedMetric.sMetrics) {
                if (LibraryLoader.isInitialized()) {
                    recordWithNative(sample);
                } else {
                    this.mSamples.add(Integer.valueOf(sample));
                    addToCache();
                }
            }
        }

        private void recordWithNative(int sample) {
            RecordHistogram.recordEnumeratedHistogram(this.mName, sample, this.mMaxValue);
        }

        protected void commitAndClear() {
            for (Integer sample : this.mSamples) {
                recordWithNative(sample.intValue());
            }
            this.mSamples.clear();
        }
    }

    public static class SparseHistogramSample extends CachedMetric {
        private final List<Integer> mSamples = new ArrayList();

        public SparseHistogramSample(String histogramName) {
            super(histogramName);
        }

        public void record(int sample) {
            synchronized (CachedMetric.sMetrics) {
                if (LibraryLoader.isInitialized()) {
                    recordWithNative(sample);
                } else {
                    this.mSamples.add(Integer.valueOf(sample));
                    addToCache();
                }
            }
        }

        private void recordWithNative(int sample) {
            RecordHistogram.recordSparseSlowlyHistogram(this.mName, sample);
        }

        protected void commitAndClear() {
            for (Integer sample : this.mSamples) {
                recordWithNative(sample.intValue());
            }
            this.mSamples.clear();
        }
    }

    public static class TimesHistogramSample extends CachedMetric {
        private final List<Long> mSamples = new ArrayList();
        private final TimeUnit mTimeUnit;

        public TimesHistogramSample(String histogramName, TimeUnit timeUnit) {
            super(histogramName);
            this.mTimeUnit = timeUnit;
        }

        public void record(long sample) {
            synchronized (CachedMetric.sMetrics) {
                if (LibraryLoader.isInitialized()) {
                    recordWithNative(sample);
                } else {
                    this.mSamples.add(Long.valueOf(sample));
                    addToCache();
                }
            }
        }

        private void recordWithNative(long sample) {
            RecordHistogram.recordTimesHistogram(this.mName, sample, this.mTimeUnit);
        }

        protected void commitAndClear() {
            for (Long sample : this.mSamples) {
                recordWithNative(sample.longValue());
            }
            this.mSamples.clear();
        }
    }

    public static void commitCachedMetrics() {
        synchronized (CachedMetric.sMetrics) {
            for (CachedMetric metric : CachedMetric.sMetrics) {
                metric.commitAndClear();
            }
        }
    }
}
