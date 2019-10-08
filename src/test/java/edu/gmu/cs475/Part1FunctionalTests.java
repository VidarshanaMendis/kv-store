package edu.gmu.cs475;

import edu.gmu.cs475.internal.OurTimeout;
import org.junit.*;

import java.nio.file.DirectoryNotEmptyException;
import java.util.*;

public class Part1FunctionalTests {
    @Rule
    public OurTimeout globalTimeout = new OurTimeout(80000);
    private AbstractKeyValueStore kvStore;

    static void assertListContainsOnly(List<String> l, String... objs) {
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
        if (failed) Assert.fail("Expected list to have: " + Arrays.toString(objs) + ", but found " + l);

    }

    static boolean validateFailed = false;

    @BeforeClass
    public static void validateDoesNotCallListKeys() {
        try {
            KeyValueStore kvStore = new KeyValueStore() {
                @Override
                protected Set<String> _listKeys() {
                    validateFailed = true;
                    return super._listKeys();
                }
            };
            kvStore.get("/foo/bar");
            kvStore.set("/foo/bar", "baz");
            kvStore.set("/foo/bar/c", "baz");
            kvStore.set("/foo2/bar/c", "baz");
            kvStore.listDirectory("/foo/");
        } catch (Throwable t) {

        } finally {
            if (validateFailed)
                Assert.fail("Error: listKeys may only be called by the listKeys method. You may not call it from set, get, etc. You must fix this before proceeding.");
        }
    }

    @Before
    public void setup() {
        kvStore = new KeyValueStore();
    }

    @Test
    public void testAddAndGetStringAndListDir() {
        String key = "/testAddAndGetStringKey";
        String val = "/testAddAndGetStringKeyvalue1";
        kvStore.set(key, val);
        Assert.assertEquals("value was not as expected", val, kvStore.get(key));
        //Check to make sure that this key was added to the directory structure and that / exists now
        Set<String> c = kvStore.listDirectory("/");
        Assert.assertTrue("Expected to find the created key in the directory listing for /", c.remove("/testAddAndGetStringKey"));
    }

    @Test
    public void testAddAndDeleteAndGetString() {
        String key = "/testAddAndDeleteAndGetStringKey";
        String val = "/testAddAndDeleteAndGetStringKeyvalue1";
        kvStore.set(key, val);
        Assert.assertEquals(val, kvStore.get(key));
        Assert.assertTrue(kvStore.remove(key));
        Assert.assertEquals(null, kvStore.get(key));
        Set<String> c = kvStore.listDirectory("/");
        Assert.assertTrue("Expected no files in directory /, found: " + c, c.isEmpty());
    }

    @Test
    public void testSetAddsToAndCreatesDirectory() {
        /**
         * Programmatically generate a whole bunch of files to create
         */
        ArrayList<String> paths = new ArrayList<>();
        HashSet<String> directories = new HashSet<>();
        directories.add("/");
        for (int i = 0; i < 5; i++) {
            directories.add("/base" + i + "/");
            for (int j = 0; j < 5; j++) {
                directories.add("/base" + i + "/another" + j + "/");
                for (int k = 0; k < 5; k++) {
                    paths.add("/base" + i + "/another" + j + "/file" + k);
                }
            }
        }
        Collections.shuffle(paths, new Random(5));
        for (String s : paths) {
            kvStore.set(s, "someValue" + s);
        }

        //Check to make sure all of the directories were created
        Set<String> allFiles = kvStore.listKeys();
        Set<String> missingDirectories = new HashSet<>();
        missingDirectories.addAll(directories);
        missingDirectories.removeAll(allFiles);
        Assert.assertTrue("Expected to find all of the keys created, but didn't", allFiles.containsAll(paths));
        Assert.assertTrue("Expected to find directories created for each folder, but didn't find entries in listKeys() for: " + missingDirectories + " found instead:" + allFiles, allFiles.containsAll(directories));

        //Check each directory to make sure listDir works
        Set<String> contents = kvStore.listDirectory("/");
        for (int i = 0; i < 5; i++) {
            Assert.assertTrue("Expected to find directory /base" + i + "/ in /, but didn't", contents.remove("/base" + i + "/"));
            Set<String> contents2 = kvStore.listDirectory("/base" + i + "/");
            for (int j = 0; j < 5; j++) {
                Assert.assertTrue("Expected to find directory /base" + i + "/another" + j + "/ in /base" + i + "/, directory listing, but didn't. Instead, found: " + kvStore.listDirectory("/base" + i + "/"), contents2.remove("/base" + i + "/another" + j + "/"));
                Set<String> contents3 = kvStore.listDirectory("/base" + i + "/another" + j + "/");
                for (int k = 0; k < 5; k++) {
                    Assert.assertTrue("Expected to find directory /base/" + i + "/another" + j + "/file" + k + " in its folder, but didn't",
                            contents3.remove("/base" + i + "/another" + j + "/file" + k));
                }
                Assert.assertTrue("Found unexpected files: " + contents3, contents3.isEmpty());
            }
            Assert.assertTrue("Found unexpected files: " + contents2, contents2.isEmpty());
        }
        Assert.assertTrue("Found unexpected files: " + contents, contents.isEmpty());
    }

