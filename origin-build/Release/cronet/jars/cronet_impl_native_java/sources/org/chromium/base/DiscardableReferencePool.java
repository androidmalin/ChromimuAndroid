package org.chromium.base;

import android.support.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class DiscardableReferencePool {
    static final /* synthetic */ boolean $assertionsDisabled = (!DiscardableReferencePool.class.desiredAssertionStatus());
    private final Set<DiscardableReference<?>> mPool = Collections.newSetFromMap(new WeakHashMap());

    public static class DiscardableReference<T> {
        static final /* synthetic */ boolean $assertionsDisabled = (!DiscardableReferencePool.class.desiredAssertionStatus());
        @Nullable
        private T mPayload;

        private DiscardableReference(T payload) {
            if ($assertionsDisabled || payload != null) {
                this.mPayload = payload;
                return;
            }
            throw new AssertionError();
        }

        @Nullable
        public T get() {
            return this.mPayload;
        }

        private void discard() {
            if ($assertionsDisabled || this.mPayload != null) {
                this.mPayload = null;
                return;
            }
            throw new AssertionError();
        }
    }

    public <T> DiscardableReference<T> put(T payload) {
        if ($assertionsDisabled || payload != null) {
            DiscardableReference<T> reference = new DiscardableReference(payload);
            this.mPool.add(reference);
            return reference;
        }
        throw new AssertionError();
    }

    public void drain() {
        for (DiscardableReference<?> ref : this.mPool) {
            ref.discard();
        }
        this.mPool.clear();
    }
}
