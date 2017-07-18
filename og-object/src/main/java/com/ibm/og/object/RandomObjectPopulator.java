/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.object;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.util.concurrent.Uninterruptibles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

@Singleton
public class RandomObjectPopulator extends Thread implements ObjectManager {
  private static final Logger _logger = LoggerFactory.getLogger(RandomObjectPopulator.class);
  public static final int OBJECT_SIZE = LegacyObjectMetadata.OBJECT_SIZE;
  private static final int MAX_PERSIST_ARG = 30 * 1000 * 60;
  public static final int MAX_OBJECT_ARG = 100 * (1048576 / OBJECT_SIZE);
  private final int maxObjects;
  private final String directory;
  private final String prefix;
  private final long persistFrequency;
  private final Integer objectFileIndex;
  public static final String SUFFIX = ".object";
  private final Pattern filenamePattern;

  // object read from a file
  private final RandomAccessConcurrentHashSet<ObjectMetadata> objects =
      new RandomAccessConcurrentHashSet<ObjectMetadata>();
  private final ReadWriteLock objectsLock = new ReentrantReadWriteLock(true);
  private final SortedMap<String, Integer> currentlyReading =
      Collections.synchronizedSortedMap(new TreeMap<String, Integer>());
  private final SortedMap<String, ObjectMetadata> currentlyUpdating =
          Collections.synchronizedSortedMap(new TreeMap<String, ObjectMetadata>());
  private final ReadWriteLock readingLock = new ReentrantReadWriteLock(true);
  private final ReadWriteLock persistLock = new ReentrantReadWriteLock(true);
  private final File saveFile;
  private volatile boolean testEnded = false;
  private final int idFileIndex;
  private final Random rand = new Random();
  private final UUID vaultId;
  private final ScheduledExecutorService saver;

  class IdFilter implements FilenameFilter {
    @Override
    public boolean accept(final File dir, final String name) {
      return RandomObjectPopulator.this.filenamePattern.matcher(name).matches();
    }
  }

  public static int getObjectSize() {
    return OBJECT_SIZE;
  }

  public RandomObjectPopulator(final UUID vaultId) {
    this(vaultId, "");
  }

  @Inject
  public RandomObjectPopulator(@Named("objectfile.location") final String directory,
      @Named("objectfile.name") final String prefix,
      @Named("objectfile.maxsize") final long maxSize,
      @Named("objectfile.persistfrequency") final long persistFrequency,
      @Named("objectfile.index") @Nullable final Integer objectFileIndex) {
    this(UUID.randomUUID(), directory, prefix, (int) (maxSize / OBJECT_SIZE),
        persistFrequency * 1000, objectFileIndex);
  }

  public RandomObjectPopulator(final UUID vaultId, final String directory, final String prefix) {
    this(vaultId, directory, prefix, MAX_OBJECT_ARG, MAX_PERSIST_ARG, null);
  }

  public RandomObjectPopulator(final UUID vaultId, final String prefix) {
    this(vaultId, ".", prefix, MAX_OBJECT_ARG, MAX_PERSIST_ARG, null);
  }

  public RandomObjectPopulator(final UUID vaultId, final int maxObjects) {
    this(vaultId, ".", "", maxObjects, MAX_PERSIST_ARG, null);
  }

  public RandomObjectPopulator(final UUID vaultId, final String prefix, final int maxObjects) {
    this(vaultId, ".", prefix, maxObjects, MAX_PERSIST_ARG, null);
  }

  public RandomObjectPopulator(final UUID vaultId, final String directory, final String prefix,
      final int maxObjectCount, final long persistTime, final Integer objectFileIndex) {
    this.vaultId = checkNotNull(vaultId);
    this.directory = checkNotNull(directory);
    if (prefix != null && !prefix.isEmpty()) {
      this.prefix = prefix;
    } else {
      this.prefix = "id_";
    }
    this.filenamePattern = Pattern
        .compile(String.format("%s(\\d|[1-9]\\d*)%s", this.prefix, RandomObjectPopulator.SUFFIX));
    checkArgument(maxObjectCount > 0, "maxObjectCount must be > 0 [%s]", maxObjectCount);
    this.maxObjects = maxObjectCount;
    this.persistFrequency = persistTime;
    this.objectFileIndex = objectFileIndex;
    final File[] files = getIdFiles();
    if (files != null && files.length > 1) {
      this.idFileIndex = selectInitialObjectFile(files.length, objectFileIndex);

      _logger.info("Initial object files list");
      for (final File f : files) {
        _logger.info("{}", f);
      }
    } else {
      this.idFileIndex = 0;
    }
    _logger.info("Initial object file index {}", this.idFileIndex);
    this.saveFile = createFile(this.idFileIndex);

    loadObjects();

    this.saver = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setDaemon(true)
        .setNameFormat("scheduled-object-persist").build());
    this.saver.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        try {
          persistIds();
        }

