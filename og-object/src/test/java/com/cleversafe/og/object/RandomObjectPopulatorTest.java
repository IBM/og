/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.object;

import java.io.File;
import java.io.FilenameFilter;
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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class RandomObjectPopulatorTest {
  final String dirName = "RandomObjectPopulatorTest";
  static final String prefix = "id_";
  static final String suffix = ".object";

  final protected UUID vaultId = new UUID(0, 0);

  static class IdFilter implements FilenameFilter {
    @Override
    public boolean accept(final File dir, final String name) {

      if (!name.startsWith(prefix, 0)) {
        return false;
      }

      try {
        final String number = name.substring(prefix.length(), name.length() - suffix.length());
        if (Integer.parseInt(number) < 0) {
          return false;
        }
      } catch (final Exception e) {
        return false;
      }

      return (name.endsWith(suffix));
    }
  }

  final static int OBJECT_SIZE = LegacyObjectMetadata.OBJECT_SIZE;
  protected final static int MAX_OBJECTS = 5;

  final Random rand = new Random();

  protected File[] getIdFiles() {
    final File dir = new File(".");
    final File[] files = dir.listFiles(new IdFilter());
    return files;
  }

  @Before
  public void copyIdFiles() {
    final File[] files = getIdFiles();
    final File directory = new File(this.dirName);
    if (!(directory.exists() && directory.isDirectory())) {
      directory.mkdir();
    }
    for (final File id : files) {
      id.renameTo(new File(directory, id.getName()));
    }
  }

  private void deleteTempFiles() {
    final File[] files = getIdFiles();
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
    rop.writeNameComplete(sid);
    Assert.assertEquals(sid, rop.acquireNameForRead());
    rop.testComplete();
    rop = new RandomObjectPopulator(this.vaultId);
    Assert.assertEquals(sid.toString(), rop.getNameForDelete().toString());
    rop.testComplete();
  }

  @Test
  public void deleteTest() throws ObjectManagerException {
    final ObjectMetadata firstId = generateId();
    ObjectMetadata secondId = firstId;
    while (secondId.equals(firstId)) {
      secondId = generateId();
    }
    RandomObjectPopulator rop = new RandomObjectPopulator(this.vaultId);
    rop.writeNameComplete(firstId);
    rop.writeNameComplete(secondId);
    final ObjectMetadata readId = rop.acquireNameForRead();
    final ObjectMetadata deleteId = rop.getNameForDelete();
    Assert.assertTrue((readId.equals(firstId) || readId.equals(secondId)));
    Assert.assertTrue((deleteId.equals(firstId) || deleteId.equals(secondId)));
    Assert.assertFalse(readId.equals(deleteId));
    rop.testComplete();
    rop = new RandomObjectPopulator(this.vaultId);
    Assert.assertFalse(deleteId.toString().equals(rop.acquireNameForRead().toString()));
    rop.testComplete();
  }

  @Test
  public void simultaneousReadDeleteTest() throws ObjectManagerException, InterruptedException,
      ExecutionException {
    final ObjectMetadata sid = generateId();
    final RandomObjectPopulator rop = new RandomObjectPopulator(this.vaultId);
    rop.writeNameComplete(sid);
    Assert.assertEquals(sid, rop.acquireNameForRead());
    // Try again for simultaneous reads
    Assert.assertEquals(sid, rop.acquireNameForRead());
    final boolean[] released = {false};
    class DeleteLoop implements Callable<Void> {
      final boolean[] released;

      public DeleteLoop(final boolean[] released) {
        this.released = released;
      }

      @Override
      public Void call() throws Exception {
        rop.getNameForDelete();
        Assert.assertTrue(this.released[0]);
        return null;
      }
    }
    final Future<Void> future =
        Executors.newSingleThreadExecutor().submit(new DeleteLoop(released));
    rop.releaseNameFromRead(sid);
    released[0] = true;
    // And again for the other read
    rop.releaseNameFromRead(sid);
    // The DeleteLoop should now complete
    future.get();
    rop.testComplete();
  }

  @Test
  public void overflowIdFile() throws ObjectManagerException {
    RandomObjectPopulator rop =
        new RandomObjectPopulator(this.vaultId, RandomObjectPopulatorTest.MAX_OBJECTS);
    for (int i = 0; i < RandomObjectPopulatorTest.MAX_OBJECTS; i++) {
      rop.writeNameComplete(generateId());
    }
    rop.testComplete();
    Assert.assertEquals(getIdFiles().length, 1);
    rop = new RandomObjectPopulator(this.vaultId, RandomObjectPopulatorTest.MAX_OBJECTS);
    rop.writeNameComplete(generateId());
    rop.testComplete();
    Assert.assertEquals(getIdFiles().length, 2);
    Assert.assertEquals(new File(prefix + 0 + suffix).length(),
        RandomObjectPopulatorTest.MAX_OBJECTS * OBJECT_SIZE);
    Assert.assertEquals(new File(prefix + 1 + suffix).length(), OBJECT_SIZE);
  }

  @Test
  public void overflowManyIdFile() throws ObjectManagerException {
    RandomObjectPopulator rop =
        new RandomObjectPopulator(this.vaultId, RandomObjectPopulatorTest.MAX_OBJECTS);
    for (int i = 0; i < RandomObjectPopulatorTest.MAX_OBJECTS; i++) {
      rop.writeNameComplete(generateId());
    }
    rop.testComplete();
    Assert.assertEquals(getIdFiles().length, 1);
    rop = new RandomObjectPopulator(this.vaultId, RandomObjectPopulatorTest.MAX_OBJECTS);
    for (int i = 0; i < 1 + RandomObjectPopulatorTest.MAX_OBJECTS * 3; i++) {
      rop.writeNameComplete(generateId());
    }
    rop.testComplete();
    Assert.assertEquals(getIdFiles().length, 5);
    for (int i = 0; i < 4; i++) {
      Assert.assertEquals(new File(prefix + i + suffix).length(),
          RandomObjectPopulatorTest.MAX_OBJECTS * OBJECT_SIZE);
    }
    Assert.assertEquals(new File(prefix + 4 + suffix).length(), OBJECT_SIZE);
  }

  @Test
  public void borrowIds() throws ObjectManagerException {
    RandomObjectPopulator rop =
        new RandomObjectPopulator(this.vaultId, RandomObjectPopulatorTest.MAX_OBJECTS);
    for (int i = 0; i < RandomObjectPopulatorTest.MAX_OBJECTS; i++) {
      rop.writeNameComplete(generateId());
    }
    // We write to objects to test borrowing one, and then two
    final int surplus = 2;
    for (int i = 0; i < surplus; i++) {
      rop.writeNameComplete(generateId());
    }
    rop.testComplete();

    Assert.assertEquals(getIdFiles().length, 2);
    Assert.assertEquals(rop.getSavedObjectCount(), RandomObjectPopulatorTest.MAX_OBJECTS + surplus);
    // Borrow one id
    rop = new RandomObjectPopulator(this.vaultId, RandomObjectPopulatorTest.MAX_OBJECTS);
    rop.getNameForDelete();
    rop.testComplete();
    Assert.assertEquals(getIdFiles().length, 2);
    Assert.assertEquals(new File(prefix + 1 + suffix).length(), OBJECT_SIZE);
    // Borrow last id from surplus file, causing that file to be deleted
    rop = new RandomObjectPopulator(this.vaultId, RandomObjectPopulatorTest.MAX_OBJECTS);
    rop.getNameForDelete();
    rop.testComplete();
    Assert.assertEquals(getIdFiles().length, 1);
    Assert.assertEquals(new File(prefix + 0 + suffix).length(),
        RandomObjectPopulatorTest.MAX_OBJECTS * OBJECT_SIZE);
  }

  @Test
  public void testMultipleFiles() throws ObjectManagerException {
    RandomObjectPopulator rop =
        new RandomObjectPopulator(this.vaultId, RandomObjectPopulatorTest.MAX_OBJECTS);
    for (int i = 0; i < RandomObjectPopulatorTest.MAX_OBJECTS * 4; i++) {
      rop.writeNameComplete(generateId());
    }
    rop.writeNameComplete(generateId());
    rop.testComplete();
    Assert.assertEquals(getIdFiles().length, 5);
    rop = new RandomObjectPopulator(this.vaultId, RandomObjectPopulatorTest.MAX_OBJECTS);
    rop.getNameForDelete();
    rop.testComplete();
    Assert.assertEquals(getIdFiles().length, 4);
    Assert.assertEquals(new File(prefix + 0 + suffix).length(),
        RandomObjectPopulatorTest.MAX_OBJECTS * OBJECT_SIZE);
  }

  @Test
  public void verifyReadIds() throws ObjectManagerException {
    RandomObjectPopulator rop =
        new RandomObjectPopulator(this.vaultId, RandomObjectPopulatorTest.MAX_OBJECTS);
    final ObjectMetadata[] savedIds = new ObjectMetadata[RandomObjectPopulatorTest.MAX_OBJECTS];
    for (int i = 0; i < RandomObjectPopulatorTest.MAX_OBJECTS; i++) {
      final ObjectMetadata sid = generateId();
      savedIds[i] = sid;
      rop.writeNameComplete(sid);
    }

    rop.testComplete();
    Assert.assertEquals(getIdFiles().length, 1);
    rop = new RandomObjectPopulator(this.vaultId, RandomObjectPopulatorTest.MAX_OBJECTS);
    final ObjectMetadata[] retrievedIds = new ObjectMetadata[RandomObjectPopulatorTest.MAX_OBJECTS];

    for (int i = 0; i < RandomObjectPopulatorTest.MAX_OBJECTS; i++) {
      final ObjectMetadata idForDelete = rop.getNameForDelete();
      retrievedIds[i] = idForDelete;
    }

    Arrays.sort(savedIds);
    Arrays.sort(retrievedIds);
    Assert.assertArrayEquals(savedIds, retrievedIds);

    rop.testComplete();
    Assert.assertEquals(getIdFiles().length, 1);
    Assert.assertEquals(new File(prefix + 0 + suffix).length(), 0);
  }

  protected ObjectMetadata generateId() {
    return LegacyObjectMetadata.fromMetadata(
        UUID.randomUUID().toString().replace("-", "") + "0000", 0);
  }

  @Ignore
  @Test
  public void testConcurrency() throws InterruptedException, ExecutionException,
      ObjectManagerException {
    final RandomObjectPopulator rop =
        new RandomObjectPopulator(this.vaultId, ".", "", 100000, 5 * 1000);
    final ConcurrentHashMap<ObjectMetadata, ObjectMetadata> ids =
        new ConcurrentHashMap<ObjectMetadata, ObjectMetadata>();

    for (int i = 0; i < 10000; i++) {
      final ObjectMetadata id = generateId();
      ids.put(id, id);
      rop.writeNameComplete(id);
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
            rop.writeNameComplete(id);
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
              final ObjectMetadata id = rop.acquireNameForRead();
              Assert.assertEquals(id, ids.get(id));
              rop.releaseNameFromRead(id);
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
              final ObjectMetadata id = rop.getNameForDelete();
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
    rop.testComplete();

    RandomObjectPopulator verify;
    do {
      verify = new RandomObjectPopulator(this.vaultId, ".", "", 100000, 5 * 1000);
      final long objectCount = verify.getSavedObjectCount();
      Assert.assertEquals(ids.size(), objectCount);
      while (true) {
        try {
          final ObjectMetadata id = verify.getNameForDelete();
          Assert.assertEquals(id, ids.remove(id));
        } catch (final ObjectManagerException e) {
          break;
        }
      }
      verify.testComplete();
    } while (verify.getSavedObjectCount() > 0);
    Assert.assertEquals(0, ids.size());
  }

  @Test
  public void testIdFilter() {
    final RandomObjectPopulator r = new RandomObjectPopulator(UUID.randomUUID(), "test");
    final FilenameFilter f = r.new IdFilter();
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
