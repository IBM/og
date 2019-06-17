/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.object;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.io.BaseEncoding;
import com.google.common.io.Files;
import com.ibm.og.util.ObjectManagerUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RandomObjectPopulatorTest {
  private static final Logger _logger = LoggerFactory.getLogger(RandomObjectPopulatorTest.class);
  final String dirName = "RandomObjectPopulatorTest";
  static final String prefix = "id_";
  static final String suffix = ".object";

  final protected UUID vaultId = new UUID(0, 0);

  final static int OBJECT_SIZE = LegacyObjectMetadata.OBJECT_SIZE;
  protected final static int MAX_OBJECTS = 5;

  final Random rand = new Random();

  protected File[] getIdFiles(String prefix, String suffix) {
    final File dir = new File(".");
    final File[] files = dir.listFiles(new ObjectManagerUtils.IdFilter(prefix, suffix));
    return files;
  }

  @Before
  public void copyIdFiles() {
    final File[] files = getIdFiles(this.prefix, this.suffix);
    final File directory = new File(this.dirName);
    if (!(directory.exists() && directory.isDirectory())) {
      directory.mkdir();
    }
    for (final File id : files) {
      id.renameTo(new File(directory, id.getName()));
    }
  }

  private void deleteTempFiles() {
    final File[] files = getIdFiles(this.prefix, suffix);
    for (final File id : files) {
      id.delete();
    }
  }

  @After
  public void restoreIdFiles() {
    deleteTempFiles();
    final File directory = new File(this.dirName);
    final File[] files = directory.listFiles();
    final File parentDir = directory.getParentFile();
    for (final File id : files) {
      id.renameTo(new File(parentDir, id.getName()));
    }
    directory.delete();
  }

  @Test
  public void writeSingleIdTest() throws ObjectManagerException {
    final ObjectMetadata sid = generateId();
    RandomObjectPopulator rop = new RandomObjectPopulator(this.vaultId);
    rop.add(sid);
    Assert.assertEquals(sid, rop.get());
    rop.shutdown();
    rop = new RandomObjectPopulator(this.vaultId);
    Assert.assertEquals(sid.toString(), rop.remove().toString());
    rop.shutdown();
  }

  @Test
  public void deleteTest() throws ObjectManagerException {
    final ObjectMetadata firstId = generateId();
    ObjectMetadata secondId = firstId;
    while (secondId.equals(firstId)) {
      secondId = generateId();
    }
    RandomObjectPopulator rop = new RandomObjectPopulator(this.vaultId);
    rop.add(firstId);
    rop.add(secondId);
    final ObjectMetadata readId = rop.get();
    final ObjectMetadata deleteId = rop.remove();
    Assert.assertTrue((readId.equals(firstId) || readId.equals(secondId)));
    Assert.assertTrue((deleteId.equals(firstId) || deleteId.equals(secondId)));
    Assert.assertFalse(readId.equals(deleteId));
    rop.shutdown();
    rop = new RandomObjectPopulator(this.vaultId);
    Assert.assertFalse(deleteId.toString().equals(rop.get().toString()));
    rop.shutdown();
  }

  @Test
  public void simultaneousReadDeleteTest()
      throws ObjectManagerException, InterruptedException, ExecutionException {
    final ObjectMetadata sid = generateId();
    final RandomObjectPopulator rop = new RandomObjectPopulator(this.vaultId);
    rop.add(sid);
    Assert.assertEquals(sid, rop.get());
    // Try again for simultaneous reads
    Assert.assertEquals(sid, rop.get());
    final boolean[] released = {false};
    class DeleteLoop implements Callable<Void> {
      final boolean[] released;

      public DeleteLoop(final boolean[] released) {
        this.released = released;
      }

      @Override
      public Void call() throws Exception {
        rop.remove();
        Assert.assertTrue(this.released[0]);
        return null;
      }
    }
    final Future<Void> future =
        Executors.newSingleThreadExecutor().submit(new DeleteLoop(released));
    rop.getComplete(sid);
    released[0] = true;
    // And again for the other read
    rop.getComplete(sid);
    // The DeleteLoop should now complete
    future.get();
    rop.shutdown();
  }

  @Test
  public void overflowIdFile() throws ObjectManagerException {
    RandomObjectPopulator rop =
        new RandomObjectPopulator(this.vaultId, RandomObjectPopulatorTest.MAX_OBJECTS);
    for (int i = 0; i < RandomObjectPopulatorTest.MAX_OBJECTS; i++) {
      rop.add(generateId());
    }
    rop.shutdown();
    Assert.assertEquals(getIdFiles(this.prefix, suffix).length, 1);
    rop = new RandomObjectPopulator(this.vaultId, RandomObjectPopulatorTest.MAX_OBJECTS);
    rop.add(generateId());
    rop.shutdown();
    Assert.assertEquals(getIdFiles(this.prefix, suffix).length, 2);
    Assert.assertEquals(new File(prefix + 0 + suffix).length(),
        ObjectFileVersion.VERSION_HEADER_LENGTH + RandomObjectPopulatorTest.MAX_OBJECTS * OBJECT_SIZE);
    Assert.assertEquals(new File(prefix + 1 + suffix).length(), ObjectFileVersion.VERSION_HEADER_LENGTH + OBJECT_SIZE);
  }

  @Test
  public void overflowManyIdFile() throws ObjectManagerException {
    RandomObjectPopulator rop =
        new RandomObjectPopulator(this.vaultId, RandomObjectPopulatorTest.MAX_OBJECTS);
    for (int i = 0; i < RandomObjectPopulatorTest.MAX_OBJECTS; i++) {
      rop.add(generateId());
    }
    rop.shutdown();
    Assert.assertEquals(getIdFiles(this.prefix, suffix).length, 1);
    rop = new RandomObjectPopulator(this.vaultId, RandomObjectPopulatorTest.MAX_OBJECTS);
    for (int i = 0; i < 1 + RandomObjectPopulatorTest.MAX_OBJECTS * 3; i++) {
      rop.add(generateId());
    }
    rop.shutdown();
    Assert.assertEquals(getIdFiles(this.prefix, suffix).length, 5);
    for (int i = 0; i < 4; i++) {
      Assert.assertEquals(new File(prefix + i + suffix).length(),
          ObjectFileVersion.VERSION_HEADER_LENGTH + RandomObjectPopulatorTest.MAX_OBJECTS * OBJECT_SIZE);
    }
    Assert.assertEquals(new File(prefix + 4 + suffix).length(),
            ObjectFileVersion.VERSION_HEADER_LENGTH + OBJECT_SIZE);
  }

  @Test
  public void borrowIds() throws ObjectManagerException {
    RandomObjectPopulator rop =
        new RandomObjectPopulator(this.vaultId, RandomObjectPopulatorTest.MAX_OBJECTS);
    for (int i = 0; i < RandomObjectPopulatorTest.MAX_OBJECTS; i++) {
      rop.add(generateId());
    }
    // We write to objects to test borrowing one, and then two
    final int surplus = 2;
    for (int i = 0; i < surplus; i++) {
      rop.add(generateId());
    }
    rop.shutdown();

    Assert.assertEquals(getIdFiles(this.prefix, suffix).length, 2);
    Assert.assertEquals(rop.getSavedObjectCount(), RandomObjectPopulatorTest.MAX_OBJECTS + surplus);
    // Borrow one id
    rop = new RandomObjectPopulator(this.vaultId, RandomObjectPopulatorTest.MAX_OBJECTS);
    rop.remove();
    rop.shutdown();
    Assert.assertEquals(getIdFiles(this.prefix, suffix).length, 2);
    Assert.assertEquals(new File(prefix + 1 + suffix).length(),
            ObjectFileVersion.VERSION_HEADER_LENGTH + OBJECT_SIZE);
    // Borrow last id from surplus file, causing that file to be deleted
    rop = new RandomObjectPopulator(this.vaultId, RandomObjectPopulatorTest.MAX_OBJECTS);
    rop.remove();
    rop.shutdown();
    Assert.assertEquals(getIdFiles(this.prefix, suffix).length, 1);
    try {
      Files.copy(new File(prefix + 0 + suffix), new File("/tmp/copy.object"));
    } catch(Exception e) {
    }
    Assert.assertEquals(new File(prefix + 0 + suffix).length(),
        ObjectFileVersion.VERSION_HEADER_LENGTH + RandomObjectPopulatorTest.MAX_OBJECTS * OBJECT_SIZE);
  }

  @Test
  public void testMultipleFiles() throws ObjectManagerException {
    RandomObjectPopulator rop =
        new RandomObjectPopulator(this.vaultId, RandomObjectPopulatorTest.MAX_OBJECTS);
    for (int i = 0; i < RandomObjectPopulatorTest.MAX_OBJECTS * 4; i++) {
      rop.add(generateId());
    }
    rop.add(generateId());
    rop.shutdown();
    Assert.assertEquals(getIdFiles(this.prefix, suffix).length, 5);
    rop = new RandomObjectPopulator(this.vaultId, RandomObjectPopulatorTest.MAX_OBJECTS);
    rop.remove();
    rop.shutdown();
    Assert.assertEquals(getIdFiles(this.prefix, suffix).length, 4);
    Assert.assertEquals(new File(prefix + 0 + suffix).length(),
            ObjectFileVersion.VERSION_HEADER_LENGTH + RandomObjectPopulatorTest.MAX_OBJECTS * OBJECT_SIZE);
  }

  @Test
  public void verifyReadIds() throws ObjectManagerException {
    RandomObjectPopulator rop =
        new RandomObjectPopulator(this.vaultId, RandomObjectPopulatorTest.MAX_OBJECTS);
    final ObjectMetadata[] savedIds = new ObjectMetadata[RandomObjectPopulatorTest.MAX_OBJECTS];
    for (int i = 0; i < RandomObjectPopulatorTest.MAX_OBJECTS; i++) {
      final ObjectMetadata sid = generateId();
      savedIds[i] = sid;
      rop.add(sid);
    }

    rop.shutdown();
    Assert.assertEquals(getIdFiles(this.prefix, suffix).length, 1);
    rop = new RandomObjectPopulator(this.vaultId, RandomObjectPopulatorTest.MAX_OBJECTS);
    final ObjectMetadata[] retrievedIds = new ObjectMetadata[RandomObjectPopulatorTest.MAX_OBJECTS];

    for (int i = 0; i < RandomObjectPopulatorTest.MAX_OBJECTS; i++) {
      final ObjectMetadata idForDelete = rop.remove();
      retrievedIds[i] = idForDelete;
    }

    Arrays.sort(savedIds);
    Arrays.sort(retrievedIds);
    Assert.assertArrayEquals(savedIds, retrievedIds);

    rop.shutdown();
    Assert.assertEquals(getIdFiles(this.prefix, suffix).length, 1);
    Assert.assertEquals(new File(prefix + 0 + suffix).length(), 0);
  }

  protected ObjectMetadata generateId() {
    return LegacyObjectMetadata.fromMetadata(UUID.randomUUID().toString().replace("-", "") + "0000",
        0, -1, (byte)0, -1);
  }

  @Ignore
  @Test
  public void testConcurrency()
      throws InterruptedException, ExecutionException, ObjectManagerException {
    final RandomObjectPopulator rop =
        new RandomObjectPopulator(this.vaultId, ".", "", 100000, 5 * 1000, null);
    final ConcurrentHashMap<ObjectMetadata, ObjectMetadata> ids =
        new ConcurrentHashMap<ObjectMetadata, ObjectMetadata>();

    for (int i = 0; i < 10000; i++) {
      final ObjectMetadata id = generateId();
      ids.put(id, id);
      rop.add(id);
    }
    final AtomicBoolean running = new AtomicBoolean(true);

    final ExecutorService executor = Executors.newCachedThreadPool();

    final List<Future<Void>> runningThreads = new ArrayList<Future<Void>>();

    // write threads
    for (int i = 0; i < 40; i++) {
      final Callable<Void> thread = new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          while (running.get()) {
            final ObjectMetadata id = generateId();
            ids.put(id, id);
            rop.add(id);
          }
          return null;
        }

      };
      runningThreads.add(executor.submit(thread));
    }
    // read threads
    for (int i = 0; i < 20; i++) {
      final Callable<Void> thread = new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          while (running.get()) {
            try {
              final ObjectMetadata id = rop.get();
              Assert.assertEquals(id, ids.get(id));
              rop.getComplete(id);
            } catch (final ObjectManagerException e) {
              e.printStackTrace();
              System.exit(-1);
            }
          }
          return null;
        }
      };
      runningThreads.add(executor.submit(thread));
    }

    // delete threads
    for (int i = 0; i < 20; i++) {
      final Callable<Void> thread = new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          while (running.get()) {
            try {
              final ObjectMetadata id = rop.remove();
              Assert.assertNotNull(ids.remove(id));
            } catch (final ObjectManagerException e) {
              e.printStackTrace();
              System.exit(-1);
            }
          }
          return null;
        }
      };
      runningThreads.add(executor.submit(thread));
    }

    Thread.sleep(1000);
    running.set(false);
    for (final Future<Void> future : runningThreads) {
      future.get();
    }
    rop.shutdown();

    RandomObjectPopulator verify;
    do {
      verify = new RandomObjectPopulator(this.vaultId, ".", "", 100000, 5 * 1000, null);
      final long objectCount = verify.getSavedObjectCount();
      Assert.assertEquals(ids.size(), objectCount);
      while (true) {
        try {
          final ObjectMetadata id = verify.remove();
          Assert.assertEquals(id, ids.remove(id));
        } catch (final ObjectManagerException e) {
          break;
        }
      }
      verify.shutdown();
    } while (verify.getSavedObjectCount() > 0);
    Assert.assertEquals(0, ids.size());
  }

  @Test
  public void testIdFilter() {
    final RandomObjectPopulator r = new RandomObjectPopulator(UUID.randomUUID(), "test");
    final FilenameFilter f = new ObjectManagerUtils.IdFilter("test", this.suffix);
    Assert.assertFalse(f.accept(null, "test" + suffix));
    Assert.assertTrue(f.accept(null, "test0" + suffix));
    Assert.assertFalse(f.accept(null, "test00" + suffix));
    Assert.assertTrue(f.accept(null, "test10" + suffix));
    Assert.assertFalse(f.accept(null, "test01" + suffix));
    Assert.assertTrue(f.accept(null, "test111" + suffix));
    Assert.assertTrue(f.accept(null, "test1111" + suffix));
    Assert.assertFalse(f.accept(null, "testt" + suffix));
    Assert.assertFalse(f.accept(null, "testt0" + suffix));
  }

  @Test
  public void testWriteHeader() throws IOException {

    final RandomObjectPopulator r = new RandomObjectPopulator(UUID.randomUUID(), "test");
    ByteArrayOutputStream bos = new ByteArrayOutputStream(1000);
    ObjectFileUtil.writeObjectFileVersion(bos);
    ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
    ObjectFileVersion version = ObjectFileUtil.readObjectFileVersion(bis);
    Assert.assertTrue(version.getMajorVersion() == (byte)0x2);
    Assert.assertTrue(version.getMinorVersion() == (byte)0x0);

  }

  @Test
  public void testLoadSingleV1ObjectFile() throws IOException {

    final OutputStream out = new BufferedOutputStream(new FileOutputStream( "id_0.object"));
    final BaseEncoding ENCODING = BaseEncoding.base16().lowerCase();
    for (int i = 0; i < 4; i++) {
      ByteBuffer objectBuffer = ByteBuffer.allocate(30);
      String objectId = UUID.randomUUID().toString().replace("-", "") + "0000";
      objectBuffer.put(ENCODING.decode(objectId));
      objectBuffer.putLong(99L);
      objectBuffer.putInt(5000);
      out.write(objectBuffer.array());
    }
    out.close();

    RandomObjectPopulator rop =
            new RandomObjectPopulator(this.vaultId, RandomObjectPopulatorTest.MAX_OBJECTS);
    rop.shutdown();
    Assert.assertTrue(rop.getSavedObjectCount() == 4);
  }

  @Test
  public void testLoadMultipleV1ObjectFile() throws IOException {

    OutputStream out = new BufferedOutputStream(new FileOutputStream( "id_0.object"));
    final BaseEncoding ENCODING = BaseEncoding.base16().lowerCase();
    for (int i = 0; i < MAX_OBJECTS; i++) {
      ByteBuffer objectBuffer = ByteBuffer.allocate(30);
      String objectId = UUID.randomUUID().toString().replace("-", "") + "0000";
      objectBuffer.put(ENCODING.decode(objectId));
      objectBuffer.putLong(99L);
      objectBuffer.putInt(5000);
      out.write(objectBuffer.array());
    }
    out.close();

    out = new BufferedOutputStream(new FileOutputStream( "id_1.object"));
    for (int i = 0; i < 3; i++) {
      ByteBuffer objectBuffer = ByteBuffer.allocate(30);
      String objectId = UUID.randomUUID().toString().replace("-", "") + "0000";
      objectBuffer.put(ENCODING.decode(objectId));
      objectBuffer.putLong(100L);
      objectBuffer.putInt(10000);
      out.write(objectBuffer.array());
    }
    out.close();
    RandomObjectPopulator rop =
            new RandomObjectPopulator(this.vaultId, RandomObjectPopulatorTest.MAX_OBJECTS);
    rop.remove();
    rop.shutdown();
    Assert.assertTrue(rop.getSavedObjectCount() == 6);

  }

  // @Test
  // public void testInvalidRetention() throws InterruptedException, ExecutionException, IOException
  // {
  // final RandomObjectPopulator rop =
  // new RandomObjectPopulator(this.vaultId, "", 100000, 10 * 1000);
  // final ConcurrentHashMap<ObjectName, ObjectName> ids =
  // new ConcurrentHashMap<ObjectName, ObjectName>();
  // final ConcurrentHashMap<ObjectName, ObjectName> deleted =
  // new ConcurrentHashMap<ObjectName, ObjectName>();
  //
  // for (int i = 0; i < 10000; i++)
  // {
  // final ObjectName id = generateId();
  // ids.put(id, id);
  // rop.writeNameComplete(id);
  // }
  // final AtomicBoolean running = new AtomicBoolean(true);
  //
  // final ExecutorService executor = Executors.newCachedThreadPool();
  //
  // final List<Future<Void>> runningThreads = new ArrayList<Future<Void>>();
  //
  // // write threads
  // for (int i = 0; i < 20; i++)
  // {
  // final Callable<Void> thread = new Callable<Void>()
  // {
  // @Override
  // public Void call() throws Exception
  // {
  // while (running.get())
  // {
  // final ObjectName id = generateId();
  // ids.put(id, id);
  // rop.writeNameComplete(id);
  // }
  // return null;
  // }
  //
  // };
  // runningThreads.add(executor.submit(thread));
  // }
  // // read threads
  // for (int i = 0; i < 20; i++)
  // {
  // final Callable<Void> thread = new Callable<Void>()
  // {
  // @Override
  // public Void call() throws Exception
  // {
  // while (running.get())
  // {
  // try
  // {
  // final ObjectName id = rop.acquireNameForRead();
  // Assert.assertEquals(id, ids.get(id));
  // rop.releaseNameFromRead(id);
  // }
  // catch (final OOMStopException e)
  // {
  // e.printStackTrace();
  // System.exit(-1);
  // }
  // }
  // return null;
  // }
  // };
  // runningThreads.add(executor.submit(thread));
  // }
  //
  // // delete threads
  // for (int i = 0; i < 20; i++)
  // {
  // final Callable<Void> thread = new Callable<Void>()
  // {
  // @Override
  // public Void call() throws Exception
  // {
  // while (running.get())
  // {
  // try
  // {
  // final ObjectName id = rop.getNameForDelete();
  // Assert.assertNotNull(ids.remove(id));
  // try
  // {
  // Assert.assertFalse(deleted.contains(id));
  // }
  // catch (final AssertionError e)
  // {
  // throw e;
  // }
  // deleted.put(id, id);
  // }
  // catch (final OOMStopException e)
  // {
  // e.printStackTrace();
  // System.exit(-1);
  // }
  // }
  // return null;
  // }
  // };
  // runningThreads.add(executor.submit(thread));
  // }
  //
  // Thread.sleep(10000);
  // running.set(false);
  // for (final Future<Void> future : runningThreads)
  // {
  // future.get();
  // }
  // rop.testComplete();
  // long objectCount = rop.getSavedObjectCount();
  //
  // do
  // {
  // Assert.assertEquals(ids.size(), objectCount);
  // System.out.println("ids.size() and objectCount both " + objectCount);
  // final RandomObjectPopulator verify =
  // new RandomObjectPopulator(this.vaultId, "", 100000, 5 * 1000);
  // try
  // {
  // while (true)
  // {
  // final ObjectName id = verify.getNameForDelete();
  // Assert.assertEquals(id, ids.remove(id));
  // }
  // }
  // catch (final OOMStopException e)
  // {
  // verify.testComplete();
  // objectCount = verify.getSavedObjectCount();
  // }
  // } while (objectCount > 0);
  // Assert.assertEquals(0, ids.size());
  // }

  // @Test
  // public void testInvalidRetention() throws InterruptedException, ExecutionException, IOException
  // {
  // final int MAX_OBJECTS = 100000;
  // final RandomObjectPopulator rop =
  // new RandomObjectPopulator(this.vaultId, "", MAX_OBJECTS, 5 * 1000);
  // final ConcurrentHashMap<ObjectName, ObjectName> ids =
  // new ConcurrentHashMap<ObjectName, ObjectName>();
  // final ConcurrentHashMap<ObjectName, ObjectName> deleted =
  // new ConcurrentHashMap<ObjectName, ObjectName>();
  //
  // for (int i = 0; i < MAX_OBJECTS / 2; i++)
  // {
  // final ObjectName id = generateId();
  // ids.put(id, id);
  // rop.writeNameComplete(id);
  // }
  // final AtomicBoolean running = new AtomicBoolean(true);
  //
  // final ExecutorService executor = Executors.newCachedThreadPool();
  //
  // final List<Future<Void>> runningThreads = new ArrayList<Future<Void>>();
  //
  // // write threads
  // for (int i = 0; i < 1; i++)
  // {
  // final Callable<Void> thread = new Callable<Void>()
  // {
  // @Override
  // public Void call() throws Exception
  // {
  // while (running.get())
  // {
  // final ObjectName id = generateId();
  // ids.put(id, id);
  // rop.writeNameComplete(id);
  // Thread.sleep(10);
  // }
  // return null;
  // }
  //
  // };
  // runningThreads.add(executor.submit(thread));
  // }
  // // delete threads
  // for (int i = 0; i < 20; i++)
  // {
  // final Callable<Void> thread = new Callable<Void>()
  // {
  // @Override
  // public Void call() throws Exception
  // {
  // while (running.get())
  // {
  // try
  // {
  // final ObjectName id = rop.getNameForDelete();
  // Assert.assertNotNull(ids.remove(id));
  // try
  // {
  // Assert.assertFalse(deleted.contains(id));
  // }
  // catch (final AssertionError e)
  // {
  // throw e;
  // }
  // deleted.put(id, id);
  // }
  // catch (final OOMStopException e)
  // {
  // e.printStackTrace();
  // System.exit(-1);
  // }
  // }
  // return null;
  // }
  // };
  // runningThreads.add(executor.submit(thread));
  // }
  //
  // // read threads
  // for (int i = 0; i < 1; i++)
  // {
  // final Callable<Void> thread = new Callable<Void>()
  // {
  // @Override
  // public Void call() throws Exception
  // {
  // while (running.get())
  // {
  // try
  // {
  // final ObjectName id = rop.acquireNameForRead();
  // Assert.assertEquals(id, ids.get(id));
  // rop.releaseNameFromRead(id);
  // }
  // catch (final OOMStopException e)
  // {
  // e.printStackTrace();
  // System.exit(-1);
  // }
  // }
  // return null;
  // }
  // };
  // runningThreads.add(executor.submit(thread));
  // }
  //
  // Thread.sleep(1000 * 60 * 60);
  // running.set(false);
  // for (final Future<Void> future : runningThreads)
  // {
  // future.get();
  // }
  // rop.testComplete();
  // long objectCount = rop.getSavedObjectCount();
  //
  // do
  // {
  // Assert.assertEquals(ids.size(), objectCount);
  // System.out.println("ids.size() and objectCount both " + objectCount);
  // final RandomObjectPopulator verify =
  // new RandomObjectPopulator(this.vaultId, "", 100000, 5 * 1000);
  // try
  // {
  // while (true)
  // {
  // final ObjectName id = verify.getNameForDelete();
  // Assert.assertEquals(id, ids.remove(id));
  // }
  // }
  // catch (final OOMStopException e)
  // {
  // verify.testComplete();
  // objectCount = verify.getSavedObjectCount();
  // }
  // } while (objectCount > 0);
  // Assert.assertEquals(0, ids.size());
  // }
}