    @Test
    public void testDeleteRemovesFromDirectory() {

        /**
         * Programmatically generate a whole bunch of files to create
         */
        ArrayList<String> paths = new ArrayList<>();
        HashSet<String> directories = new HashSet<>();
        directories.add("/");
        HashSet<String> files = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            directories.add("/base" + i + "/");
            for (int j = 0; j < 5; j++) {
                directories.add("/base" + i + "/another" + j + "/");
                for (int k = 0; k < 5; k++) {
                    paths.add("/base" + i + "/another" + j + "/file" + k);
                    files.add("/base" + i + "/another" + j + "/file" + k);
                }
            }
        }
        Collections.shuffle(paths, new Random(6));
        for (String s : paths) {
            kvStore.set(s, "someValue" + s);
        }

        for (String s : files) {
            kvStore.remove(s);
        }

        //Check to make sure all of the directories are still there
        Set<String> allFiles = kvStore.listKeys();
        Set<String> missingDirectories = new HashSet<>();
        missingDirectories.addAll(directories);
        missingDirectories.removeAll(allFiles);
        Set<String> extraFiles = kvStore.listKeys();
        extraFiles.removeAll(directories);
        Assert.assertTrue("Expected to find all of the file keys deleted, but didn't. Here's what we found: " + extraFiles, extraFiles.isEmpty());
        Assert.assertTrue("Expected to find directories (still) created for each folder, but didn't find entries in listKeys() for: " + missingDirectories + " found instead:" + allFiles, allFiles.containsAll(directories));

