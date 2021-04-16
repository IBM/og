/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.cli;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.google.common.io.Files;
import com.ibm.og.object.LegacyObjectMetadata;
import com.ibm.og.object.ObjectFileHeader;
import com.ibm.og.object.ObjectFileUtil;
import com.ibm.og.object.ObjectFileVersion;
import com.ibm.og.object.RandomObjectPopulator;
import com.ibm.og.util.ObjectManagerUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.og.cli.Application.Cli;
import com.ibm.og.object.ObjectMetadata;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;

/**
 * A cli for managing Object Generator object files
 * 
 * @since 1.0
 */
public class ObjectFile {
  private static final Logger _consoleLogger = LoggerFactory.getLogger("ConsoleLogger");
  private static final Logger _logger = LoggerFactory.getLogger(ObjectFile.class);
  private static final DateTimeFormatter FORMATTER =
          DateTimeFormat.forPattern("dd/MMM/yyyy:HH:mm:ss Z").withLocale(Locale.US);
  private ObjectFile() {}

  public static void main(final String[] args) {
    final ObjectFileGetOpt getopt = new ObjectFileGetOpt();
    final Cli cli = Application.cli("object-file", getopt, args);
    if (cli.shouldStop()) {
      if (cli.help()) {
        cli.printUsage();
      } else if (cli.version()) {
        cli.printVersion();
      } else if (cli.error()) {
        cli.printErrors();
        cli.printUsage();
        Application.exit(Application.TEST_ERROR);
      }
      Application.exit(0);
    }

    final List<File> inputFiles = getopt.getInput();
    final List<File> filesToShuffle = new ArrayList<File>();

    final boolean write = getopt.getWrite();
    final boolean read = getopt.getRead();
    final boolean shuffle = getopt.getShuffle();
    final boolean filter = getopt.getFilter();
    final boolean upgrade = getopt.getUpgrade();
    final boolean split = getopt.getSplit();
    final int splitSize = getopt.getSplitSize();
    final String output = getopt.getOutput();
    final long minFilesize = getopt.getMinSize();
    final long maxFilesize = getopt.getMaxSize();
    final int minContainerSuffix = getopt.getMinSuffix();
    final int maxContainerSuffix = getopt.getMaxSuffix();
    final int minRetention = getopt.getMinRetention();
    final int maxRetention = getopt.getMaxRetention();
    final int minLegalHolds = getopt.getMinLegalHolds();
    final int maxLegalHolds = getopt.getMaxLegalHolds();
    final String objectFilesDir = getopt.getObjectFilesDir();
    final String prefix = getopt.getPrefix();
    final int shuffleMaxObjectFileSize = getopt.getShuffleMaxObjectFileSize();

    final Set<Integer> containerSuffixes = getopt.getContainerSuffixes();
    try {
      final OutputStream out;
      boolean writeVersionHeader = true;
      int objectVersionSize = 0;

      if (shuffle) {
        checkArgument(shuffleMaxObjectFileSize > 0, "Shuffle output object file maximum size should be greater than 0");
        int shuffleMaxObjects = Math.max(shuffleMaxObjectFileSize / LegacyObjectMetadata.OBJECT_SIZE, 1);
        _consoleLogger.info("Target MaxObjects [{}]", shuffleMaxObjects);
        shuffle(prefix, objectFilesDir, shuffleMaxObjects, shuffleMaxObjectFileSize);
        return;
      }
      if (write)  {
        out = getOutputStream(false, RandomObjectPopulator.MAX_OBJECT_ARG, write, false, output);
      } else if (filter) {
        // get split aware ObjectFileOutputStream
        out = getOutputStream(split, RandomObjectPopulator.MAX_OBJECT_ARG, false, true, output);
      } else if (upgrade) {
        out = getOutputStream(false, RandomObjectPopulator.MAX_OBJECT_ARG, false, false, output);
      } else if (split) {
        out = getOutputStream(split, splitSize, write, false, output);
      } else {
        out = getOutputStream(output);
      }

      for (File f : inputFiles) {
        _consoleLogger.info("processing input file {}", f.getName());
        InputStream in = getInputStream(f);
        if (write) {
          write(in, out);
        } else if (read) {
          read(in, output, writeVersionHeader);
        } else if (filter) {
          filter(in, (ObjectFileOutputStream)out, minFilesize, maxFilesize, minContainerSuffix, maxContainerSuffix, containerSuffixes,
                  minLegalHolds, maxLegalHolds, minRetention, maxRetention, writeVersionHeader);
        } else if (upgrade) {
          upgrade(in, out);
        } else if (split) { // Order matters here - write, filter, upgrade must be above
          split(in, out, splitSize);
        } else { // Default case - just output the same file
          ByteStreams.copy(in, out);
        }
        // already wrote the version header to the outputstream while processing previous input stream
        writeVersionHeader = false;
      }
      if (!out.equals(System.out)) {
        out.close();
      }

    } catch( final IOException e){
          _consoleLogger.error("", e);
          Application.exit(Application.TEST_ERROR);
      }
  Application.exit(0);
  }

  public static InputStream getInputStream(final File input) throws FileNotFoundException {
    InputStream in = System.in;
    if (input != null) {
      in = new FileInputStream(input);
    }
    return new BufferedInputStream(in);
  }

  public static OutputStream getOutputStream(final boolean split, final int splitSize, final boolean write,
      final boolean filter, final String output) throws FileNotFoundException, IOException {
    if (split) {
      int maxObjects;
      if (splitSize > 0)
        maxObjects = splitSize/ RandomObjectPopulator.OBJECT_SIZE;
      else
        maxObjects = RandomObjectPopulator.MAX_OBJECT_ARG;
      return new ObjectFileOutputStream(output, maxObjects, RandomObjectPopulator.SUFFIX, true, true);
    } else if (write) {
      return new ObjectFileOutputStream(output, RandomObjectPopulator.MAX_OBJECT_ARG, RandomObjectPopulator.SUFFIX, false, true);
    } else if (filter) {
      return new ObjectFileOutputStream(output, RandomObjectPopulator.MAX_OBJECT_ARG, RandomObjectPopulator.SUFFIX, true, true);
    }
    else  {
      return new BufferedOutputStream(getOutputStream(output));
    }
  }

