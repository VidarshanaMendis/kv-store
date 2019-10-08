package edu.gmu.cs475;

import java.awt.*;
import java.nio.file.DirectoryNotEmptyException;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implement your assignment here.
 */
public class KeyValueStore extends AbstractKeyValueStore {


    /**
     * Retrieve an element from this key value store
     *
     * @param key the key to retrieve
     * @return The value mapped to this key, if one exists, otherwise null
     * @throws NullPointerException     if key is null
     * @throws IllegalArgumentException if the key represents a directory (ends in /)
     * @throws IllegalArgumentException if the key does not start with a /
     */
    @Override
    public String get(String key) {
        System.out.println("GET begin");
        if (key == null) {
            throw new NullPointerException("key is null in get");
        } else if (!key.startsWith("/") || key.endsWith("/")) {
            throw new IllegalArgumentException("Error: Illegal Key value. (get)");
        }

        KeyValueStoreValue value = (KeyValueStoreValue) _get(key);
        if (value == null) {
            return null;
        }
        value.getLock().writeLock().lock();
        String result = null;
        try{
            result = value.getValue();
        } finally {
            value.getLock().writeLock().unlock();
        }
        return result;
    }

    /**
     * Lists all of the keys that are currently known to this key-value store
     *
     * @return A set containing all currently valid keys
     */
    @Override
    public Set<String> listKeys() {
        Set<String> result = _listKeys();            
        return result;
    }

    /**
     * Sets a key to be the given value
     *
     * @param key   key to set
     * @param value value to store
     * @throws NullPointerException     if key or value is null
     * @throws IllegalArgumentException if key does not start with a /
     * @throws IllegalArgumentException if key represents a direcotry (ends with a /)
     */
    @Override
     public synchronized void set(String key, String value) {
        if (key == null || value == null) {
            throw new NullPointerException("key or value was null in set");
        } else if (!key.startsWith("/") || key.endsWith("/")) {
            throw new IllegalArgumentException("Error: Illegal Key value. (set)");
        }

        KeyValueStoreValue valueNode = (KeyValueStoreValue) _get(key);
        if (valueNode == null) {
            valueNode = new KeyValueStoreValue(value);
        }

        try {
            String[] directories = key.split("/");
            if (_get("/") == null) {
                _set("/", new KeyValueStoreDirectory("/"));
                //System.out.println("SET directory: /");
            }

            String parentDirectory = "/";
            String child = "/";
            for (int i = 0; i < directories.length - 1; i++) {
                child = child + directories[i + 1];
                if (i != directories.length - 2) {
                    child = child + "/";
                }
                valueNode.getLock().writeLock().lock();
                if (_get(parentDirectory) == null) {
                    _set(parentDirectory, new KeyValueStoreDirectory(parentDirectory));
                    //System.out.println("SET directory: " + parentDirectory);
                }

                List<String> children = ((KeyValueStoreDirectory) _get(parentDirectory)).getChildren();
                if (!children.contains(child)) {
                    children.add(child);
                }
                parentDirectory = child;
            }

            _set(key, valueNode);
        }finally {
            valueNode.getLock().writeLock().unlock();
        }

    }

    /**
     * Removes this key and any values stored at it from the map
     *
     * @param key key to remove
     * @return true if the key was successfully deleted, or false if it did not exist
     * @throws NullPointerException     if key is null
     * @throws IllegalArgumentException if key does not start with a /
     * @throws IllegalArgumentException if key represents a directory (ends with a /)
     */
    @Override
    public boolean remove(String key) {
        if (key == null) {
            throw new NullPointerException();
        } else if (!key.startsWith("/") || key.endsWith("/")) {
            throw new IllegalArgumentException("Error: Illegal Key value. (remove)");
        }

        String[] pathArray = key.split("/");
        //System.out.println("REMOVE array length = " + pathArray.length);
        String path = "";
        for (int i = 0; i < pathArray.length - 1; i++) {
            //System.out.println("REMOVE i = " + i);
            path = path + pathArray[i] + "/";
            //System.out.println("REMOVE path is = " + path);
        }
        boolean res = false;
        KeyValueStoreValue value = (KeyValueStoreValue) _get(key);

        if(value != null){
            value.getLock().writeLock().lock();
        }
        else{
            while(true){
                value = (KeyValueStoreValue) _get(key);
                if(value!=null){
                    value.getLock().writeLock().lock();
                    break;
                }
            }
        }

        try{
            List<String> children = ((KeyValueStoreDirectory) _get(path)).getChildren();
            children.remove(key);

            res =  _remove(key);

        }catch (Exception e){

        }
        finally {
            value.getLock().writeLock().unlock();
        }
        return res;
    }

    /**
     * Lists the contents of a directory (non-recursively)
     *
     * @param directory path of the directory
     * @return unsorted set of the files and directories inside of this directory, or null if the directory doesn't exist.
     * @throws NullPointerException     if key is null
     * @throws IllegalArgumentException if the key does not represent a directory (does not end in a /)
     * @throws IllegalArgumentException if the key does not start with a /
     */
    @Override
    public Set<String> listDirectory(String directory) {
        if (directory == null) {
            throw new NullPointerException();
        } else if (!directory.startsWith("/") || !directory.endsWith("/")) {
            throw new IllegalArgumentException("Error: Illegal Key value. (listDirectory)");
        }

        KeyValueStoreDirectory directoryContents = (KeyValueStoreDirectory)_get(directory);
        directoryContents.getLock().writeLock().lock();
        Set<String> result;
        try{
            result = new HashSet<String>();
            result.addAll(directoryContents.getChildren());
        }finally {
            directoryContents.getLock().writeLock().unlock();
        }
        return result;
    }

    /**
     * Deletes an empty directory
     *
     * @param directory path of the directory
     * @throws IllegalArgumentException   if the key does not represent a directory (does not end in a /)
     * @throws IllegalArgumentException   if the key does not start with a /
     * @throws DirectoryNotEmptyException if the directory is not empty
     */
    @Override
    public void removeDirectory(String directory) throws DirectoryNotEmptyException {
        if (directory == null) {
            throw new NullPointerException();
        } else if (!directory.startsWith("/") || !directory.endsWith("/")) {
            throw new IllegalArgumentException("Error: Illegal Key value. (removeDirectory)");
        } else if (!listDirectory(directory).isEmpty()) {
            throw new DirectoryNotEmptyException("Error: the given directory is not empty.");
        }

        String[] pathArray = directory.split("/");
        String path = "";
        for (int i = 0; i < pathArray.length - 1; i++) {
            path = path + pathArray[i] + "/";
        }

        //System.out.println("REMOVE directory is = " + directory);
        //System.out.println("REMOVE path is = " + path);

        KeyValueStoreDirectory directoryValue = (KeyValueStoreDirectory) _get(path);
        if (directoryValue != null) {
            directoryValue.getLock().writeLock().lock();
            List<String> children = directoryValue.getChildren();
            children.remove(directory);
            directoryValue.getLock().writeLock().unlock();
        }

        _remove(directory);
    }
}