        //Check each directory to make sure listDir works
        Set<String> contents = kvStore.listDirectory("/");
        for (int i = 0; i < 5; i++) {
            Assert.assertTrue("Expected to find directory /base" + i + "/ in /, but didn't", contents.remove("/base" + i + "/"));
            Set<String> contents2 = kvStore.listDirectory("/base" + i + "/");
            for (int j = 0; j < 5; j++) {
                Assert.assertTrue("Expected to find directory /base" + i + "/another" + j + "/ in /base" + i + "/, directory listing, but didn't. Instead, found: " + kvStore.listDirectory("/base" + i + "/"), contents2.remove("/base" + i + "/another" + j + "/"));
                Set<String> contents3 = kvStore.listDirectory("/base" + i + "/another" + j + "/");
                Assert.assertTrue("Expected directory /base" + i + "/another" + j + "/ to be empty, but found: " + contents3, contents3.isEmpty());
            }
            Assert.assertTrue("Found unexpected files: " + contents2, contents2.isEmpty());
        }
        Assert.assertTrue("Found unexpected files: " + contents, contents.isEmpty());
    }

    @Test
    public void testRemoveDirectory() {
        /**
         * Programmatically generate a whole bunch of files to create
         */
        ArrayList<String> paths = new ArrayList<>();
        HashSet<String> directories = new HashSet<>();
        directories.add("/");
        HashSet<String> files = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            directories.add("/base" + i + "/");
            for (int j = 0; j < 5; j++) {
                directories.add("/base" + i + "/another" + j + "/");
                for (int k = 0; k < 5; k++) {
                    paths.add("/base" + i + "/another" + j + "/file" + k);
                    files.add("/base" + i + "/another" + j + "/file" + k);
                }
            }
        }
        Collections.shuffle(paths, new Random(6));
        for (String s : paths) {
            kvStore.set(s, "someValue" + s);
        }

        //Should not be able to remove a directory n ow
        try {
            kvStore.removeDirectory("/base0/another1/");
            Assert.fail("Expected to be unable to delete a directory that wasn't empty!");
        } catch (DirectoryNotEmptyException e) {
            //Expected
        }

        for (String s : files) {
            kvStore.remove(s);
        }


        //All folders should now be empty, we can delete them
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                try {
                    kvStore.removeDirectory("/base" + i + "/another" + j + "/");
                } catch (DirectoryNotEmptyException e) {
                    e.printStackTrace();
                    Assert.fail("Unexpected exception");
                }
            }
            try {
                kvStore.removeDirectory("/base" + i + "/");
            } catch (DirectoryNotEmptyException e) {
                e.printStackTrace();
                Assert.fail("Unexpected exception");
            }
        }
        try {
            kvStore.removeDirectory("/");
        } catch (DirectoryNotEmptyException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception");
        }

        //Now there should be nothing
        Assert.assertTrue("Expected to have deleted all keys, but found: " + kvStore.listKeys(), kvStore.listKeys().isEmpty());
    }

    @Test
    public void testNoNullKeysOrValues() {

        try {
            kvStore.get(null);
            Assert.fail("Should not be able to call get(null) without a NullPointerException");
        } catch (NullPointerException ex) {
        }

        try {
            kvStore.set(null, "abcd");
            Assert.fail("Should not be able to call set(null,...) without a NullPointerException");
        } catch (NullPointerException ex) {
        }

        try {
            kvStore.set("/abcd", null);
            Assert.fail("Should not be able to call set(...,null) without a NullPointerException");
        } catch (NullPointerException ex) {
        }

        try {
            kvStore.remove(null);
            Assert.fail("Should not be able to call remove(null) without a NullPointerException");
        } catch (NullPointerException ex) {
        }

        try {
            kvStore.listDirectory(null);
            Assert.fail("Should not be able to call listDirectory(null) without a NullPointerException");
        } catch (NullPointerException ex) {
        }
        try {
            kvStore.removeDirectory(null);
            Assert.fail("Should not be able to call removeDirectory(null) without a NullPointerException");
        } catch (NullPointerException ex) {
        } catch (DirectoryNotEmptyException ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected exception");
        }
    }

    @Test
    public void testPathCheckingForDirectories() {
        try {
            kvStore.removeDirectory("abcd/");
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
        } catch (DirectoryNotEmptyException ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected exception");
        }
        try {
            kvStore.removeDirectory("/abcd");
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
        } catch (DirectoryNotEmptyException ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected exception");
        }
        try {
            kvStore.removeDirectory("abcd");
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
        } catch (DirectoryNotEmptyException ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected exception");
        }
        try {
            kvStore.listDirectory("abcd");
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
        }
        try {
            kvStore.listDirectory("/abcd");
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
        }
        try {
            kvStore.listDirectory("abcd/adsf/asdf/");
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
        }


        try {
            kvStore.set("abcd", "def");
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
        }

        try {
            kvStore.get("abcd");
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
        }
        try {
            kvStore.set("/abcd/", "def");
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
        }
        try {
            kvStore.get("/abcd/");
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
        }

        try {
            kvStore.remove("abcd");
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
        }
        try {
            kvStore.remove("/abcd/");
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
        }


    }

    String failedMessage = null;

    @Test
    public void testCreatesParentDirectoriesBeforeCreatingFile() {
        failedMessage = "_set was never called";
        this.kvStore = new KeyValueStore() {
            @Override
            protected void _set(String key, AbstractKeyValueStoreEntry value) {
              checkParents(key);
                super._set(key, value);
            }

            private void checkParents(String key){
                if (!key.startsWith("/"))
                    throw new IllegalArgumentException("Invalid key was allowed to go to _set: " + key);
                int i = 0;
                failedMessage = "";
                while (i <= key.lastIndexOf('/')) {
                    i = key.substring(i).indexOf('/') + i + 1;
                    String str = key.substring(0, i);
                    if (_get(str) == null)
                        failedMessage = "Expected to find directory <" + str + "> created before " + key + " was created. ";

                }
            }
        };
        this.kvStore.set("/some", "abcd");
        this.kvStore.set("/some/path/file", "abcd");
        this.kvStore.set("/other/path/file", "abcd");
        if (failedMessage != null && failedMessage.length() > 0)
            Assert.fail(failedMessage);

    }
}
