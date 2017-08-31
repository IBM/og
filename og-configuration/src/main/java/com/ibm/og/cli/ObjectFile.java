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
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

import com.ibm.og.object.LegacyObjectMetadata;
import com.ibm.og.object.ObjectFileUtil;
import com.ibm.og.object.ObjectFileVersion;
import com.ibm.og.object.RandomObjectPopulator;
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

    final boolean write = getopt.getWrite();
    final boolean read = getopt.getRead();
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

    final Set<Integer> containerSuffixes = getopt.getContainerSuffixes();
    try {
      final OutputStream out;
      boolean writeVersionHeader = true;
      if (read) {
        out = getOutputStream(output);
      } else if (write || filter || upgrade || split) {
        out = getOutputStream(split, splitSize, output);
        if (split) {
          // for split, version header is written when ObjectFileOutputStream is created
          writeVersionHeader = false;
        }
      } else {
        out = getOutputStream(output);
      }
      for (File f : inputFiles) {
        InputStream in = getInputStream(f);
        if (write) {
          write(in, out,writeVersionHeader);
        } else if (read) {
          read(in, out, writeVersionHeader);
        } else if (filter) {
          filter(in, out, minFilesize, maxFilesize, minContainerSuffix, maxContainerSuffix, containerSuffixes,
                  minLegalHolds, maxLegalHolds, minRetention, maxRetention, writeVersionHeader);
        } else if (upgrade) {
          upgrade(in, out);
        } else if (split) { // Order matters here - write, filter, upgrade must be above
          split(in, out);
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

  public static OutputStream getOutputStream(final boolean split, final int splitSize,
      final String output) throws FileNotFoundException, IOException {
    if (split) {
      int maxObjects;
      if (splitSize > 0)
        maxObjects = splitSize/ RandomObjectPopulator.OBJECT_SIZE;
      else
        maxObjects = RandomObjectPopulator.MAX_OBJECT_ARG;
      return new ObjectFileOutputStream(output, maxObjects, RandomObjectPopulator.SUFFIX);
    } else {
      return new BufferedOutputStream(getOutputStream(output));
    }
  }

  public static OutputStream getOutputStream(final String output) throws FileNotFoundException {
    if (output != null) {
      return new FileOutputStream(output);
    }
    return System.out;
  }

  public static void write(final InputStream in, final OutputStream out, boolean writeVersionHeader) throws IOException {
    checkNotNull(in);
    checkNotNull(out);

    final BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charsets.UTF_8));
    String line;
    if (writeVersionHeader) {
      ObjectFileUtil.writeObjectFileVersion(out);
    }

    line = reader.readLine();
    ObjectFileVersion version = null;
    if (line != null) {
      version = ObjectFileVersion.fromCharString(line);
      if (version.getMajorVersion() == 1 && version.getMinorVersion() == 0) {
        LegacyObjectMetadata objectName = getObjectFromCharString(version.getMajorVersion(), version.getMinorVersion(),
                line);
        out.write(objectName.toBytes());
      }
    }

    ObjectMetadata objectName = null;
    while ((line = reader.readLine()) != null) {
      objectName = getObjectFromCharString(version.getMajorVersion(), version.getMinorVersion(), line);
      out.write(objectName.toBytes());
    }
  }

  private static LegacyObjectMetadata getObjectFromCharString(int majorVersion, int minorVersion, String inputLine) {
    LegacyObjectMetadata objectName = null;
    if (majorVersion == LegacyObjectMetadata.MAJOR_VERSION && minorVersion == LegacyObjectMetadata.MINOR_VERSION) {
      final String[] components = inputLine.split(",");
      checkArgument(components.length == 5, "Invalid record - %s", inputLine);
      final String objectString = components[0].trim();
      final long objectSize = Long.parseLong(components[1].trim());
      final int containerSuffix = Integer.parseInt(components[2].trim());
      final byte numLegalHolds = Byte.parseByte(components[3].trim());
      final int retention = Integer.parseInt(components[4].trim());
      objectName = LegacyObjectMetadata.fromMetadata(objectString, objectSize, containerSuffix, numLegalHolds, retention);
    } else if (majorVersion < LegacyObjectMetadata.MAJOR_VERSION && minorVersion <= LegacyObjectMetadata.MINOR_VERSION){
      final String[] components = inputLine.split(",");
      checkArgument(components.length == 3, "Invalid record - %s", inputLine);
      final String objectString = components[0].trim();
      final long objectSize = Long.parseLong(components[1].trim());
      final int containerSuffix = Integer.parseInt(components[2].trim());
      final byte numLegalHolds = 0;
      final int retention = -1;
      objectName = LegacyObjectMetadata.fromMetadata(objectString, objectSize, containerSuffix, numLegalHolds, retention);
    }
    return objectName;
  }


  public static void read(final InputStream in, final OutputStream out, boolean writeVersionHeader) throws IOException {
    checkNotNull(in);
    checkNotNull(out);

    BufferedWriter writer = null;
    try {
      writer = new BufferedWriter(new OutputStreamWriter(out, Charsets.UTF_8));
      final ByteBuffer objectBuffer = ByteBuffer.allocate(LegacyObjectMetadata.OBJECT_SIZE);
      ObjectFileVersion version = ObjectFileUtil.readObjectFileVersion(in);
      final byte[] readBytes = ObjectFileUtil.allocateObjectBuffer(version.getMajorVersion(), version.getMinorVersion(),
              in);
      // skip header
      int skipLength = ObjectFileUtil.getVersionHeaderLength(version.getMajorVersion(), version.getMinorVersion());
      in.skip(skipLength);
      if (writeVersionHeader) {
        writer.write("VERSION:" + LegacyObjectMetadata.MAJOR_VERSION + "." + LegacyObjectMetadata.MINOR_VERSION);
        writer.newLine();
      }
      while (readFully(in, readBytes)) {
        //final ObjectMetadata objectName = LegacyObjectMetadata.fromBytes(buf);
        final ObjectMetadata objectName = ObjectFileUtil.getObjectFromInputBuffer(version.getMajorVersion(),
                version.getMinorVersion(), readBytes, objectBuffer.array());
        writer.write(String.format("%s,%s,%s,%s,%s", objectName.getName(), objectName.getSize(),
            objectName.getContainerSuffix(), objectName.getNumberOfLegalHolds(), objectName.getRetention()));
        writer.newLine();
      }
    } finally {
      if (writer != null) {
        writer.flush();
      }
    }
  }

  public static void filter(final InputStream in, final OutputStream out, final long minFilesize,
      final long maxFilesize, final int minContainerSuffix, final int maxContainerSuffix,
      final Set<Integer> containerSuffixes, final int minLegalholds, final int maxLegalHolds,
      final long minRetention, final long maxRetention, boolean writeVersionHeader) throws IOException {
    checkNotNull(in);
    checkNotNull(out);
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

    final MutableObjectMetadata object = new MutableObjectMetadata();
    final ByteBuffer objectBuffer = ByteBuffer.allocate(LegacyObjectMetadata.OBJECT_SIZE);
    ObjectFileVersion version = ObjectFileUtil.readObjectFileVersion(in);
    final byte[] readBytes = ObjectFileUtil.allocateObjectBuffer(version.getMajorVersion(), version.getMinorVersion(), in);
    // skip header
    int skipLength = 0;
    if (version.getMajorVersion() == LegacyObjectMetadata.MAJOR_VERSION &&
            version.getMinorVersion() == LegacyObjectMetadata.MINOR_VERSION) {
      skipLength = ObjectFileVersion.VERSION_HEADER_LENGTH;
    } else {
      skipLength = 0;
    }
    in.skip(skipLength);
    if (writeVersionHeader) {
      ObjectFileUtil.writeObjectFileVersion(out);
    }
    while (readFully(in, readBytes)) {
      final ObjectMetadata objectName = ObjectFileUtil.getObjectFromInputBuffer(version.getMajorVersion(),
              version.getMinorVersion(), readBytes, objectBuffer.array());
      object.setBytes(objectName.toBytes());
      if (object.getSize() < minFilesize || object.getSize() > maxFilesize) {
        continue;
      }
      if (object.getContainerSuffix() < minContainerSuffix || object.getContainerSuffix() > maxContainerSuffix) {
        continue;
      }
      if (!containerSuffixes.isEmpty() && !containerSuffixes.contains(object.getContainerSuffix())) {
        continue;
      }
      if (object.getRetention() < minRetention || object.getRetention() > maxRetention) {
        continue;
      }
      if (object.getNumberOfLegalHolds() < minLegalholds || object.getNumberOfLegalHolds() > maxLegalHolds) {
        continue;
      }
      out.write(object.toBytes());
    }
  }

  public static void split(final InputStream in, final OutputStream out)
      throws IOException {
    checkNotNull(in);
    checkNotNull(out);

    final MutableObjectMetadata object = new MutableObjectMetadata();
    final ByteBuffer objectBuffer = ByteBuffer.allocate(LegacyObjectMetadata.OBJECT_SIZE);
    ObjectFileVersion version = ObjectFileUtil.readObjectFileVersion(in);
    final byte[] readBytes = ObjectFileUtil.allocateObjectBuffer(version.getMajorVersion(), version.getMinorVersion(),
            in);
    int skipLength = ObjectFileUtil.getVersionHeaderLength(version.getMajorVersion(), version.getMinorVersion());
    in.skip(skipLength);
    while (readFully(in, readBytes)) {
      final ObjectMetadata objectName = ObjectFileUtil.getObjectFromInputBuffer(version.getMajorVersion(),
              version.getMinorVersion(), readBytes, objectBuffer.array());
      object.setBytes(objectName.toBytes());
      out.write(object.toBytes());
    }
  }

  public static class MutableObjectMetadata extends LegacyObjectMetadata {
    public MutableObjectMetadata() {
      super(ByteBuffer.allocate(OBJECT_SIZE));
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

  public static void upgrade(final InputStream in, final OutputStream out) throws IOException {
    // oom size
    final int legacySize = 18;
    final byte[] buf = new byte[legacySize];
    final MutableObjectMetadata object = new MutableObjectMetadata();
    object.setSize(0);
    object.setContainerSuffix(-1);
    while (readFully(in, buf)) {
      object.setBytes(buf);
      out.write(object.toBytes());
    }
  }

  // adapt Bytestreams.readFully to return a boolean rather than throwing an exception
  public static boolean readFully(final InputStream in, final byte[] b) throws IOException {
    try {
      ByteStreams.readFully(in, b);
    } catch (final EOFException e) {
      // FIXME deal with the case where bytes not divisible by b.size, rather than regular EOF
      return false;
    }
    return true;
  }

  public static class ObjectFileOutputStream extends OutputStream {
    private final String prefix;
    private int index;
    private int written;
    private final int maxObjects;
    private final String suffix;
    private OutputStream out;

    public ObjectFileOutputStream(final String prefix, final int maxObjects, final String suffix)
        throws FileNotFoundException, IOException {
      this.prefix = checkNotNull(prefix);
      this.index = 0;
      this.written = 0;
      checkArgument(maxObjects > 0, "maxObjects must be > 0 [%s]", maxObjects);
      this.maxObjects = maxObjects;
      this.suffix = checkNotNull(suffix);
      this.out = create();
    }

    private OutputStream create() throws FileNotFoundException, IOException {
      BufferedOutputStream bos = new BufferedOutputStream(
          new FileOutputStream(String.format("%s%d%s", this.prefix, this.index, this.suffix)));
      ObjectFileUtil.writeObjectFileVersion(bos);
      return bos;
    }

    @Override
    public void write(final byte[] b) throws IOException {
      if (this.written >= this.maxObjects) {
        this.index++;
        this.out.close();
        this.out = create();
        this.written = 0;
      }

      this.out.write(b);
      this.written++;
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
