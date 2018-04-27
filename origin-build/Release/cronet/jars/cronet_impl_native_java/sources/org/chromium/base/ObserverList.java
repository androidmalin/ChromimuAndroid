package org.chromium.base;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class ObserverList<E> implements Iterable<E> {
    static final /* synthetic */ boolean $assertionsDisabled = (!ObserverList.class.desiredAssertionStatus());
    private int mCount;
    private int mIterationDepth;
    private boolean mNeedsCompact;
    public final List<E> mObservers = new ArrayList();

    public interface RewindableIterator<E> extends Iterator<E> {
        void rewind();
    }

    private class ObserverListIterator implements RewindableIterator<E> {
        private int mIndex;
        private boolean mIsExhausted;
        private int mListEndMarker;

        private ObserverListIterator() {
            ObserverList.this.incrementIterationDepth();
            this.mListEndMarker = ObserverList.this.capacity();
        }

        public void rewind() {
            compactListIfNeeded();
            ObserverList.this.incrementIterationDepth();
            this.mListEndMarker = ObserverList.this.capacity();
            this.mIsExhausted = false;
            this.mIndex = 0;
        }

        public boolean hasNext() {
            int lookupIndex = this.mIndex;
            while (lookupIndex < this.mListEndMarker && ObserverList.this.getObserverAt(lookupIndex) == null) {
                lookupIndex++;
            }
            if (lookupIndex < this.mListEndMarker) {
                return true;
            }
            compactListIfNeeded();
            return false;
        }

        public E next() {
            while (this.mIndex < this.mListEndMarker && ObserverList.this.getObserverAt(this.mIndex) == null) {
                this.mIndex++;
            }
            if (this.mIndex < this.mListEndMarker) {
                ObserverList observerList = ObserverList.this;
                int i = this.mIndex;
                this.mIndex = i + 1;
                return observerList.getObserverAt(i);
            }
            compactListIfNeeded();
            throw new NoSuchElementException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        private void compactListIfNeeded() {
            if (!this.mIsExhausted) {
                this.mIsExhausted = true;
                ObserverList.this.decrementIterationDepthAndCompactIfNeeded();
            }
        }
    }

    public boolean addObserver(E obs) {
        if (obs == null || this.mObservers.contains(obs)) {
            return false;
        }
        boolean result = this.mObservers.add(obs);
        if ($assertionsDisabled || result) {
            this.mCount++;
            return true;
        }
        throw new AssertionError();
    }

    public boolean removeObserver(E obs) {
        if (obs == null) {
            return false;
        }
        int index = this.mObservers.indexOf(obs);
        if (index == -1) {
            return false;
        }
        if (this.mIterationDepth == 0) {
            this.mObservers.remove(index);
        } else {
            this.mNeedsCompact = true;
            this.mObservers.set(index, null);
        }
        this.mCount--;
        if ($assertionsDisabled || this.mCount >= 0) {
            return true;
        }
        throw new AssertionError();
    }

    public boolean hasObserver(E obs) {
        return this.mObservers.contains(obs);
    }

    public void clear() {
        int i = 0;
        this.mCount = 0;
        if (this.mIterationDepth == 0) {
            this.mObservers.clear();
            return;
        }
        int size = this.mObservers.size();
        boolean z = this.mNeedsCompact;
        if (size != 0) {
            i = 1;
        }
        this.mNeedsCompact = i | z;
        for (int i2 = 0; i2 < size; i2++) {
            this.mObservers.set(i2, null);
        }
    }

    public Iterator<E> iterator() {
        return new ObserverListIterator();
    }

    public RewindableIterator<E> rewindableIterator() {
        return new ObserverListIterator();
    }

    public int size() {
        return this.mCount;
    }

    public boolean isEmpty() {
        return this.mCount == 0;
    }

    private void compact() {
        if ($assertionsDisabled || this.mIterationDepth == 0) {
            for (int i = this.mObservers.size() - 1; i >= 0; i--) {
                if (this.mObservers.get(i) == null) {
                    this.mObservers.remove(i);
                }
            }
            return;
        }
        throw new AssertionError();
    }

    private void incrementIterationDepth() {
        this.mIterationDepth++;
    }

    private void decrementIterationDepthAndCompactIfNeeded() {
        this.mIterationDepth--;
        if (!$assertionsDisabled && this.mIterationDepth < 0) {
            throw new AssertionError();
        } else if (this.mIterationDepth <= 0 && this.mNeedsCompact) {
            this.mNeedsCompact = false;
            compact();
        }
    }

    private int capacity() {
        return this.mObservers.size();
    }

    private E getObserverAt(int index) {
        return this.mObservers.get(index);
    }
}