        catch (final IOException e) {
          _logger.error("Can't store id file", e);
        }
      }
      // Every 30 minutes
    }, persistTime, persistTime, TimeUnit.MILLISECONDS);
  }

  private int selectInitialObjectFile(final int objectFileCount, final Integer objectFileIndex) {
    if (objectFileIndex != null) {
      checkArgument(objectFileIndex >= 0, "index must be >= 0 [%s]", objectFileIndex);
      return Math.min(objectFileCount - 1, objectFileIndex);
    }
    return this.rand.nextInt(objectFileCount - 1);
  }

  private void loadObjects() {
    this.objects.clear();
    try {
      final byte[] objectBytes = new byte[OBJECT_SIZE];

      if (this.saveFile.exists()) {
        _logger.debug("loading objects from file: {}", this.saveFile);
        final InputStream input = new BufferedInputStream(new FileInputStream(this.saveFile));
        while (input.read(objectBytes) == OBJECT_SIZE) {
          final ObjectMetadata id = LegacyObjectMetadata.fromBytes(objectBytes);
          this.objects.put(id);
        }
        input.close();
      }
    } catch (final Exception e) {
      this.testEnded = true;
      _logger.error("", e);
    }
  }

  private File[] getIdFiles() {
    final File dir = new File(this.directory);
    return dir.listFiles(new IdFilter());
  }

  public long getSavedObjectCount() {
    long count = 0;
    final File[] idFiles = getIdFiles();
    for (final File file : idFiles) {
      count += file.length() / OBJECT_SIZE;
    }
    return count;
  }

  public long getCurrentObjectCount() {
    return this.objects.size();
  }

  @Override
  public ObjectMetadata remove() {
    this.persistLock.readLock().lock();
    try {
      ObjectMetadata id = null;
      while (id == null) {
        this.objectsLock.writeLock().lock();
        id = this.objects.removeRandom();
        this.objectsLock.writeLock().unlock();
        checkForNull(id);
        boolean unavailable;
        this.readingLock.readLock().lock();
        unavailable = this.currentlyReading.containsKey(id.getName());
        this.readingLock.readLock().unlock();
        if (unavailable) {
          this.objects.put(id);
          id = null;
        }
      }
      _logger.debug("Removing object: {}", id);
      return id;
    } finally {
      this.persistLock.readLock().unlock();
    }
  }

  @Override
  public ObjectMetadata removeForUpdate() {
    this.persistLock.readLock().lock();
    try {
      ObjectMetadata id = null;
      while (id == null) {
        this.objectsLock.writeLock().lock();
        id = this.objects.removeRandom();
        this.objectsLock.writeLock().unlock();
        checkForNull(id);
        boolean unavailable;
        this.readingLock.readLock().lock();
        unavailable = this.currentlyReading.containsKey(id.getName());
        this.readingLock.readLock().unlock();
        if (unavailable) {
          this.objects.put(id);
          id = null;
        }
      }
      _logger.debug("Removing object: {}", id);
      this.currentlyUpdating.put(id.getName(), id);
      return id;
    } finally {
      this.persistLock.readLock().unlock();
    }
  }
  @Override
  public ObjectMetadata removeObject(ObjectMetadata objectMetadata) {
    this.persistLock.readLock().lock();
    try {
      ObjectMetadata id = null;
      while (id == null) {
        this.objectsLock.writeLock().lock();
        id = this.objects.remove(objectMetadata);
        this.objectsLock.writeLock().unlock();
        checkForNull(id);
        boolean unavailable;
        this.readingLock.readLock().lock();
        unavailable = this.currentlyReading.containsKey(id.getName());
        this.readingLock.readLock().unlock();
        if (unavailable) {
          _logger.info("object {} is available already in currently reading. so skipping", id.getName());
          this.objects.put(id);
          id = null;
        }
      }
      _logger.trace("Removing object: {}", id);
      this.currentlyUpdating.put(id.getName(), id);
      return id;
    } finally {
      this.persistLock.readLock().unlock();
    }

  }



  private void checkForNull(final ObjectMetadata id) {
    if (id == null) {
      throw new ObjectManagerException("No objects available.");
    }
  }

  @Override
  public ObjectMetadata get() {
    if (this.testEnded) {
      throw new RuntimeException("Test already ended");
    }

    ObjectMetadata id;

    this.objectsLock.readLock().lock();
    id = this.objects.getRandom();
    try {
      checkForNull(id);
    } catch (final ObjectManagerException e) {
      this.objectsLock.readLock().unlock();
      throw e;
    }

    int count = 0;
    this.readingLock.writeLock().lock();
    if (this.currentlyReading.containsKey(id.getName())) {
      // The only reason to have both locked simultaneously is to prevent an id from being
      // selected for deletion before it has been added to currentlyReading
      this.objectsLock.readLock().unlock();
      count = this.currentlyReading.get(id.getName()).intValue();
    }
    this.currentlyReading.put(id.getName(), Integer.valueOf(count + 1));
    if (count == 0) {
      this.objectsLock.readLock().unlock();
    }
    this.readingLock.writeLock().unlock();

    _logger.trace("Getting object: {}", id);
    return id;
  }

  @Override
  public ObjectMetadata getOnce() {
    if (this.testEnded) {
      throw new RuntimeException("Test already ended");
    }

    ObjectMetadata id = null;
    while (id == null) {
      this.objectsLock.readLock().lock();
      id = this.objects.getRandom();
      try {
        checkForNull(id);
      } catch (final ObjectManagerException e) {
        this.objectsLock.readLock().unlock();
        throw e;
      }

      this.readingLock.writeLock().lock();
      if (this.currentlyReading.containsKey(id.getName())) {
        // The only reason to have both locked simultaneously is to prevent an id from being
        // selected for deletion before it has been added to currentlyReading
        _logger.debug("object {} already found in currently reading",id.getName());
        id = null;
        Uninterruptibles.sleepUninterruptibly(20, TimeUnit.MILLISECONDS);
      } else {
        // The only reason to have both locked simultaneously is to prevent an id from being
        // selected for deletion before it has been added to currentlyReading
        this.currentlyReading.put(id.getName(),1);
        _logger.debug("adding object {} to currently reading",id.getName());
      }
      this.readingLock.writeLock().unlock();
      this.objectsLock.readLock().unlock();
    }
    _logger.trace("Getting currently not read object : {}", id);
    return id;
  }

  @Override
  public void getComplete(final ObjectMetadata id) {
    this.readingLock.writeLock().lock();
    Integer c = this.currentlyReading.get(id.getName());
    _logger.debug("id {} count {}", id, c);
    final int count = this.currentlyReading.get(id.getName()).intValue();
    if (count > 1) {
      this.currentlyReading.put(id.getName(), Integer.valueOf(count - 1));
      _logger.debug("decrementing {} from currentlyReading", id.getName());
    } else {
      _logger.debug("removing {} from currentlyReading", id.getName());
      this.currentlyReading.remove(id.getName());
    }
    this.readingLock.writeLock().unlock();
    _logger.trace("Returning read object: {}", id);
    return;
  }

  @Override
  public void add(final ObjectMetadata id) {
    _logger.debug("Adding object: {}", id);
    this.persistLock.readLock().lock();
    try {
      this.objects.put(id);
    } finally {
      this.persistLock.readLock().unlock();
    }
  }

  @Override
  public void updateObject(final ObjectMetadata id) {
    _logger.debug("Adding Updated object: {}", id);
    this.persistLock.readLock().lock();
    try {
      this.currentlyUpdating.remove(id.getName());
      this.objects.put(id);
    } finally {
      this.persistLock.readLock().unlock();
    }
  }

  @Override
  public void removeUpdatedObject(final ObjectMetadata id) {
    _logger.debug("Removing Updated object: {}", id);
    this.persistLock.readLock().lock();
    try {
      this.currentlyUpdating.remove(id.getName());
    } finally {
      this.persistLock.readLock().unlock();
    }
  }

  @Override
  public int getCurrentlyUpdatingCount() {
    this.persistLock.readLock().lock();
    int size = this.currentlyUpdating.size();
    this.persistLock.readLock().unlock();
    return size;
  }
  private void persistIds() throws IOException {
    _logger.info("persisting objects");
    this.persistLock.writeLock().lock();
    final int toSave = this.objects.size();
    _logger.info("number of objects to persist {}", toSave);
    final OutputStream out = new BufferedOutputStream(new FileOutputStream(this.saveFile));
    if (toSave > this.maxObjects) {
      for (int size = this.objects.size(); size > this.maxObjects; size = this.objects.size()) {
        final int numFiles = getIdFiles().length;
        File surplus = createFile(numFiles - 1);
        if (surplus.equals(this.saveFile) || (surplus.length() / OBJECT_SIZE) >= this.maxObjects) {
          // Create a new file
          surplus = createFile(numFiles);
        }
        final OutputStream dos = new BufferedOutputStream(new FileOutputStream(surplus, true));
        final int remaining = getRemaining(size, surplus);
        // While writing surplus, remove them from this.objects, to keep consistent with
        // this.savefile
        final Iterator<ObjectMetadata> iterator = this.objects.iterator();
        for (int i = 0; i < remaining; i++) {
          final ObjectMetadata sid = iterator.next();
          dos.write(sid.toBytes());
          iterator.remove();
        }
        dos.close();
      }
    } else if (toSave < this.maxObjects) {
      for (int size = this.objects.size(); size < this.maxObjects; size = this.objects.size()) {
        // Try to borrow from last id file
        // When borrowing, add to this.objects
        // Count the number of objects to borrow and truncate file by that amount
        final int numFiles = getIdFiles().length;
        final File surplus = createFile(numFiles - 1);
        // Need to ensure last file is not current file
        // If it is, don't borrow at all
        if (this.saveFile.equals(surplus)) {
          break;
        }
        final int toTransfer = getTransferrable(size, surplus);
        final DataInputStream in = new DataInputStream(new FileInputStream(surplus));
        final long skip = surplus.length() - (toTransfer * OBJECT_SIZE);
        in.skip(skip);
        final byte[] buf = new byte[OBJECT_SIZE];
        for (int i = 0; i < toTransfer; i++) {
          if (in.read(buf) == OBJECT_SIZE) {
            final ObjectMetadata sid = LegacyObjectMetadata.fromBytes(buf);
            this.objects.put(sid);
          }
        }
        in.close();

        // If surplus is out of objects, delete it
        if (skip == 0) {
          surplus.delete();
        } else {
          // We borrowed from the end of the file so nothing is lost from truncating
          RandomAccessFile truncater = null;
          try {
            truncater = new RandomAccessFile(surplus, "rwd");
            truncater.setLength(skip);
          } finally {
            if (truncater != null) {
              truncater.close();
            }
          }
        }
      }
    }

    // Finally we save a number less than or equal to the maximum number of objects to our
    // savefile
    _logger.info(
        String.format("Writing state file: %d objects into ", this.objects.size()) + this.saveFile);
    for (final Iterator<ObjectMetadata> iterator = this.objects.iterator(); iterator.hasNext();) {
      out.write(iterator.next().toBytes());
    }
    Set<String> ids = this.currentlyUpdating.keySet();
    for (String id : ids) {
      out.write(this.currentlyUpdating.get(id).toBytes());
    }
    out.close();
    this.persistLock.writeLock().unlock();
  }

  private int getRemaining(final int size, final File surplus) {
    final int objectsAvailable = size - this.maxObjects;
    final int spaceAvailable = this.maxObjects - ((int) (surplus.length() / OBJECT_SIZE));
    final int remaining = Math.min(objectsAvailable, spaceAvailable);
    _logger.debug("remaining objects {}", remaining);
    return remaining;
  }

  private int getTransferrable(final int size, final File surplus) {
    final int slotsAvailable = this.maxObjects - size;
    final int surplusAvailable = (int) (surplus.length() / OBJECT_SIZE);
    final int transferrable = Math.min(slotsAvailable, surplusAvailable);
    _logger.debug("transferrable objects {}", transferrable);
    return transferrable;
  }

  private File createFile(final int idx) {
    return new File(this.directory + "/" + this.prefix + idx + SUFFIX);
  }

  @Override
  public void shutdown() {
    _logger.info("shutting down object manager");
    this.testEnded = true;
    shutdownSaverThread();
    try {
      join();
    } catch (final InterruptedException e) {
      throw new RuntimeException("Failed to join");
    }

    try {
      persistIds();
    } catch (final Exception e) {
      throw new ObjectManagerException(e);
    }
    _logger.info("object manager is shutdown");
  }

  private void shutdownSaverThread() {
    this.saver.shutdown();
    while (!this.saver.isTerminated()) {
      try {
        this.saver.awaitTermination(10, TimeUnit.SECONDS);

      }

      catch (final InterruptedException e)

      {
        _logger.error("", e);
      }
    }
  }

  @Override
  public String toString() {
    return String.format(
        "RandomObjectPopulator [maxObjects=%s, directory=%s, prefix=%s, persistFrequency=%s, objectFileIndex=%s]",
        this.maxObjects, this.directory, this.prefix, this.persistFrequency, this.objectFileIndex);
  }
}
