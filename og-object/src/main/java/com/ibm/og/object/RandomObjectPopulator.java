/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.object;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.ibm.og.util.ObjectManagerUtils.getFileIndex;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
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

import com.google.common.io.BaseEncoding;
import com.google.common.util.concurrent.Uninterruptibles;
import com.ibm.og.util.ObjectManagerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

@Singleton
public class RandomObjectPopulator extends Thread implements ObjectManager {
  private static final Logger _logger = LoggerFactory.getLogger(RandomObjectPopulator.class);
  public static final int OBJECT_SIZE_V1 = 30;
  public static final int OBJECT_SIZE_V2 = 35;
  public static final int OBJECT_SIZE = LegacyObjectMetadata.OBJECT_SIZE;
  public static final int MAX_PERSIST_ARG = 30 * 1000 * 60;
  public static final int MAX_OBJECT_ARG = 100 * (1048576 / OBJECT_SIZE);
  private int maxObjects;
  private long maxSize = -1;
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
  private final int desiredFileIndex;
  private final int idFileIndex;
  private final Random rand = new Random();
  private final UUID vaultId;
  private final ScheduledExecutorService saver;
  private boolean objectVersionOn;

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
    this(UUID.randomUUID(), directory, prefix, maxSize, (int) (maxSize / OBJECT_SIZE),
        persistFrequency * 1000, objectFileIndex);
  }

  public RandomObjectPopulator(final UUID vaultId, final String directory, final String prefix) {
    this(vaultId, directory, prefix, -1, MAX_OBJECT_ARG, MAX_PERSIST_ARG, null);
  }

  public RandomObjectPopulator(final UUID vaultId, final String prefix) {
    this(vaultId, ".", prefix, -1, MAX_OBJECT_ARG, MAX_PERSIST_ARG, null);
  }

  public RandomObjectPopulator(final UUID vaultId, final int maxObjects) {
    this(vaultId, ".", "", -1, maxObjects, MAX_PERSIST_ARG, null);
  }

  public RandomObjectPopulator(final UUID vaultId, final String prefix, final int maxObjects) {
    this(vaultId, ".", prefix, -1, maxObjects, MAX_PERSIST_ARG, null);
  }

  public RandomObjectPopulator(final UUID vaultId, final String directory, final String prefix,
      final long maxSize, final int maxObjectCount, final long persistTime, final Integer objectFileIndex) {
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
    this.maxSize = maxSize;
    this.persistFrequency = persistTime;
    this.objectFileIndex = objectFileIndex;
    final File[] files = ObjectManagerUtils.getIdFiles(this.prefix, this.SUFFIX, this.directory);
    if (files != null && files.length > 1) {
      if (objectFileIndex != null) {
        this.desiredFileIndex = objectFileIndex;
      } else {
        this.desiredFileIndex = selectInitialObjectFile(files.length, objectFileIndex, files);
      }
      this.idFileIndex = selectInitialObjectFile(files.length, objectFileIndex, files);

      _logger.info("Initial object files list");
      for (final File f : files) {
        _logger.info("{}", f);
      }
    } else {
      this.idFileIndex = 0;
      this.desiredFileIndex = 0;
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

  private int selectInitialObjectFile(final int objectFileCount, final Integer objectFileIndex, final File[] files) {
    if (objectFileIndex != null) {
      if (objectFileIndex == -1) {
       // select the max index available
        return getFileMaxIndex(files);
      } else {
        //checkArgument(objectFileIndex >= 0, "index must be >= 0 [%s]", objectFileIndex);
        return Math.min(objectFileCount - 1, objectFileIndex);
      }
    }
    return this.rand.nextInt(objectFileCount - 1);
  }

  private int getFileMaxIndex(final File[] files) {
    ObjectManagerUtils.ObjectFileNameIndexComparator comparator = new ObjectManagerUtils.ObjectFileNameIndexComparator(prefix);
    Collections.sort(Arrays.asList(files), comparator);
    String fileWithMaxIndex = files[files.length-1].getName();
    int maxIndex = getFileIndex(prefix, fileWithMaxIndex);
    _logger.info("current max_index is {}", maxIndex);
    return maxIndex;
  }

  private void loadObjects() {
    this.objects.clear();
    try {
      int actualObjectSize = OBJECT_SIZE;
      if (this.saveFile.exists()) {
        _logger.debug("loading objects from file: {}", this.saveFile);
        final InputStream input = new BufferedInputStream(new FileInputStream(this.saveFile));

        int versionHeaderLength = 0;
        int objectFileHeaderLength = 0;
        ObjectFileHeader header = null;
        ObjectFileVersion version = ObjectFileUtil.readObjectFileVersion(input);
        if (version.getMajorVersion() >= 3 && version.getMinorVersion() >= 0) {
          header = ObjectFileUtil.readObjectFileHeader(input);
          actualObjectSize = LegacyObjectMetadata.getObjectRecordSize(header);
          versionHeaderLength = ObjectFileVersion.VERSION_HEADER_LENGTH;
          objectFileHeaderLength = ObjectFileHeader.HEADER_LENGTH;
        } else if (version.getMajorVersion() == 2 && version.getMinorVersion() == 0) {
          versionHeaderLength = ObjectFileVersion.VERSION_HEADER_LENGTH;
          actualObjectSize = OBJECT_SIZE_V2;
        } else if (version.getMajorVersion() == 1 && version.getMinorVersion() == 0) {
          actualObjectSize = OBJECT_SIZE_V1;
        } else {
          throw new IllegalArgumentException("Unsupported Object File version [%sd].[%s]".
                  format(Byte.toString(version.getMajorVersion()), Byte.toString(version.getMinorVersion())));
        }
        input.reset();
        input.skip(versionHeaderLength + objectFileHeaderLength);
        final byte[] objectBytes;
        if (header != null) {
          // use the size calculated based on the whether the object version is present or not
          objectBytes = new byte[actualObjectSize];
          if (header.getObjectVersionLen() > 0) {
            this.objectVersionOn = true;
          }
        } else {
          // even if the object file is version 1.0, the read object is returned as 2.0 format
          objectBytes = new byte[OBJECT_SIZE_V2];
        }
        final byte[] inputBytes = new byte[actualObjectSize];
        //ByteBuffer b2 = ByteBuffer.wrap(objectBytes);

        ObjectMetadata id;
        while (input.read(inputBytes) == actualObjectSize) {
          id = ObjectFileUtil.getObjectFromInputBuffer(version.getMajorVersion(), version.getMinorVersion(),
                  inputBytes, objectBytes, (header != null && header.getObjectVersionLen()>0));
          this.objects.put(id);
        }
        _logger.info("No. of objects loaded {}", this.objects.size());
        input.close();
      }
    } catch (final Exception e) {
      this.testEnded = true;
      _logger.error("", e);
    }
  }


  public long getSavedObjectCount() {
    long count = 0;
    final File[] idFiles = ObjectManagerUtils.getIdFiles(this.prefix, this.SUFFIX, this.directory);
    for (final File file : idFiles) {
      try {
        int objectSize = ObjectFileUtil.getObjectSize(file);
        count += file.length() / objectSize;
      } catch (FileNotFoundException fne) {
        _logger.warn("getSavedObjectCount: FileNotFoundException {}", file.getName());
      } catch (IOException ioe) {
        _logger.warn("getSavedObjectCount: IOException {}", file.getName());
      }
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
      if (id.hasVersion()) {
        this.objectVersionOn = true;
      }
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
  public ObjectMetadata getObjectFromUpdatingCache(final String id) {
    _logger.debug("Getting object {} from currentUpdating cache", id);
    ObjectMetadata objectId = null;
    this.persistLock.readLock().lock();
    try {
      objectId = this.currentlyUpdating.get(id);
    } finally {
      this.persistLock.readLock().unlock();
    }
    return objectId;
  }

  @Override
  public void removeUpdatedObject(final ObjectMetadata id) {
    _logger.trace("Removing Updated object from currentlyUpdating cache: {}", id);
    this.persistLock.readLock().lock();
    try {
      this.currentlyUpdating.remove(id.getName());
    } finally {
      this.persistLock.readLock().unlock();
    }
  }

  @Override
  public void removeUpdatedObjectByName(final String name) {
    _logger.trace("Removing Updated object from currentlyUpdating cache: {}", name);
    this.persistLock.readLock().lock();
    try {
      this.currentlyUpdating.remove(name);
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
    _logger.info("number of objects to persist [{}]", toSave);
    OutputStream out = new BufferedOutputStream(new FileOutputStream(this.saveFile));
    boolean flipVersionInfoInHeader = false;
    if (toSave > 0) {
      _logger.info("writing version to file {}", this.saveFile);
      ObjectFileUtil.writeObjectFileVersion(out);
      if (this.objectVersionOn) {
        ObjectFileUtil.writeObjectFileHeader(out, LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE);
        //recalculate maxObjects accordingly
        if (this.maxSize != -1) {
          this.maxObjects = (int)(this.maxSize / (LegacyObjectMetadata.OBJECT_SIZE + LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE));
        }
      } else {
        ObjectFileUtil.writeObjectFileHeader(out, 0);
      }
    }
    _logger.info("toSave [{}] maxObjects [{}]", toSave, this.maxObjects);
    if (toSave > this.maxObjects) {
      for (int size = this.objects.size(); size > this.maxObjects; size = this.objects.size()) {
        // surplus file index should be looked based on the max index currently. This will change or gaps
        // could be created because of the compaction / shuffling
        int surplusIndex;
        final File[] files = ObjectManagerUtils.getIdFiles(this.prefix, this.SUFFIX, this.directory);
        final int numFiles = files.length;
        if (desiredFileIndex == -1) {
          surplusIndex = getFileMaxIndex(files);
        } else {
          surplusIndex = numFiles - 1;
        }
        File surplus = createFile(surplusIndex);
        if (surplus.equals(this.saveFile) || (surplus.length() / OBJECT_SIZE) >= this.maxObjects) {
          // Create a new file
          surplusIndex = surplusIndex + 1;
          surplus = createFile(surplusIndex);
        }
        _logger.info("surplus file [{}]", surplus.getName());


        OutputStream dos = new BufferedOutputStream(new FileOutputStream(surplus, true));
        // write header if only if it is a new file
        if (surplus.length() == 0) {
          _logger.info("writing version in surplus file [{}]", surplus.getName());
          ObjectFileUtil.writeObjectFileVersion(dos);
          if (this.objectVersionOn) {
            ObjectFileUtil.writeObjectFileHeader(dos, LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE);
          } else {
            ObjectFileUtil.writeObjectFileHeader(dos, 0);
          }
        }
        dos.close(); // close stream and open again after upgrade
        // check if the surplus file needs upgrade, versioning field
        ObjectFileUtil.upgrade(surplus, this.objectVersionOn);
        dos = new BufferedOutputStream(new FileOutputStream(surplus, true));
        final int remaining = getRemaining(size, surplus);
        _logger.info("remaining objects [{}] to write in surplus ", remaining);
        // While writing surplus, remove them from this.objects, to keep consistent with
        // this.savefile
        final Iterator<ObjectMetadata> iterator = this.objects.iterator();
        for (int i = 0; i < remaining; i++) {
          final ObjectMetadata sid = iterator.next();
          if (this.objectVersionOn) {
            byte[] bytes = sid.toBytes(true);
            dos.write(sid.toBytes(true));
          } else {
            dos.write(sid.toBytes(false));
          }
          iterator.remove();
        }
        dos.close();
      }
    } else if (toSave < this.maxObjects) {
      for (int size = this.objects.size(); size < this.maxObjects; size = this.objects.size()) {
        // Try to borrow from last id file
        // When borrowing, add to this.objects
        // Count the number of objects to borrow and truncate file by that amount
        if (this.desiredFileIndex == -1) {
          // if shuffling / compaction enabled already one with object-file script, skip borrowing
          break;
        }
        final int numFiles = ObjectManagerUtils.getIdFiles(this.prefix, this.SUFFIX, this.directory).length;
        final File surplus = createFile(numFiles-1);
        // Need to ensure last file is not current file
        // If it is, don't borrow at all
        if (this.saveFile.equals(surplus)) {
          break;
        }
        final int toTransfer = getTransferrable(size, surplus);
        _logger.info("borrow [{}] objects from [{}]", toTransfer, surplus.getName());

        final BufferedInputStream in = new BufferedInputStream(new FileInputStream(surplus));
        // check the version of the file and calculate skip
        long skip = 0;
        byte[] readBuf = null;
        int actualObjectSize = 0;
        in.mark(ObjectFileVersion.VERSION_HEADER_LENGTH);
        ObjectFileVersion version = ObjectFileUtil.readObjectFileVersion(in);
        boolean objectVersionPresent = false;
        byte[] objectBuf = new byte[actualObjectSize]; // correct size will be allocated based on the borrowed object
        if (version.getMajorVersion() >= 3 && version.getMinorVersion() >= 0) {
          _logger.info("borrowing from object file {} of length {} version 3.0 ", surplus.getName(),
                  surplus.length());
          // calculate object size by reading file header
          ObjectFileHeader header = ObjectFileUtil.readObjectFileHeader(in);
          int objectSize = LegacyObjectMetadata.getObjectRecordSize(header);
          if (header.getObjectVersionLen() > 0) {
            objectVersionPresent = true;
          }
          skip = surplus.length() - (toTransfer * objectSize);
          readBuf = new byte[objectSize];
          actualObjectSize = objectSize;
          objectBuf = new byte[actualObjectSize];
        } else if (version.getMajorVersion() == 2 && version.getMinorVersion() == 0) {
          _logger.info("borrowing from object file {} of length {} version 2.0 ", surplus.getName(),
                  surplus.length());
          skip = surplus.length() - (toTransfer * OBJECT_SIZE_V2);
          readBuf = new byte[OBJECT_SIZE_V2];
          actualObjectSize = OBJECT_SIZE_V2;
          objectBuf = new byte[actualObjectSize];
        } else if (version.getMajorVersion() == 1 && version.getMinorVersion() == 0) {
          _logger.info("borrowing from object file {} of length {} version 1.0 ", surplus.getName(),
                  surplus.length());
          if (in.markSupported()) {
            _logger.warn("Missing version in object file [%s].", surplus.getName());
            readBuf = new byte[OBJECT_SIZE_V1];
            actualObjectSize = OBJECT_SIZE_V1;
            objectBuf = new byte[OBJECT_SIZE_V2];
            // allocate objectBuf size to include the retention, legalholds.
            // ObjectFileUtil.getObjectFromInputBuffer fills in default values for retention and legalholds.
          }
          skip = surplus.length() - (toTransfer * OBJECT_SIZE_V1);
        } else {
          throw new IllegalArgumentException("Unsupported Object file version");
        }

        in.reset();
        _logger.info("skip object file length [{}]", skip);
        long skippedBytes = 0;
        while(skippedBytes < skip) {
          skippedBytes += in.skip(skip - skippedBytes);
        }
        _logger.info("skippedBytes [{}]", skippedBytes);
        final byte[] buf = new byte[actualObjectSize];
        ObjectMetadata sid;

        for (int i = 0; i < toTransfer; i++) {
          int readBytes = in.read(readBuf, 0, actualObjectSize);
          if (readBytes == actualObjectSize) {
            sid = ObjectFileUtil.getObjectFromInputBuffer(version.getMajorVersion(), version.getMinorVersion(),
                    readBuf, objectBuf, objectVersionPresent);
            _logger.trace("borrowed object [{}]", sid);
            // if the read object has a version but the current object file does not have the version
            // update the header, update all objects with 0 bytes versionid. this happens when writing the
            // object to the file
            // if the current object file has version Id, the read object does not add the 0 bytes versionId
            // and then save it.
            if (!this.objectVersionOn && objectVersionPresent) {
              // if this objectfile does not have version Ids and the borrowed object has version Id
              flipVersionInfoInHeader = true;
              this.objectVersionOn = true;
            }
            if (this.objectVersionOn && !objectVersionPresent) {
              // if this object file has version Ids but the borrowed object does not then set the version id
              // of the borrowed object as 0 bytes
              byte[] newBytes = sid.toBytes(true);
              sid = LegacyObjectMetadata.fromBytes(newBytes, true);
            }

            this.objects.put(sid);

          } else {
            _logger.error("borrow object readBytes [{}] not equal to object length [{}]", readBytes, actualObjectSize);
          }
        }
        in.close();
        // If surplus is out of objects, delete it
        // skip may be VERSION_HEADER_LENGTH or VERSION_HEADER_LENGTH + OBJECT_FILE_HEADER_LEN based on the
        // object file version
        int checkLength = 0;
        if (version.getMajorVersion() >= 3 && version.getMinorVersion() >= 0) {
          checkLength = ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH;
        } else if (version.getMajorVersion() == 2 && version.getMinorVersion() >= 0){
          checkLength = ObjectFileVersion.VERSION_HEADER_LENGTH;
        } else {
          checkLength = 0;
        }
        if (skip == checkLength) {
          _logger.info("deleting surplus file [{}]", surplus.getName());
          surplus.delete();
        } else {
          // We borrowed from the end of the file so nothing is lost from truncating
          RandomAccessFile truncater = null;
          try {
            _logger.info("truncating surplus file [{}] to [{}] ", surplus.getName(), skip);
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

    if (flipVersionInfoInHeader) {
      // if the versioning header was changed in the saveFile because of the borrowed object
      // rewrite the objectFile header
      out.close();
      out = new BufferedOutputStream(new FileOutputStream(this.saveFile));
        _logger.info("writing version to file {}", this.saveFile);
        ObjectFileUtil.writeObjectFileVersion(out);
        if (this.objectVersionOn) {
          ObjectFileUtil.writeObjectFileHeader(out, LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE);
          //recalculate maxObjects accordingly
          this.maxObjects = (this.maxObjects * LegacyObjectMetadata.OBJECT_SIZE) /
                  (LegacyObjectMetadata.OBJECT_SIZE + LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE);
        }
    }

    for (final Iterator<ObjectMetadata> iterator = this.objects.iterator(); iterator.hasNext();) {
      if (this.objectVersionOn) {
        out.write(iterator.next().toBytes(true));
      } else {
        out.write(iterator.next().toBytes(false));
      }
    }
    out.close();
    this.persistLock.writeLock().unlock();
  }

  private int getRemaining(final int size, final File surplus) {
    final int objectsAvailable = size - this.maxObjects;
    int objectSize;
    try {
      objectSize = ObjectFileUtil.getObjectSize(surplus);
    } catch (FileNotFoundException fne) {
      _logger.warn("getRemaining: FileNotFoundException for file {}", surplus.getName());
      return 0;
    } catch (IOException ioe) {
      _logger.warn("getRemaining: IOException for file {}", surplus.getName());
      return 0;
    }
    final int spaceAvailable = this.maxObjects - ((int) (surplus.length() / objectSize));
    final int remaining = Math.min(objectsAvailable, spaceAvailable);
    return remaining;
  }

  private int getTransferrable(final int size, final File surplus) {
    final int slotsAvailable = this.maxObjects - size;
    int objectSize;
    try {
      objectSize = ObjectFileUtil.getObjectSize(surplus);
    } catch (FileNotFoundException fne) {
      _logger.warn("getRemaining: FileNotFoundException for file {}", surplus.getName());
      return 0;
    } catch (IOException ioe) {
      _logger.warn("getRemaining: IOException for file {}", surplus.getName());
      return 0;
    }
    final int surplusAvailable = (int) (surplus.length() / objectSize);
    final int transferrable = Math.min(slotsAvailable, surplusAvailable);
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
