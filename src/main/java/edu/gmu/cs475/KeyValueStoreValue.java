package edu.gmu.cs475;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Do not change this file
 */
public class KeyValueStoreValue extends AbstractKeyValueStoreEntry {
    private String value;
    private final ReentrantReadWriteLock lock;

    public KeyValueStoreValue(String startingValue) {
        lock = new ReentrantReadWriteLock();
        this.value = startingValue;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public ReentrantReadWriteLock getLock() {
        return lock;
    }
}
