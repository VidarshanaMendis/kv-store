package edu.gmu.cs475;

import java.nio.file.DirectoryNotEmptyException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Do not modify this file.
 */
public abstract class AbstractKeyValueStore {

	private final HashMap<String, AbstractKeyValueStoreEntry> map = new HashMap<>();

	/**
	 * Retrieve an element from this key value store
	 *
	 * @param key the key to retrieve
	 * @return The value mapped to this key, if one exists, otherwise null
	 * @throws NullPointerException if key is null
	 * @throws IllegalArgumentException if the key represents a directory (ends in /)
	 * @throws IllegalArgumentException if the key does not start with a /
	 */
	public abstract String get(String key);

	/**
	 * Lists all of the keys that are currently known to this key-value store
	 *
	 * @return A set containing all currently valid keys
	 */
	public abstract Set<String> listKeys();

	/**
	 * Sets a key to be the given value
	 *
	 * @param key   key to set
	 * @param value value to store
	 * @throws NullPointerException     if key or value is null
	 * @throws IllegalArgumentException if key does not start with a /
	 * @throws IllegalArgumentException if key represents a direcotry (ends with a /)
	 */
	public abstract void set(String key, String value);

	/**
	 * Removes this key and any values stored at it from the map
	 *
	 * @param key key to remove
	 * @return true if the key was successfully deleted, or false if it did not exist
	 * @throws NullPointerException if key is null
	 * @throws IllegalArgumentException if key does not start with a /
	 * @throws IllegalArgumentException if key represents a directory (ends with a /)
	 */
	public abstract boolean remove(String key);

	/**
	 * Lists the contents of a directory (non-recursively)
	 *
	 * @param directory path of the directory
	 * @return unsorted set of the files and directories inside of this directory, or null if the directory doesn't exist.
	 *
	 * @throws NullPointerException if key is null
	 * @throws IllegalArgumentException if the key does not represent a directory (does not end in a /)
	 * @throws IllegalArgumentException if the key does not start with a /
	 */
	public abstract Set<String> listDirectory(String directory);

	/**
	 * Deletes an empty directory
	 *
	 * @param directory path of the directory
	 *
	 * @throws IllegalArgumentException if the key does not represent a directory (does not end in a /)
	 * @throws IllegalArgumentException if the key does not start with a /
	 * @throws DirectoryNotEmptyException if the directory is not empty
	 */
	public abstract void removeDirectory(String directory) throws DirectoryNotEmptyException;

	/**
	 * Retrieve an item from the underlying store - you must call this from your KeyValueStore
	 *
	 * @param key key to retrieve
	 * @return The value stored at the given key, or null if none exists
	 * @throws NullPointerException if key is null
	 */
	protected AbstractKeyValueStoreEntry _get(String key) {
		if (key == null)
			throw new NullPointerException("key is null in _get");
		synchronized (map) {
			return map.get(key);
		}
	}

	/**
	 * Add an item to the underlying store - you must call this from your KeyValueStore
	 *
	 * @param key   key to set
	 * @param value value to store
	 * @throws NullPointerException if key or value is null
	 */
	protected void _set(String key, AbstractKeyValueStoreEntry value) {
		if (key == null || value == null) throw new NullPointerException("key or value was null in _set.");
		synchronized (map) {
			map.put(key, value);
		}
	}

	/**
	 * Remove an item from the underlying store - you must call this from your KeyValueStore
	 *
	 * This method supports removing both directory nodes and regular key/value pairs
	 *
	 * @param key key to remove
	 * @return true if the value was removed, false if not
	 * @throws NullPointerException if key is null
	 */
	protected boolean _remove(String key) {
		if (key == null)
			throw new NullPointerException("key is null in _remove");
		synchronized (map) {
			return map.remove(key) != null;
		}
	}

	/**
	 * Enumerates all of the keys (both directory structures and regular key value pairs) currently in the map
	 *
	 * @return Set containing all currently valid keys
	 */
	protected Set<String> _listKeys() {
		synchronized (map) {
			return new HashSet<>(map.keySet());
		}
	}

	@Override
	public String toString() {
		return "KeyValueStore{" +
				"map=" + map +
				'}';
	}
}
