/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.cli;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.cli.Application.Cli;
import com.cleversafe.og.object.LegacyObjectMetadata;
import com.cleversafe.og.object.ObjectMetadata;
import com.cleversafe.og.object.RandomObjectPopulator;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;

public class ObjectFile {
  private static final Logger _consoleLogger = LoggerFactory.getLogger("ConsoleLogger");

  public static void main(final String[] args) {
    final Cli cli = Application.cli("object-file", "objectfile.jsap", args);
    if (cli.shouldStop()) {
      if (cli.error()) {
        cli.printErrors();
        cli.printUsage();
      } else if (cli.help()) {
        cli.printUsage();
      } else if (cli.version()) {
        cli.printVersion();
      }

      Application.exit(Application.TEST_ERROR);
    }

    final File input = cli.flags().getFile("input");
    final boolean write = cli.flags().getBoolean("write");
    final boolean read = cli.flags().getBoolean("read");
    final boolean filter = cli.flags().getBoolean("filter");
    final boolean split = cli.flags().getBoolean("split");
    final String output = cli.flags().getString("output");
    final long minFilesize = cli.flags().getLong("min-filesize");
    final long maxFilesize = cli.flags().getLong("max-filesize");
    final int minContainerSuffix = cli.flags().getInt("min-suffix");
    final int maxContainerSuffix = cli.flags().getInt("max-suffix");

    try {
      final InputStream in = getInputStream(input);
      final OutputStream out;

      if (write) {
        out = getOutputStream(split, output);
        write(in, out);
      } else if (read) {
        out = getOutputStream(output);
        read(in, out);
      } else if (filter) {
        out = getOutputStream(split, output);
        filter(in, out, minFilesize, maxFilesize, minContainerSuffix, maxContainerSuffix);
      } else {
        out = getOutputStream(output);
        ByteStreams.copy(in, out);
      }

      if (!out.equals(System.out)) {
        out.close();
      }
    } catch (final IOException e) {
      _consoleLogger.error("", e);
    }
  }

  public static InputStream getInputStream(final File input) throws FileNotFoundException {
    if (input != null) {
      return new FileInputStream(input);
    }
    return System.in;
  }

  public static OutputStream getOutputStream(final boolean split, final String output)
      throws FileNotFoundException {
    if (split) {
      return new ObjectFileOutputStream(output, RandomObjectPopulator.MAX_OBJECT_ARG,
          RandomObjectPopulator.SUFFIX);
    }
    return getOutputStream(output);
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
      final long objectSize = Long.valueOf(components[1].trim());
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
      while (in.read(buf) == LegacyObjectMetadata.OBJECT_SIZE) {
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
      final long maxFilesize, final int minContainerSuffix, final int maxContainerSuffix)
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
    while (in.read(buf) == LegacyObjectMetadata.OBJECT_SIZE) {
      final ObjectMetadata object = LegacyObjectMetadata.fromBytes(buf);
      if ((object.getSize() >= minFilesize && object.getSize() <= maxFilesize)
          && (object.getContainerSuffix() >= minContainerSuffix && object.getContainerSuffix() <= maxContainerSuffix)) {
        out.write(object.toBytes());
      }
    }
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
      return new FileOutputStream(String.format("%s%d%s", this.prefix, this.index, this.suffix));
    }

    @Override
    public void write(final byte[] b) throws IOException {
      if (this.written >= this.maxObjects) {
        this.index++;
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
