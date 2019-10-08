package edu.gmu.cs475;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Do not change this file
 */
public abstract class AbstractKeyValueStoreEntry {
    public abstract ReentrantReadWriteLock getLock();
}