  public static OutputStream getOutputStream(final String output) throws FileNotFoundException {
    if (output != null) {
      return new FileOutputStream(output);
    }
    return System.out;
  }

  public static void write(final InputStream in, final OutputStream out)
          throws IOException {
    checkNotNull(in);
    checkNotNull(out);

    final BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charsets.UTF_8));
    String line;
    line = reader.readLine();
    ObjectFileVersion version = null;
    if (line != null) {
      version = ObjectFileVersion.fromCharString(line);
    }

    boolean objectVersionPresent = false;
    ObjectFileHeader header = null;
    if (version != null) {
      int objectVersionSize = 0;
      // read header if available
      if (version.getMajorVersion() >= 3 && version.getMinorVersion() >= 0) {
        line = reader.readLine();
        header = ObjectFileHeader.fromCharString(line);
      }
      if (header != null) {
        objectVersionSize = header.getObjectVersionLen();
        if (objectVersionSize > 0) {
          objectVersionPresent = true;
        }
      }
    }

    // check if the output file needs upgrade. if the output file does not have versioning enabled, but inputfile
    // has object versions upgrade it to receive objects with versions
    _consoleLogger.info("objectfile split objectVersionPresent {}", objectVersionPresent);
    ((ObjectFileOutputStream)out).upgrade(objectVersionPresent);

    // if it is a version 1.0, first line is the object itself. So first write it to the file
    if (version.getMajorVersion() == 1 && version.getMinorVersion() == 0) {
      LegacyObjectMetadata objectName = getObjectFromCharString(version.getMajorVersion(), version.getMinorVersion(),
              line, false);
      out.write(objectName.toBytes(objectVersionPresent));
    }

    while ((line = reader.readLine()) != null) {
      // read object with or without version based on the input file header
      LegacyObjectMetadata objectName = getObjectFromCharString(version.getMajorVersion(), version.getMinorVersion(),
              line, objectVersionPresent);
      // write object with or without version based on the output file header
      out.write(objectName.toBytes(((ObjectFileOutputStream)out).getObjectVersionSize()>0));
    }
  }

  private static LegacyObjectMetadata getObjectFromCharString(int majorVersion, int minorVersion,
                                                              String inputLine, boolean objectVersionPresent) {
    LegacyObjectMetadata objectName = null;
    if (majorVersion == LegacyObjectMetadata.MAJOR_VERSION && minorVersion == LegacyObjectMetadata.MINOR_VERSION) {
      final String[] components = inputLine.split(",");
      if (objectVersionPresent) {
        checkArgument(components.length == 6, "Invalid record - %s", inputLine);
      } else {
        checkArgument(components.length == 5, "Invalid record - %s", inputLine);
      }
      final String objectString = components[0].trim();
      String objectVersion=null;
      long objectSize;
      int containerSuffix;
      byte numLegalHolds;
      int retention;
      if (objectVersionPresent) {
        objectVersion = components[1].trim();
        objectSize = Long.parseLong(components[2].trim());
        containerSuffix = Integer.parseInt(components[3].trim());
        numLegalHolds = Byte.parseByte(components[4].trim());
        retention = Integer.parseInt(components[5].trim());
      } else {
        objectSize = Long.parseLong(components[1].trim());
        containerSuffix = Integer.parseInt(components[2].trim());
        numLegalHolds = Byte.parseByte(components[3].trim());
        retention = Integer.parseInt(components[4].trim());
      }
      if (objectVersionPresent) {
        objectName = LegacyObjectMetadata.fromMetadata(objectString, objectSize, containerSuffix, numLegalHolds, retention, objectVersion);
      } else {
        objectName = LegacyObjectMetadata.fromMetadata(objectString, objectSize, containerSuffix, numLegalHolds, retention, null);
      }
    } else  if (majorVersion == 2 && minorVersion == 0) {
      final String[] components = inputLine.split(",");
      checkArgument(components.length == 5, "Invalid record - %s", inputLine);
      final String objectString = components[0].trim();
      final long objectSize = Long.parseLong(components[1].trim());
      final int containerSuffix = Integer.parseInt(components[2].trim());
      final byte numLegalHolds = Byte.parseByte(components[3].trim());
      final int retention = Integer.parseInt(components[4].trim());
      objectName = LegacyObjectMetadata.fromMetadata(objectString, objectSize, containerSuffix, numLegalHolds, retention, null);
    } else if (majorVersion < LegacyObjectMetadata.MAJOR_VERSION && minorVersion <= LegacyObjectMetadata.MINOR_VERSION){
      final String[] components = inputLine.split(",");
      checkArgument(components.length == 3, "Invalid record - %s", inputLine);
      final String objectString = components[0].trim();
      final long objectSize = Long.parseLong(components[1].trim());
      final int containerSuffix = Integer.parseInt(components[2].trim());
      final byte numLegalHolds = 0;
      final int retention = -1;
      objectName = LegacyObjectMetadata.fromMetadata(objectString, objectSize, containerSuffix, numLegalHolds,
              retention, null);
    }
    return objectName;
  }


  public static void read(final InputStream in, String outputFileName, boolean writeVersionHeader)
          throws IOException, IllegalArgumentException {
    checkNotNull(in);

    ObjectFileVersion currentOutputFileVersion = null;
    ObjectFileHeader currentOutputFileHeader = null;

    if (outputFileName != null) {
      final File outputFile = new File(outputFileName);

      if (outputFile.length() > 0) {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(outputFileName),
                Charsets.UTF_8));
        String line;
        line = reader.readLine();
        if (line != null) {
          currentOutputFileVersion = ObjectFileVersion.fromCharString(line);
        }
        if (currentOutputFileVersion.getMajorVersion() >= 3 && currentOutputFileVersion.getMinorVersion() >= 0) {
          line = reader.readLine();
          currentOutputFileHeader = ObjectFileHeader.fromCharString(line);
        }
        reader.close();

      }
    }

    BufferedWriter writer = null;
    OutputStream out;
    if (outputFileName != null) {
      out = new FileOutputStream(outputFileName, true);
    } else {
      out = System.out;
    }

    try {

      writer = new BufferedWriter(new OutputStreamWriter(out, Charsets.UTF_8));
      ObjectFileVersion version = ObjectFileUtil.readObjectFileVersion(in);
      // skip header
      int headerLength = 0;
      ObjectFileHeader header = null;
      int skipLength = ObjectFileUtil.getVersionHeaderLength(version.getMajorVersion(), version.getMinorVersion());
      if (version.getMajorVersion() >= 3 && version.getMinorVersion() >= 0) {
        header = ObjectFileUtil.readObjectFileHeader(in);
        headerLength = ObjectFileHeader.HEADER_LENGTH;
      }
      in.skip(skipLength);

      //_consoleLogger.info("VERSION:" + version.getMajorVersion() + "." + version.getMinorVersion());

      if (writeVersionHeader || outputFileName == null) {
        // writeVersionHeader if it is requested or if writing to the console that way version header will be written
        // for subsequent input files
        writer.write("VERSION:" + version.getMajorVersion() + "." + version.getMinorVersion());
        writer.newLine();
      }
      if (!writeVersionHeader) {
        // processing second or subsequent input file. If the output is not going to standard out,
        // check the input fileheader. if it is different a different version i.e < 3 or if the input file has
        // object versions and the output does not have versions bail out. otherwise the combine output file
        // will have object records with and without version. It will not be usable.
        // in such as case, if the files have to be combined they have individually convert to text format and then
        // use -w option
        if (out != System.out) {

          if ((currentOutputFileVersion.getMajorVersion() >= 3 &&  version.getMajorVersion() >= 3) &&
                  (currentOutputFileHeader.getObjectVersionLen() != header.getObjectVersionLen())) {
            // same version but one file contains object version and other does not
            out.close();
            in.close();
            throw new IllegalArgumentException("When reading multiple object files and redirecting to plain text outputfile, " +
                    "object files with and without object versions encountered");
          }
          else if (currentOutputFileVersion.getMajorVersion() != version.getMajorVersion()) {
            // outputfile has version > 3 and input file has major version < 3
            _consoleLogger.error("Cannot combine object files with different versions. " +
                            "Inputfile has version {}.{} and ouputfile has version {}.{}",
                    version.getMajorVersion(), version.getMinorVersion(),
                    currentOutputFileVersion.getMajorVersion(), currentOutputFileVersion.getMinorVersion());
            out.close();
            in.close();
            throw new IllegalArgumentException("When reading multiple object files and redirecting to plain text outputfile," +
                    " different versions object file encountered");
          }
        }
      }

      if ((writeVersionHeader && header != null) || (header != null && outputFileName == null)) {
        // writeFileHeader if it is requested or if writing to the console that way file header will be written
        // for subsequent input files
        writer.write(header.getObjectFileHeaderLen() + "," + header.getObjectNameLen() + "," +
                header.getObjectVersionLen() + "," +
                header.getObjectSizeLen() + ","  + header.getObjectSuffixLen() + "," +
                header.getObjectLegalHoldsLen() + "," + header.getObjectRetentionLen());
        writer.newLine();
      }
      in.skip(headerLength);
      final ByteBuffer objectBuffer;
      if (header != null) {
        objectBuffer = ByteBuffer.allocate(LegacyObjectMetadata.getObjectRecordSize(header));
      } else {
        objectBuffer = ByteBuffer.allocate(LegacyObjectMetadata.OBJECT_SIZE);
      }
      final byte[] readBytes = ObjectFileUtil.allocateObjectBuffer(version.getMajorVersion(), version.getMinorVersion(),
              header);
      while (ObjectFileUtil.readFully(in, readBytes)) {
        final int objectVersionSize;
        if (header != null) {
          objectVersionSize = header.getObjectVersionLen();
        } else {
          objectVersionSize = 0;
        }
        final ObjectMetadata objectName = ObjectFileUtil.getObjectFromInputBuffer(version.getMajorVersion(),
                version.getMinorVersion(), readBytes, objectBuffer.array(), objectVersionSize>0);
        if (objectVersionSize > 0) {
          writer.write(String.format("%s,%s,%s,%s,%s,%s", objectName.getName(), objectName.getVersion(), objectName.getSize(),
                  objectName.getContainerSuffix(), objectName.getNumberOfLegalHolds(), objectName.getRetention()));
        } else {
          writer.write(String.format("%s,%s,%s,%s,%s", objectName.getName(), objectName.getSize(),
                  objectName.getContainerSuffix(), objectName.getNumberOfLegalHolds(), objectName.getRetention()));
        }
        writer.newLine();
      }
    } finally {
      if (writer != null) {
        writer.flush();
        writer.close();
      }
    }
  }

  public static void filter(final InputStream in, ObjectFileOutputStream output, final long minFilesize,
      final long maxFilesize, final int minContainerSuffix, final int maxContainerSuffix,
      final Set<Integer> containerSuffixes, final int minLegalholds, final int maxLegalHolds,
      final long minRetention, final long maxRetention, boolean writeVersionHeader) throws IOException {
    checkNotNull(in);
    checkNotNull(output);
    checkArgument(minFilesize >= 0, "minFilesize must be >= 0 [%s]", minFilesize);
    checkArgument(minFilesize <= maxFilesize, "minFilesize must be <= maxFilesize [%s, %s]",
        minFilesize, maxFilesize);
    checkArgument(minContainerSuffix >= -1, "minContainerSuffix must be >= -1 [%s]",
        minContainerSuffix);
    checkArgument(minContainerSuffix <= maxContainerSuffix,
        "minContainerSuffix must be <= maxContainerSuffix [%s, %s]", minContainerSuffix,
        maxContainerSuffix);
    checkArgument(minContainerSuffix >= -1, "minContainerSuffix must be >= -1 [%s]",
            minContainerSuffix);
    checkArgument(minContainerSuffix >= -1, "minContainerSuffix must be >= -1 [%s]",
            minContainerSuffix);
    checkArgument(minLegalholds >= 0, "minLegalHolds must be >= 0 [%s]",
            minLegalholds);
    checkArgument(minLegalholds <= maxLegalHolds, "minLegalHolds must be <= maxLegalHolds [%s, %s]",
            minLegalholds, maxLegalHolds);

    ObjectFileVersion version = ObjectFileUtil.readObjectFileVersion(in);
    ObjectFileHeader header = null;
    boolean objectVersionPresent = false;
    int objectVersionSize = 0;
    int skipLength = 0;
    if (version.getMajorVersion() >= 3 && version.getMinorVersion() >= 0) {
        header = ObjectFileUtil.readObjectFileHeader(in);
    }
    if (version.getMajorVersion() == LegacyObjectMetadata.MAJOR_VERSION &&
            version.getMinorVersion() == LegacyObjectMetadata.MINOR_VERSION) {
      skipLength = ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH;
    } else if (version.getMajorVersion() == 2 &&
            version.getMinorVersion() == 0) {
      skipLength = ObjectFileVersion.VERSION_HEADER_LENGTH;
    } else {
      skipLength = 0;
    }
    if (header != null) {
      objectVersionSize = header.getObjectVersionLen();
      if (objectVersionSize > 0) {
        objectVersionPresent = true;
      }
    }
    final byte[] readBytes = ObjectFileUtil.allocateObjectBuffer(version.getMajorVersion(), version.getMinorVersion(),
            header);
    final ByteBuffer objectBuffer = ByteBuffer.wrap(readBytes);

    in.skip(skipLength);

    // convert the file if the object versions are present in the input file
    output.upgrade(objectVersionPresent);

    while (ObjectFileUtil.readFully(in, readBytes)) {
      final ObjectMetadata objectName = ObjectFileUtil.getObjectFromInputBuffer(version.getMajorVersion(),
              version.getMinorVersion(), readBytes, objectBuffer.array(), objectVersionPresent);
      //object.setBytes(objectName.toBytes(objectVersionPresent));
      if (objectName.getSize() < minFilesize || objectName.getSize() > maxFilesize) {
        continue;
      }
      if (objectName.getContainerSuffix() < minContainerSuffix || objectName.getContainerSuffix() > maxContainerSuffix) {
        continue;
      }
      if (!containerSuffixes.isEmpty() && !containerSuffixes.contains(objectName.getContainerSuffix())) {
        continue;
      }
      if (objectName.getRetention() < minRetention || objectName.getRetention() > maxRetention) {
        continue;
      }
      if (objectName.getNumberOfLegalHolds() < minLegalholds || objectName.getNumberOfLegalHolds() > maxLegalHolds) {
        continue;
      }
      output.write(objectName.toBytes(objectVersionPresent));
    }
  }

  public static void split(final InputStream in, final OutputStream out, int splitSize)
      throws IOException {
    checkNotNull(in);
    checkNotNull(out);

    ObjectFileVersion version = ObjectFileUtil.readObjectFileVersion(in);
    ObjectFileHeader header = null;
    boolean objectVersionPresent = false;
    int objectVersionSize = 0;
    if (version.getMajorVersion() == LegacyObjectMetadata.MAJOR_VERSION && version.getMinorVersion() == 0) {
      header = ObjectFileUtil.readObjectFileHeader(in);
    }
    final byte[] readBytes = ObjectFileUtil.allocateObjectBuffer(version.getMajorVersion(), version.getMinorVersion(),
            header);
    // skip header
    if (header != null) {
      objectVersionSize = header.getObjectVersionLen();
      if (objectVersionSize > 0) {
        objectVersionPresent = true;
        int maxObjects = Math.max((splitSize - ObjectFileVersion.VERSION_HEADER_LENGTH - ObjectFileHeader.HEADER_LENGTH) /
                (LegacyObjectMetadata.OBJECT_SIZE + LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE), 1);
        ((ObjectFileOutputStream)out).setMaxObjects(maxObjects);
      }
    }
    final ByteBuffer objectBuffer = ByteBuffer.wrap(readBytes);

    int skipLength = 0;
    if (version.getMajorVersion() == LegacyObjectMetadata.MAJOR_VERSION &&
            version.getMinorVersion() == LegacyObjectMetadata.MINOR_VERSION) {
      skipLength = ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH;
    } else if (version.getMajorVersion() == 2 &&
            version.getMinorVersion() == 0) {
      skipLength = ObjectFileVersion.VERSION_HEADER_LENGTH;
    } else {
      skipLength = 0;
    }
    in.skip(skipLength);

    // check if the output file needs upgrade. if the output file does not have versioning enabled, but inputfile
    // has object versions upgrade it to receive objects with versions
    _consoleLogger.info("objectfile split objectVersionPresent {}", objectVersionPresent);
    ((ObjectFileOutputStream)out).upgrade(objectVersionPresent);

    while (ObjectFileUtil.readFully(in, readBytes)) {
      final ObjectMetadata objectName = ObjectFileUtil.getObjectFromInputBuffer(version.getMajorVersion(),
              version.getMinorVersion(), readBytes, objectBuffer.array(), objectVersionPresent);
      out.write(objectName.toBytes( ((ObjectFileOutputStream)out).getObjectVersionSize()>0));
    }
  }

  public static class MutableObjectMetadata extends LegacyObjectMetadata {
    public MutableObjectMetadata(int bufSize, int objectVersionSize) {
      super(ByteBuffer.allocate(bufSize), objectVersionSize);
    }

    public void setBytes(final byte[] bytes) {
      this.objectBuffer.position(0);
      this.objectBuffer.put(bytes);
    }

    public void setSize(final long size) {
      this.objectBuffer.position(OBJECT_NAME_SIZE);
      this.objectBuffer.putLong(size);
    }

    public void setContainerSuffix(final int suffix) {
      this.objectBuffer.position(OBJECT_NAME_SIZE + OBJECT_SIZE_SIZE);
      this.objectBuffer.putInt(suffix);
    }
  }

  // TODO: fix this to version 3.0
  public static void upgrade(final InputStream in, final OutputStream out) throws IOException {
    // oom size
    final int legacySize = 18;
    final byte[] buf = new byte[legacySize];
    //TODO: fix this
    final MutableObjectMetadata object = new MutableObjectMetadata(51, 16);
    object.setSize(0);
    object.setContainerSuffix(-1);
    while (ObjectFileUtil.readFully(in, buf)) {
      object.setBytes(buf);
      out.write(object.toBytes(false));
    }
  }






  private static int getNumberOfObjects(File objectFile) {
    try {
      InputStream in = getInputStream(objectFile);
      ObjectFileVersion version = ObjectFileUtil.readObjectFileVersion(in);
      ObjectFileHeader objectFileHeader = null;
      if (version.getMajorVersion() >= 3 && version.getMinorVersion() >= 0) {
        objectFileHeader = ObjectFileUtil.readObjectFileHeader(in);
      }
      final byte[] objectBytes = ObjectFileUtil.allocateObjectBuffer(version.getMajorVersion(), version.getMinorVersion(),
              objectFileHeader);
      int versionHeaderLength = ObjectFileUtil.getVersionHeaderLength(version.getMajorVersion(), version.getMinorVersion());
      int fileHeaderLength = 0;
      if (objectFileHeader !=  null) {
        fileHeaderLength = objectFileHeader.getObjectFileHeaderLen();
      }
      long fileSize = objectFile.length();
      int nObjects = (int) (fileSize - (versionHeaderLength + fileHeaderLength)) / objectBytes.length;
      return nObjects;

    } catch (FileNotFoundException fne) {
      _consoleLogger.error("File {} not found", objectFile.getPath());
    } catch (IOException ioe) {
      _consoleLogger.error("IOException: {}", objectFile.getPath());
    }

    return 0;
  }

  private static class ObjectFileObjectsCountComparator<K extends File, V extends Integer> implements Comparator<K> {

    private Map<? extends File,? extends Integer> map;

    public ObjectFileObjectsCountComparator(Map<K, V> map) {
      this.map = new HashMap(map);
    }

    @Override
    public int compare(K k1, K k2) {
      return this.map.get(k1) - this.map.get(k2);
    }
  }

  private static class ObjectFileNameIndexComparator<K extends File> implements Comparator<K> {

    private final String prefix;

    public ObjectFileNameIndexComparator(final String prefix) {
      this.prefix = prefix;
    }
    @Override
    public int compare(K k1, K k2) {
      String fn1 = k1.getName();
      String[] parts = fn1.split("\\.");
      String name = parts[0];
      int index1 = Integer.parseInt(name.substring(prefix.length()));

      String fn2 = k2.getName();
      parts = fn2.split("\\.");
      name = parts[0];
      int index2 = Integer.parseInt(name.substring(prefix.length()));;
      return index1 - index2;
    }
  }

  public static void compactObjectFiles(final HashMap<File, Integer> objectFiles, final int maxcount) {
    // sort the files based on the no. of objects present. combine files that have less than 50% of
    // maxcount objects
    List<File> fileList = new ArrayList();
    for (File f: objectFiles.keySet()) {
      _consoleLogger.info("Before sorting File {} No. of objects {}", f.getPath(), objectFiles.get(f));
      fileList.add(f);
    }
    ObjectFileObjectsCountComparator<File, Integer> comparator = new ObjectFileObjectsCountComparator(objectFiles);


    Collections.sort(fileList, comparator);

    for (File f: fileList) {
      _consoleLogger.info("After sorting File {} No. of objects {}", f.getPath(), objectFiles.get(f));
    }

    int s = 0;
    File previousFile = null;
    for (File f: fileList) {
      if (s + objectFiles.get(f) <= maxcount && previousFile != null) {
          combineFiles(previousFile, f);
          s += objectFiles.get(f);
      }
      if (s >= maxcount) {
        // reset the file to be merged with to the next file
        previousFile = null;
        s = 0;
        continue;

      }
      if (previousFile == null) {
        s = objectFiles.get(f);
        previousFile = f;
      }
    }
  }

  public static void combineFiles(File f1, File f2) {

    //TODO: handle object file versions
    _consoleLogger.info("combine file {} and file {}", f1.getPath(), f2.getPath());
    try {
      RandomAccessFile fp1 = new RandomAccessFile(f1, "rw");
      long length = fp1.length();
      fp1.seek(length);

      InputStream in = getInputStream(f2);
      ObjectFileVersion version = ObjectFileUtil.readObjectFileVersion(in);
      ObjectFileHeader objectFileHeader = null;
      if (version.getMajorVersion() >= 3 && version.getMinorVersion() >= 0) {
        objectFileHeader = ObjectFileUtil.readObjectFileHeader(in);
      }
      final byte[] objectBytes = ObjectFileUtil.allocateObjectBuffer(version.getMajorVersion(), version.getMinorVersion(),
              objectFileHeader);

      int versionHeaderLength = ObjectFileUtil.getVersionHeaderLength(version.getMajorVersion(), version.getMinorVersion());
      int fileHeaderLength = 0;
      if (objectFileHeader !=  null) {
        fileHeaderLength = objectFileHeader.getObjectFileHeaderLen();
      }
      int skipLength = versionHeaderLength + fileHeaderLength;

      in.close();
      RandomAccessFile fp2 = new RandomAccessFile(f2, "rw");
      fp2.seek(skipLength);


      byte[] buffer = new byte[128*1024];
      int sz = 0;
      while ((sz = fp2.read(buffer)) > 0) {
        fp1.write(buffer, 0, sz);
      }

      fp2.close();
      fp1.close();
      // delete file 2
      f2.delete();
    } catch(IOException ioe) {
      _consoleLogger.warn("IOException: {}", ioe.getMessage());
    }
}


  public static void randomShuffleObjects(final InputStream in, final OutputStream out) throws IOException {
    checkNotNull(in);
    checkNotNull(out);

    ArrayList<ObjectMetadata> slist = new ArrayList<ObjectMetadata>();
    long timestampStart = System.currentTimeMillis();
    _consoleLogger.info("random shuffle of output object file start time [{}]", FORMATTER.print(timestampStart));
    boolean objectVersionPresent = false;
    try {
      ObjectFileVersion version = ObjectFileUtil.readObjectFileVersion(in);
      ObjectFileHeader objectFileHeader = null;
      if (version.getMajorVersion() >= 3 && version.getMinorVersion() >= 0) {
        objectFileHeader = ObjectFileUtil.readObjectFileHeader(in);
      }
      final byte[] objectBytes = ObjectFileUtil.allocateObjectBuffer(version.getMajorVersion(), version.getMinorVersion(),
              objectFileHeader);

      int versionHeaderLength = ObjectFileUtil.getVersionHeaderLength(version.getMajorVersion(), version.getMinorVersion());
      int fileHeaderLength = 0;

      if (objectFileHeader !=  null) {
        fileHeaderLength = objectFileHeader.getObjectFileHeaderLen();
        objectVersionPresent = objectFileHeader.getObjectVersionLen() > 0 ? true: false;
      }
      int skipLength = versionHeaderLength + fileHeaderLength;



      in.skip(skipLength);
      final ByteBuffer objectBuffer = ByteBuffer.wrap(objectBytes);
      while (ObjectFileUtil.readFully(in, objectBytes)) {
        //final ObjectMetadata objectName = LegacyObjectMetadata.fromBytes(buf);
        final ObjectMetadata objectName = ObjectFileUtil.getObjectFromInputBuffer(version.getMajorVersion(),
                version.getMinorVersion(), objectBytes, objectBuffer.array(), objectVersionPresent);
        slist.add(objectName);
      }
      // shuffle
      Random r = new Random();
      ObjectFileUtil.writeObjectFileVersion(out);
      int objectVersionSize = objectVersionPresent == true ? 16: 0;
      ObjectFileUtil.writeObjectFileHeader(out, objectVersionSize);
      int nObects = slist.size();
      while (nObects > 0) {
        int i = r.nextInt(nObects);
        ObjectMetadata e = slist.remove(i);
        out.write(e.toBytes(objectVersionPresent));
        //dlist.add(e);
        nObects = slist.size();
      }

    } catch (Exception e) {
      _consoleLogger.warn("Unexpected Error {}", e.getMessage());
      return;
    }
    long timestampFinish = System.currentTimeMillis();
    _consoleLogger.info("random shuffle of output object file end time [{}]", FORMATTER.print(timestampFinish));
    _consoleLogger.info("random shuffle of output object file total time time [{}] seconds", (timestampFinish - timestampStart)/1000);

  }

  private static int getFileIndex(String prefix, String fileName) {
    String[] parts = fileName.split("\\.");
    String name = parts[0];
    int index = Integer.parseInt(name.substring(prefix.length()));
    return index;
  }


  private static void reIndexFiles(String prefix, File[] files) {
    ObjectFileNameIndexComparator comparator = new ObjectFileNameIndexComparator(prefix);
    int currentIndex = 0;
    Collections.sort(Arrays.asList(files), comparator);
    for (File f: files) {
      int index = getFileIndex(prefix, f.getName());
      if (index != currentIndex) {
        String directory = f.getParent();
        File destFile = new File(directory, String.format("%s%d.object", prefix, currentIndex));
        _logger.debug("reindex file {} to {}", f.getName(), destFile.getName());
        f.renameTo(destFile);
      }
      ++currentIndex;

    }
  }


  public static void shuffle(String prefix, String directory, int maxObjects, int shuffleMaxObjectFileSize) {
    _consoleLogger.info("object-file shuffling ...");
    long tsStartShuffleProcess = System.currentTimeMillis();
    File[] files = ObjectManagerUtils.getIdFiles(prefix, RandomObjectPopulator.SUFFIX, directory);
    if (files.length < 2) {
      _consoleLogger.info("No shuffling needed. Only {} objects file found", files.length);
      return;
    }

    Integer nTotalObjects = 0;
    int objectVersionSize = 0;
    HashMap<File, Integer> fileObjectsMap = new LinkedHashMap<File, Integer>();
    // adjust maxObjects based on if an object file contains object version
    try {
      //FileInputStream fis = new FileInputStream(files[0].getPath());
      InputStream is = getInputStream(files[0]);
      ObjectFileVersion version = ObjectFileUtil.readObjectFileVersion(is);
      if (version.getMajorVersion() >= 3 && version.getMinorVersion() >= 0) {
        ObjectFileHeader header = ObjectFileUtil.readObjectFileHeader(is);
        objectVersionSize = header.getObjectVersionLen();
        if (objectVersionSize > 0) {
          if (shuffleMaxObjectFileSize > 0) {
            maxObjects =
                    Math.max((shuffleMaxObjectFileSize /
                            (LegacyObjectMetadata.OBJECT_SIZE + LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE)), 1);
          }
        }
      }
    } catch (FileNotFoundException fne) {
      _consoleLogger.error("shuffle: {} not found while checking file header", files[0].getPath());
    } catch (IOException ioe) {
      _consoleLogger.error("shuffle: {} IOException while checking file header", files[0].getPath());
    }
    for (File f: files) {
      int nObjects = getNumberOfObjects(f);
      fileObjectsMap.put(f, nObjects);
      nTotalObjects += nObjects;
    }
    _consoleLogger.info("Total number of object available in all object files [{}]", nTotalObjects);

    if (maxObjects > nTotalObjects) {
      _consoleLogger.warn(
              "maxObjects [{}] requested greater than available [{}] objects. setting maxObjects to available objects",
              maxObjects, nTotalObjects);
      maxObjects = nTotalObjects;
    }
    _consoleLogger.info("starting compaction...");
    long tsStartCompaction = System.currentTimeMillis();
    compactObjectFiles(fileObjectsMap, maxObjects);
    _consoleLogger.info("compaction done.");
    long tsFinishCompaction = System.currentTimeMillis();
    _consoleLogger.info("Compaction total time time [{}] seconds", (tsFinishCompaction - tsStartCompaction)/1000);

    // reindex files to create a set of consequent index
    // for example indexes 0,1,3,5,6 => 0,1,2,3,4
    _consoleLogger.info("reindex files ...");
    files = ObjectManagerUtils.getIdFiles(prefix, RandomObjectPopulator.SUFFIX, directory);
    reIndexFiles(prefix, files);
    _consoleLogger.info("reindex files done.");

    _consoleLogger.info("start shuffling ...");
    long tsStartShuffle = System.currentTimeMillis();
    FileOutputStream fos;
    // re-get the files list because compaction might have removed some files
    files = ObjectManagerUtils.getIdFiles(prefix, RandomObjectPopulator.SUFFIX, directory);

    ObjectFileNameIndexComparator comparator = new ObjectFileNameIndexComparator(prefix);
    Collections.sort(Arrays.asList(files), comparator);
    String fileWithMaxIndex = files[files.length-1].getName();
    int maxIndex = getFileIndex(prefix, fileWithMaxIndex);
    _consoleLogger.info("current max_index is {} and next index to use {}", maxIndex, maxIndex+1);
    maxIndex++;
    nTotalObjects = 0;
    // re-initialize object files - object count map
    fileObjectsMap = new LinkedHashMap<File, Integer>();
    for (File f: files) {
      //_consoleLogger.info(f.getPath());
      int nObjects = getNumberOfObjects(f);
      //_consoleLogger.info("Number of objects in file {} is {}", f.getPath(), nObjects);
      fileObjectsMap.put(f, nObjects);
      nTotalObjects += nObjects;
    }

    try {
      fos = new FileOutputStream(new File(directory , prefix + maxIndex + ".object"));
      _consoleLogger.info("Shuffle output file [{}]", directory + "/" + prefix + maxIndex + ".object");
      try {
        ObjectFileUtil.writeObjectFileVersion(fos);
        ObjectFileUtil.writeObjectFileHeader(fos, objectVersionSize);
      } catch(IOException ioe) {
          _consoleLogger.error("IOException: Failed to created new Object file");
          return;
      }
    } catch(FileNotFoundException fne) {
      _consoleLogger.error("FileNotFoundException: Failed to created new Object file");
      return;
    }
    long borrowedObjects = 0;
    int newObjectVersionSize = 0;
    for (File f: files) {
      long nObjects = fileObjectsMap.get(f);
      long toBorrow = (long)Math.floor((double)(nObjects * maxObjects) / nTotalObjects);
      toBorrow = Math.min(toBorrow, nObjects);
      _consoleLogger.info("file [{}] has total [{}] objects to borrow [{}]", f.getPath(), nObjects, toBorrow);
      // borrow no. of objects from the end and truncate the file
      try {
        BufferedInputStream fis = new BufferedInputStream(new FileInputStream(f));
        // TODO: handle version based object size
        ObjectFileVersion version = ObjectFileUtil.readObjectFileVersion(fis);
        ObjectFileHeader objectFileHeader = null;
        if (version.getMajorVersion() >= 3 && version.getMinorVersion() >= 0) {
          objectFileHeader = ObjectFileUtil.readObjectFileHeader(fis);
          newObjectVersionSize = objectFileHeader.getObjectVersionLen();
        }
        final byte[] objectBytes = ObjectFileUtil.allocateObjectBuffer(version.getMajorVersion(), version.getMinorVersion(),
                objectFileHeader);
        int objectSize = objectBytes.length;
        byte[] outputBytes = new byte[objectSize];
        ByteBuffer objectBuffer = ByteBuffer.wrap(outputBytes);
        try {

          long skip = f.length() - toBorrow * objectSize;
          _consoleLogger.info("Input file [{}] size [{}] skipping [{}] bytes from beginning", f.getPath(), f.length(),
                  skip);
          int countSkipped = 0;
          while (countSkipped < skip) {
            countSkipped += fis.skip(skip-countSkipped);
          }
          if (newObjectVersionSize > objectVersionSize) {
            // new file from which objects are borrowed has object versions but the shuffle output file
            // does not
            objectVersionSize = newObjectVersionSize;
            fos.close();
            ObjectFileUtil.upgrade(new File(directory , prefix + maxIndex + ".object"), true);
            fos = new FileOutputStream(new File(directory , prefix + maxIndex + ".object"), true);
            ByteStreams.copy(fis, fos);

          } else if (newObjectVersionSize < objectVersionSize) {
            // new file from which objects are borrowed does not have object versions but the shuffle output file
            //does. read object by object and add with zero version id
            while (ObjectFileUtil.readFully(fis, objectBytes)) {
              final ObjectMetadata objectName = ObjectFileUtil.getObjectFromInputBuffer(version.getMajorVersion(),
                      version.getMinorVersion(), objectBytes, objectBuffer.array(), false);
              //object.setBytes(objectName.toBytes(objectVersionPresent));
              fos.write(objectName.toBytes(true));
            }

          } else {
            ByteStreams.copy(fis, fos);
          }
          borrowedObjects += toBorrow;

          fis.close();
          // We borrowed from the end of the file so nothing is lost from truncating
          RandomAccessFile truncater = null;
          try {
            _consoleLogger.info("truncating surplus file [{}] to [{}] ", f.getPath(), skip);
            truncater = new RandomAccessFile(f, "rwd");
            truncater.setLength(skip);
          } finally {
            if (truncater != null) {
              truncater.close();
            }
          }
        } catch(IOException ioe) {
          _consoleLogger.warn("IOException skipping on File {} not found. skipping file", f.getPath());
        }
      } catch(IOException ioe) {
        _consoleLogger.warn("IOException skipping on File {} not found. skipping file", f.getPath());
      }
    }
    _consoleLogger.info("Shuffle output file [{}] objects [{}]", directory + "/" + prefix + maxIndex + ".object",
            borrowedObjects);
    _consoleLogger.info("collected objects from different object files.");


    try {
      fos.close();
    } catch (IOException ioe) {
      _consoleLogger.error("Failed to close new object file stream");
    }

    _consoleLogger.info("start randomizing objects in the output object file ...");
    File tempFile = new File(directory, "temp.object");
    File srcFile = new File(directory , prefix + maxIndex + ".object");
    try {
      InputStream is = getInputStream(srcFile);
      OutputStream os = getOutputStream(directory + "/" + "temp.object");
      randomShuffleObjects(is, os);
      os.close();
      is.close();
      Files.move(tempFile, srcFile);
    } catch(FileNotFoundException fne) {
      _consoleLogger.error("File {} not found", tempFile.getPath());
      //TODO return error code
      return;
    } catch(IOException ioe) {
      //TODO return error code
      _consoleLogger.error("File {} not found", tempFile.getPath());
      return;
    }
    _consoleLogger.info("randomizing objects in the output object file done.");
    long tsEndShuffle = System.currentTimeMillis();
    long tsEndShuffleProcess = tsEndShuffle;
    _consoleLogger.info("Shuffle/Randomize total time time [{}] seconds", (tsEndShuffle - tsStartShuffle)/1000);
    _consoleLogger.info("Whole shuffle process total time time [{}] seconds", (tsEndShuffleProcess - tsStartShuffleProcess)/1000);
  }


  public static class ObjectFileOutputStream extends OutputStream {
    private final String prefix;
    private int index;
    private int written;
    private int maxObjects;
    private final String suffix;
    private OutputStream out;
    private int objectVersionSize = 0;
    private final boolean autoCreate;
    public ObjectFileOutputStream(final String prefix, final int maxObjects, final String suffix, boolean autoCreate, boolean writeVersion)
        throws FileNotFoundException, IOException {
      this.prefix = checkNotNull(prefix);
      this.index = 0;
      this.written = 0;
      checkArgument(maxObjects > 0, "maxObjects must be > 0 [%s]", maxObjects);
      this.maxObjects = maxObjects;
      this.suffix = checkNotNull(suffix);
      this.autoCreate = autoCreate;
      this.out = create(writeVersion);
    }

    private OutputStream create(boolean createHeaders) throws IOException {
      if (createHeaders) {
        BufferedOutputStream bos = new BufferedOutputStream(
                new FileOutputStream(String.format("%s%d%s", this.prefix, this.index, this.suffix)));
        ObjectFileUtil.writeObjectFileVersion(bos);
        ObjectFileUtil.writeObjectFileHeader(bos, this.objectVersionSize);
        return bos;
      } else {
        //skip to the end of file
        BufferedOutputStream bos = new BufferedOutputStream(
                new FileOutputStream(String.format("%s%d%s", this.prefix, this.index, this.suffix),true));
        return bos;
      }

    }

    public String outputFile() {
      return String.format("%s%d%s", this.prefix, this.index, this.suffix);
    }

    public void setMaxObjects(int maxObjects) {
      this.maxObjects = maxObjects;
    }

    public void setObjectVersionSize(int objectVersionSize) throws IOException {
      this.objectVersionSize =  objectVersionSize;
    }

    public void writeFileHeader() throws IOException {
      ObjectFileUtil.writeObjectFileHeader(this.out, this.objectVersionSize);
    }

    public int getObjectVersionSize() {
      return this.objectVersionSize;
    }

    public void upgrade(boolean objectVersionPresent) throws IOException {
      // close the stream, upgrade and reopen the stream

      if (objectVersionPresent && this.objectVersionSize <= 0) {
        this.out.close();
        File file = new File(String.format("%s%d%s", this.prefix, this.index, this.suffix));
        ObjectFileUtil.upgrade(file, objectVersionPresent);
        this.out = create(false);
        this.objectVersionSize = LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE;
      }
    }


    @Override
    public void write(final byte[] b) throws IOException {
      if (this.autoCreate) {
        if (this.written >= this.maxObjects) {
          this.index++;
          this.out.close();
          this.out = create(true);

          this.written = 0;
        }
      }

      this.out.write(b);
      this.written++;
    }

    public void write(final byte[] b, int objectVersionSize) {

    }

    @Override
    public void write(final int b) throws IOException {
      throw new IOException("ObjectFileOutputStream.write(b) should not be called");
    }

    @Override
    public void close() throws IOException {
      this.out.close();
    }
  }
}
