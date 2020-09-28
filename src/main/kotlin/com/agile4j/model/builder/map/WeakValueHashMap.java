package com.agile4j.model.builder.map;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 注意，不建议作为长生命周期对象使用。否则value中可能有大量值为null的弱引用
 * @author liurenpeng
 * Created on 2020-09-27
 */
public class WeakValueHashMap<K, V> implements Map<K, V> {

    private final Map<K, WeakValueReference<V>> map;

    public WeakValueHashMap() {
        this.map = new HashMap<>();
    }

    @Override
    public void clear() {
        this.map.clear();
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        return map.values().stream()
                .map(reference -> reference != null ? reference.get() : null)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return map.entrySet().stream()
                .map(e -> new WeakValueEntry<>(e.getKey(),
                        e.getValue() != null ? e.getValue().get() : null))
                .collect(Collectors.toSet());
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(new WeakValueReference<>(value));
    }

    @Override
    public V get(Object key) {
        WeakValueReference<V> reference = this.map.get(key);
        return reference != null ? reference.get() : null;
    }

    @Override
    public V put(K key, V value) {
        WeakValueReference<V> reference = this.map.put(key, new WeakValueReference<>(value));
        return reference != null ? reference.get() : null;
    }

    @Override
    public V remove(Object key) {
        WeakValueReference<V> reference = this.map.remove(key);
        return reference != null ? reference.get() : null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {

    }

    @Override
    public String toString() {
        return map.toString();
    }

    static class WeakValueReference<V> extends WeakReference<V> {

        public WeakValueReference(V referent) {
            super(referent);
        }

        public WeakValueReference(V referent, ReferenceQueue<? super V> q) {
            super(referent, q);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof WeakReference) {
                WeakReference<?> w = (WeakReference<?>) o;
                return Objects.equals(get(), w.get());
            }
            return false;
        }
    }

    static class WeakValueEntry<K, V> implements Map.Entry<K, V> {

        private K key;
        private V value;

        WeakValueEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public final K getKey() {
            return key;
        }

        public final V getValue() {
            return value;
        }

        public final String toString() {
            return key + "=" + value;
        }

        public final int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        public final boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof Map.Entry) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                return Objects.equals(key, e.getKey()) && Objects.equals(value, e.getValue());
            }
            return false;
        }
    }
}
