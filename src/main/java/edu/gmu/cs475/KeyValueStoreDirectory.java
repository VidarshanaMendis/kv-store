package edu.gmu.cs475;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Do not change this file
 */
public class KeyValueStoreDirectory extends AbstractKeyValueStoreEntry {
    private final ReentrantReadWriteLock lock;
    private final String directoryPath;
    private final List<String> children;

    public KeyValueStoreDirectory(String directoryPath) {
        this.directoryPath = directoryPath;
        this.children = new LinkedList<>();
        this.lock = new ReentrantReadWriteLock();
    }

    public List<String> getChildren() {
        return children;
    }

    public ReentrantReadWriteLock getLock() {
        return lock;
    }

    @Override
    public String toString() {
        return "DirectoryNode{" +
                " directoryPath='" + directoryPath + '\'' +
                ", children=" + children +
                '}';
    }

    public String getDirectoryPath() {
        return directoryPath;
    }
}
