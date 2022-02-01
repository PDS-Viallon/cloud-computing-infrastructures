package org.example.kvstore;

public interface Store<K,V> {

    V get(K k) throws Exception;

    V put(K k, V v) throws Exception;

}
