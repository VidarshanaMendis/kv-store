package edu.gmu.cs475;

import edu.gmu.cs475.internal.OurTimeout;
import edu.gmu.cs475.internal.ValidateKVStoreMakesNoDuplicateCalls;
import org.junit.*;
import org.junit.internal.AssumptionViolatedException;

import java.nio.file.DirectoryNotEmptyException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Part2ConcurrentTests {

	@Rule
	public OurTimeout globalTimeout = new OurTimeout(80000);
	volatile boolean failed = false;
	volatile boolean shouldIntercept = false;
	private AbstractKeyValueStore kvStoreUnderTest;
	private volatile Throwable ex;

	void assertDirectoryContainsOnly(String dir, String... objs) {
		Set<String> l = kvStoreUnderTest.listDirectory(dir);
		boolean failed = false;
		if (objs.length != l.size())
			failed = true;
		else {
			List<String> dup = new ArrayList<>(l);
			for (String s : objs)
				if (!dup.remove(s)) {
					failed = true;
					break;
				}
		}
		if (failed)
			Assert.fail("Expected directory  " + dir + " to have: " + Arrays.toString(objs) + ", but found " + l);

	}

	@Before
	public void setup() {
		kvStoreUnderTest = new KeyValueStore();
		//do some sanity checks
		String err = ValidateKVStoreMakesNoDuplicateCalls.validate();
		if (err.length() > 0)
			throw new AssumptionViolatedException("Before running any concurrent tests, you must ensure that calls to get() and set() never make more than one call to _get or _set for the same key path. " + err);
		ex = null;
	}

	@After
	public void checkUncaughtException() {
		if (ex != null)
			Assert.fail("Unexpected exception");
	}

	@Test
	public void testGetSameKeySimultaneously() {
		//Should be possible to get two different keys at once
		//As measured by making two requests, pausing in _get, and viewing both threads hit the _get
		failed = true;
		final long mainThreadID = Thread.currentThread().getId();

		this.kvStoreUnderTest = new KeyValueStore() {
			@Override
			protected AbstractKeyValueStoreEntry _get(String key) {
				if (key != null && key.equals("/theKey1") && Thread.currentThread().getId() == mainThreadID) {
					failed = false; //If this code never gets run, we need to fail, so default to fail
					Thread otherThread = new Thread(() -> {
						try {
							kvStoreUnderTest.get("/theKey1");
						} catch (Throwable t) {
							t.printStackTrace();
							ex = t;
						}
					});
					otherThread.start();
					try {
						otherThread.join(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (otherThread.isAlive()) {
						//Other thread did not finish get
						failed = true;
					}
				}
				return super._get(key);
			}
		};
		this.kvStoreUnderTest.set("/theKey1", "abcd");
		this.kvStoreUnderTest.get("/theKey1");
		Assert.assertTrue("Expected to be able to concurrently get the same key, but did not finish within 1 sec", !failed);
	}

	@Test
	public void testGetAndSetSameKeySimultaneously() {
		failed = true;
		final long mainThreadID = Thread.currentThread().getId();

		this.kvStoreUnderTest = new KeyValueStore() {
			@Override
			protected AbstractKeyValueStoreEntry _get(String key) {
				if (key != null && key.equals("/theKey1") && Thread.currentThread().getId() == mainThreadID) {
					Thread otherThread = new Thread(() -> {
						try {
							kvStoreUnderTest.set("/theKey1", "def");
						} catch (Throwable t) {
							t.printStackTrace();
							ex = t;
						}
					});
					otherThread.start();
					try {
						otherThread.join(1000);
					} catch (InterruptedException e) {
						//e.printStackTrace();
					}
					if (otherThread.isAlive()) {
						//Other thread did not finish get
						failed = false;
					}
				}
				return super._get(key);
			}
		};
		this.kvStoreUnderTest.set("/theKey1", "abcd");
		this.kvStoreUnderTest.get("/theKey1");
		Assert.assertTrue("Expected to not be able to concurrently get and set on the same key, but did", !failed);

	}

	@Test
	public void testGetAndSetDifferentKeysSimultaneously() {

		//Should not be possible to get two different keys at once
		//As measured by making two requests, pausing in _get, and trying to call _set
		failed = true;
		final long mainThreadID = Thread.currentThread().getId();

		this.kvStoreUnderTest = new KeyValueStore() {
			@Override
			protected AbstractKeyValueStoreEntry _get(String key) {
				if (key != null && key.equals("/theKey1") && Thread.currentThread().getId() == mainThreadID) {
					failed = false;
					Thread otherThread = new Thread(() -> {
						try {
							kvStoreUnderTest.set("/theKey2", "def");
						} catch (Throwable t) {
							t.printStackTrace();
							ex = t;
						}
					});
					otherThread.start();
					try {
						otherThread.join(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (otherThread.isAlive()) {
						//Other thread did not finish get
						failed = true;
					}
				}
				return super._get(key);
			}
		};
		this.kvStoreUnderTest.set("/theKey1", "abcd");
		this.kvStoreUnderTest.get("/theKey1");
		Assert.assertTrue("Expected to be able to concurrently get/set two different keys, but did not complete the set within 1sec", !failed);
	}

	@Test
	public void testSetAndRemoveSameKeySimultaneously() {

		failed = true;
		final long mainThreadID = Thread.currentThread().getId();

		this.kvStoreUnderTest = new KeyValueStore() {
			@Override
			protected void _set(String key, AbstractKeyValueStoreEntry value) {
				if (key != null && key.equals("/theKey1") && Thread.currentThread().getId() == mainThreadID) {
					Thread otherThread = new Thread(() -> {
						try {
							kvStoreUnderTest.remove("/theKey1");
						} catch (Throwable t) {
							t.printStackTrace();
							ex = t;
						}
					});
					otherThread.start();
					try {
						otherThread.join(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (otherThread.isAlive()) {
						//Other thread did not finish get
						failed = false;
					}
				}
				super._set(key, value);
			}
		};
		this.kvStoreUnderTest.set("/theKey1", "abcd");
		this.kvStoreUnderTest.get("/theKey1");
		Assert.assertTrue("Expected to not be able to set and remove the same key simultaneoulsy, but did", !failed);
	}

	@Test
	public void testSetAndRemoveDifferentKeySimultaneously() {

		failed = true;
		final long mainThreadID = Thread.currentThread().getId();

		this.kvStoreUnderTest = new KeyValueStore() {
			@Override
			protected void _set(String key, AbstractKeyValueStoreEntry value) {
				if (key != null && key.equals("/theKey1") && Thread.currentThread().getId() == mainThreadID) {
					failed = false;
					Thread otherThread = new Thread(() -> {
						try {
							kvStoreUnderTest.remove("/otherDir/theKey2");
						} catch (Throwable t) {
							t.printStackTrace();
							ex = t;
						}
					});
					otherThread.start();
					try {
						otherThread.join(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (otherThread.isAlive()) {
						//Other thread did not finish get
						failed = true;
					}
				}
				super._set(key, value);
			}
		};
		this.kvStoreUnderTest.set("/otherDir/theKey2", "abcd");
		this.kvStoreUnderTest.set("/theKey1", "abcd");
		Assert.assertTrue("Expected to be able to set and remove the different keys simultaneously, but did not within 1 sec", !failed);
	}

	@Test
	public void testSimultaneousInitializationOfDirectories() {
		final long mainThreadID = Thread.currentThread().getId();

		final Thread otherThread = new Thread(() -> {
			try {
				kvStoreUnderTest.set("/middleDir/anotherFolder/anotherFile", "def");
			} catch (Throwable t) {
				t.printStackTrace();
				ex = t;
			}
		});
		failed = true;
		this.kvStoreUnderTest = new KeyValueStore() {
			@Override
			protected void _set(String key, AbstractKeyValueStoreEntry value) {
				if (key != null && key.equals("/middleDir/") && Thread.currentThread().getId() == mainThreadID) {
					otherThread.start();
					try {
						otherThread.join(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (otherThread.isAlive()) {
						//Other thread did not finish get
						failed = false;
					} else
						failed = true;
				}
				super._set(key, value);
			}
		};
		this.kvStoreUnderTest.set("/middleDir/anotherFolder/file2", "abcd");
		if (otherThread.isAlive())
			try {
				otherThread.join();
			} catch (InterruptedException ex) {

			}
		if (!failed) {

			//Make sure that everything got created correctly
			Set<String> files = kvStoreUnderTest.listKeys();
			Set<String> expectedFiles = new HashSet<>();
			Set<String> expectedDirs = new HashSet<>();
			expectedDirs.add("/");
			expectedDirs.add("/middleDir/");
			expectedDirs.add("/middleDir/anotherFolder/");
			expectedFiles.add("/middleDir/anotherFolder/file2");
			expectedFiles.add("/middleDir/anotherFolder/anotherFile");

			for (String s : expectedDirs)
				Assert.assertTrue("Expected to have successfully created directory " + s + ", but it's not in the list from listKeys()", files.remove(s));

			for (String s : expectedFiles)
				Assert.assertTrue("Expected to have successfully created file " + s + ", but it's not in the list from listKeys()", files.remove(s));
			Assert.assertTrue("Unexpected keys found: " + files, files.isEmpty());

			assertDirectoryContainsOnly("/", "/middleDir/");
			assertDirectoryContainsOnly("/middleDir/", "/middleDir/anotherFolder/");
			assertDirectoryContainsOnly("/middleDir/anotherFolder/", "/middleDir/anotherFolder/file2", "/middleDir/anotherFolder/anotherFile");

		} else {
			Assert.fail("While in the middle of initialization anotherFolder, we found another thread also created it (it was created twice)! You must hold locks as you initialize paths");
		}
	}

	@Test
	public void testListDirSimultaneously() {
		//should be able to call listDir on same dir more than once at a time

		failed = true;
		final long mainThreadID = Thread.currentThread().getId();

		shouldIntercept = false;
		this.kvStoreUnderTest = new KeyValueStore() {
			@Override
			protected AbstractKeyValueStoreEntry _get(String key) {
				if (shouldIntercept && key != null && key.equals("/middleDir/anotherFolder/") && Thread.currentThread().getId() == mainThreadID) {
					failed = false;
					Thread otherThread = new Thread(() -> {
						try {
							kvStoreUnderTest.listDirectory("/middleDir/anotherFolder/");
						} catch (Throwable t) {
							t.printStackTrace();
							ex = t;
						}
					});
					otherThread.start();
					try {
						otherThread.join(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (otherThread.isAlive()) {
						//Other thread did not finish get
						failed = true;
					}
				}
				return super._get(key);
			}
		};
		this.kvStoreUnderTest.set("/middleDir/anotherFolder/file2", "abcd");
		this.kvStoreUnderTest.set("/middleDir/anotherFolder/file1", "abcd");
		this.kvStoreUnderTest.set("/middleDir/anotherFolder/file3", "abcd");
		shouldIntercept = true;
		this.kvStoreUnderTest.listDirectory("/middleDir/anotherFolder/");
		Assert.assertTrue("Expected to be able list the same directory simultaneously, but did not within 1sec", !failed);
	}

	@Test
	public void testListDirWhileRemoveFile() {
		failed = true;
		final long mainThreadID = Thread.currentThread().getId();

		shouldIntercept = false;
		this.kvStoreUnderTest = new KeyValueStore() {
			@Override
			protected AbstractKeyValueStoreEntry _get(String key) {
				if (shouldIntercept && key != null && key.equals("/middleDir/anotherFolder/") && Thread.currentThread().getId() == mainThreadID) {
					KeyValueStoreDirectory ret = (KeyValueStoreDirectory) super._get(key);
					return new KeyValueStoreDirectory(ret.getDirectoryPath()){
						@Override
						public ReentrantReadWriteLock getLock() {
							return ret.getLock();
						}

						@Override
						public List<String> getChildren() {
							Thread otherThread = new Thread(() -> {
								try {
									kvStoreUnderTest.removeDirectory("/middleDir/anotherFolder/");
								} catch (DirectoryNotEmptyException e) {
									e.printStackTrace();
								} catch (Throwable t) {
									t.printStackTrace();
									ex = t;
								}
							});
							otherThread.start();
							try {
								otherThread.join(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							if (otherThread.isAlive()) {
								//Other thread did not finish get
								failed = false;
							}
							return ret.getChildren();
						}
					};
				}
				return super._get(key);
			}
		};
		this.kvStoreUnderTest.set("/middleDir/anotherFolder/file2", "abcd");
		this.kvStoreUnderTest.remove("/middleDir/anotherFolder/file2");
		//Make sure directory is still there
		if (!this.kvStoreUnderTest.listDirectory("/middleDir/").contains("/middleDir/anotherFolder/")) {
			Assert.fail("Error in setup: found that creating a file (in a folder), then deleting that file deleted the folder");
		}
		shouldIntercept = true;
		this.kvStoreUnderTest.listDirectory("/middleDir/anotherFolder/");
		Assert.assertTrue("Expected to not be able list the same directory while deleting it, but did", !failed);
	}

	@Test
	public void testListParentDirWhileRemoveFile() {
		failed = true;
		final long mainThreadID = Thread.currentThread().getId();

		shouldIntercept = false;
		this.kvStoreUnderTest = new KeyValueStore() {
			@Override
			protected AbstractKeyValueStoreEntry _get(String key) {
				if (shouldIntercept && key != null && key.equals("/middleDir/") && Thread.currentThread().getId() == mainThreadID) {
					KeyValueStoreDirectory ret = (KeyValueStoreDirectory) super._get(key);
					return new KeyValueStoreDirectory(ret.getDirectoryPath()){
						@Override
						public ReentrantReadWriteLock getLock() {
							return ret.getLock();
						}

						@Override
						public List<String> getChildren() {
							Thread otherThread = new Thread(() -> {
								try {
									kvStoreUnderTest.removeDirectory("/middleDir/anotherFolder/");
								} catch (DirectoryNotEmptyException e) {
									e.printStackTrace();
								} catch (Throwable t) {
									t.printStackTrace();
									ex = t;
								}
							});
							otherThread.start();
							try {
								otherThread.join(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							if (otherThread.isAlive()) {
								//Other thread did not finish get
								failed = false;
							}
							return super.getChildren();
						}
					};
				}
				return super._get(key);
			}
		};
		this.kvStoreUnderTest.set("/middleDir/anotherFolder/file2", "abcd");
		this.kvStoreUnderTest.remove("/middleDir/anotherFolder/file2");
		//Make sure directory is still there
		if (!this.kvStoreUnderTest.listDirectory("/middleDir/").contains("/middleDir/anotherFolder/")) {
			Assert.fail("Error in setup: found that creating a file (in a folder), then deleting that file deleted the folder");
		}
		shouldIntercept = true;
		this.kvStoreUnderTest.listDirectory("/middleDir/");
		Assert.assertTrue("Expected to not be able list a directory while changing its contents, but did", !failed);
	}

	@Test
	public void testListParentDirWhileCreateFile() {
		failed = true;
		final long mainThreadID = Thread.currentThread().getId();

		shouldIntercept = false;
		this.kvStoreUnderTest = new KeyValueStore() {
			@Override
			protected AbstractKeyValueStoreEntry _get(String key) {
				if (shouldIntercept && key != null && key.equals("/middleDir/") && Thread.currentThread().getId() == mainThreadID) {
					KeyValueStoreDirectory ret = (KeyValueStoreDirectory) super._get(key);
					return new KeyValueStoreDirectory(ret.getDirectoryPath()) {
						@Override
						public ReentrantReadWriteLock getLock() {
							return ret.getLock();
						}

						@Override
						public List<String> getChildren() {
							Thread otherThread = new Thread(() -> {
								try {
									kvStoreUnderTest.set("/middleDir/some/other/file", "bar");
								} catch (Throwable t) {
									t.printStackTrace();
									ex = t;
								}
							});
							otherThread.start();
							try {
								otherThread.join(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							if (otherThread.isAlive()) {
								//Other thread did not finish get
								failed = false;
							}
							return ret.getChildren();
						}
					};
				}
				return super._get(key);
			}
		};
		this.kvStoreUnderTest.set("/middleDir/anotherFolder/file2", "abcd");
		this.kvStoreUnderTest.remove("/middleDir/anotherFolder/file2");
		//Make sure directory is still there
		if (!this.kvStoreUnderTest.listDirectory("/middleDir/").contains("/middleDir/anotherFolder/")) {
			Assert.fail("Error in setup: found that creating a file (in a folder), then deleting that file deleted the folder");
		}
		shouldIntercept = true;
		this.kvStoreUnderTest.listDirectory("/middleDir/");
		Assert.assertTrue("Expected to not be able list a directory while changing its contents, but did", !failed);
	}

	@Test
	public void testListAllKeysWhileSetKey() {
		failed = true;
		final long mainThreadID = Thread.currentThread().getId();

		shouldIntercept = false;
		this.kvStoreUnderTest = new KeyValueStore() {
			@Override
			protected Set<String> _listKeys() {
				if (shouldIntercept && Thread.currentThread().getId() == mainThreadID) {
					Thread otherThread = new Thread(() -> {
						try {
							kvStoreUnderTest.set("/middleDir/some/other/file", "bar");
						} catch (Throwable t) {
							t.printStackTrace();
							ex = t;
						}
					});
					otherThread.start();
					try {
						otherThread.join(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (otherThread.isAlive()) {
						//Other thread did not finish get
						failed = false;
					}
				}
				return super._listKeys();
			}
		};
		this.kvStoreUnderTest.set("/middleDir/anotherFolder/file2", "abcd");
		this.kvStoreUnderTest.set("/middleDir2/anotherFolder2/file2", "abcd");
		shouldIntercept = true;
		this.kvStoreUnderTest.listKeys();
		Assert.assertTrue("Expected to not be able list keys while adding keys, but did", !failed);
	}

	@Test
	public void testRemoveParentDirectoryWhileSetKey() {

		failed = true;
		final long mainThreadID = Thread.currentThread().getId();

		this.kvStoreUnderTest = new KeyValueStore() {
			@Override
			protected void _set(String key, AbstractKeyValueStoreEntry value) {
				if (key.equals("/middleDir/anotherFolder/file2") && Thread.currentThread().getId() == mainThreadID) {
					Thread otherThread = new Thread(() -> {
						try {
							kvStoreUnderTest.removeDirectory("/middleDir/anotherFolder/");
						} catch (DirectoryNotEmptyException e) {
							//expected
						} catch (Throwable t) {
							t.printStackTrace();
							ex = t;
						}
					});
					otherThread.start();
					try {
						otherThread.join(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (otherThread.isAlive()) {
						//Other thread did not finish get
						failed = false;
					}
				}
				super._set(key, value);
			}
		};
		this.kvStoreUnderTest.set("/middleDir/anotherFolder/file2", "abcd");
		Assert.assertTrue("Expected to not be able to delete a file's parent directory while creating the file", !failed);
	}


}
