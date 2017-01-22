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
import java.util.Set;

import com.ibm.og.object.LegacyObjectMetadata;
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

    final File input = getopt.getInput();

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

    final Set<Integer> containerSuffixes = getopt.getContainerSuffixes();

    try {
      final InputStream in = getInputStream(input);
      final OutputStream out;

      if (write) {
        out = getOutputStream(split, splitSize, output);
        write(in, out);
      } else if (read) {
        out = getOutputStream(output);
        read(in, out);
      } else if (filter) {
        out = getOutputStream(split, splitSize, output);
        filter(in, out, minFilesize, maxFilesize, minContainerSuffix, maxContainerSuffix, containerSuffixes);
      } else if (upgrade) {
        out = getOutputStream(split, splitSize, output);
        upgrade(in, out);
      } else if (split) { // Order matters here - write, filter, upgrade must be above
        out = getOutputStream(split, splitSize, output);
        split(in, out);
      } else { // Default case - just output the same file
        out = getOutputStream(output);
        ByteStreams.copy(in, out);
      }

      if (!out.equals(System.out)) {
        out.close();
      }
    } catch (final IOException e) {
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
      final String output) throws FileNotFoundException {
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

  public static void write(final InputStream in, final OutputStream out) throws IOException {
    checkNotNull(in);
    checkNotNull(out);

    final BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charsets.UTF_8));
    String line;
    while ((line = reader.readLine()) != null) {
      final String[] components = line.split(",");
      checkArgument(components.length == 3, "Invalid record - %s", line);
      final String objectString = components[0].trim();
      final long objectSize = Long.parseLong(components[1].trim());
      final int containerSuffix = Integer.parseInt(components[2].trim());
      final ObjectMetadata objectName =
          LegacyObjectMetadata.fromMetadata(objectString, objectSize, containerSuffix);
      out.write(objectName.toBytes());
    }
  }

  public static void read(final InputStream in, final OutputStream out) throws IOException {
    checkNotNull(in);
    checkNotNull(out);

    BufferedWriter writer = null;
    try {
      writer = new BufferedWriter(new OutputStreamWriter(out, Charsets.UTF_8));
      final byte[] buf = new byte[LegacyObjectMetadata.OBJECT_SIZE];
      while (readFully(in, buf)) {
        final ObjectMetadata objectName = LegacyObjectMetadata.fromBytes(buf);
        writer.write(String.format("%s,%s,%s", objectName.getName(), objectName.getSize(),
            objectName.getContainerSuffix()));
        writer.newLine();
      }
    } finally {
      if (writer != null) {
        writer.flush();
      }
    }
  }

  public static void filter(final InputStream in, final OutputStream out, final long minFilesize,
      final long maxFilesize, final int minContainerSuffix, final int maxContainerSuffix, final Set<Integer> containerSuffixes)
          throws IOException {
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

    final byte[] buf = new byte[LegacyObjectMetadata.OBJECT_SIZE];
    final MutableObjectMetadata object = new MutableObjectMetadata();
    while (readFully(in, buf)) {
      object.setBytes(buf);
      if ((object.getSize() >= minFilesize && object.getSize() <= maxFilesize)
          && (object.getContainerSuffix() >= minContainerSuffix
              && object.getContainerSuffix() <= maxContainerSuffix)) {
        if(!containerSuffixes.isEmpty() && containerSuffixes.contains(object.getContainerSuffix())) {
          out.write(object.toBytes());
        } else if (containerSuffixes.isEmpty()) {
          out.write(object.toBytes());
        }
      }
    }
  }

  public static void split(final InputStream in, final OutputStream out)
      throws IOException {
    checkNotNull(in);
    checkNotNull(out);

    final byte[] buf = new byte[LegacyObjectMetadata.OBJECT_SIZE];
    final MutableObjectMetadata object = new MutableObjectMetadata();
    while (readFully(in, buf)) {
      object.setBytes(buf);
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
        throws FileNotFoundException {
      this.prefix = checkNotNull(prefix);
      this.index = 0;
      this.written = 0;
      checkArgument(maxObjects > 0, "maxObjects must be > 0 [%s]", maxObjects);
      this.maxObjects = maxObjects;
      this.suffix = checkNotNull(suffix);
      this.out = create();
    }

    private OutputStream create() throws FileNotFoundException {
      return new BufferedOutputStream(
          new FileOutputStream(String.format("%s%d%s", this.prefix, this.index, this.suffix)));
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
