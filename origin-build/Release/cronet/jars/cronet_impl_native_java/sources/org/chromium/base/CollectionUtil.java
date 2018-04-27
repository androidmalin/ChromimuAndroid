package org.chromium.base;

import android.util.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

public final class CollectionUtil {
    private CollectionUtil() {
    }

    @SafeVarargs
    public static <E> HashSet<E> newHashSet(E... elements) {
        HashSet<E> set = new HashSet(elements.length);
        Collections.addAll(set, elements);
        return set;
    }

    @SafeVarargs
    public static <E> ArrayList<E> newArrayList(E... elements) {
        ArrayList<E> list = new ArrayList(elements.length);
        Collections.addAll(list, elements);
        return list;
    }

    @VisibleForTesting
    public static <E> ArrayList<E> newArrayList(Iterable<E> iterable) {
        ArrayList<E> list = new ArrayList();
        for (E element : iterable) {
            list.add(element);
        }
        return list;
    }

    @SafeVarargs
    public static <K, V> HashMap<K, V> newHashMap(Pair<? extends K, ? extends V>... entries) {
        HashMap<K, V> map = new HashMap();
        for (Pair<? extends K, ? extends V> entry : entries) {
            map.put(entry.first, entry.second);
        }
        return map;
    }

    public static <T> void forEach(Collection<? extends T> collection, Callback<T> worker) {
        for (T entry : collection) {
            worker.onResult(entry);
        }
    }

    public static <K, V> void forEach(Map<? extends K, ? extends V> map, Callback<Entry<K, V>> worker) {
        for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
            worker.onResult(entry);
        }
    }
}
