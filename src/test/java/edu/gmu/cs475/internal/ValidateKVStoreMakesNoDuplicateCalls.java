package edu.gmu.cs475.internal;

import edu.gmu.cs475.KeyValueStore;
import edu.gmu.cs475.AbstractKeyValueStoreEntry;

import java.util.HashSet;

public class ValidateKVStoreMakesNoDuplicateCalls {
	static String ret;

	public static synchronized String validate() {
		ret = "";
		final HashSet<String> seenCalls = new HashSet<>();
		KeyValueStore test = new KeyValueStore() {
			@Override
			protected AbstractKeyValueStoreEntry _get(String key) {
				if (!key.endsWith("/") && !seenCalls.add("get" + key))
					ret += "Duplicate call to _get(" + key + "). ";
				return super._get(key);
			}

			@Override
			protected void _set(String key, AbstractKeyValueStoreEntry value) {
				if (!key.endsWith("/") && !seenCalls.add("set" + key))
					ret += "Duplicate call to _set(" + key + "). ";
				super._set(key, value);
			}
		};

		//get should
		seenCalls.clear();

		test.set("/key1/value", "something");
		seenCalls.clear();


		test.set("/key1/value2", "something");
		seenCalls.clear();


		test.get("/key1/value");
		seenCalls.clear();

		return ret;
	}
}
