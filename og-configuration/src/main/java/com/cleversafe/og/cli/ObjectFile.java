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
      } else if (cli.help())
        cli.printUsage();
      else if (cli.version())
        cli.printVersion();

      Application.exit(Application.TEST_ERROR);
    }

    File input = cli.flags().getFile("input");
    boolean write = cli.flags().getBoolean("write");
    boolean read = cli.flags().getBoolean("read");
    boolean filter = cli.flags().getBoolean("filter");
    boolean split = cli.flags().getBoolean("split");
    String output = cli.flags().getString("output");
    long minFilesize = cli.flags().getLong("min-filesize");
    long maxFilesize = cli.flags().getLong("max-filesize");

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
        filter(in, out, minFilesize, maxFilesize);
      } else {
        out = getOutputStream(output);
        ByteStreams.copy(in, out);
      }

      if (!out.equals(System.out))
        out.close();
    } catch (final IOException e) {
      _consoleLogger.error("", e);
    }
  }

  public static InputStream getInputStream(final File input) throws FileNotFoundException {
    if (input != null)
      return new FileInputStream(input);
    return System.in;
  }

  public static OutputStream getOutputStream(boolean split, String output)
      throws FileNotFoundException {
    if (split)
      return new ObjectFileOutputStream(output, RandomObjectPopulator.MAX_OBJECT_ARG,
          RandomObjectPopulator.SUFFIX);
    return getOutputStream(output);
  }

  public static OutputStream getOutputStream(String output) throws FileNotFoundException {
    if (output != null)
      return new FileOutputStream(output);
    return System.out;
  }

  public static void write(final InputStream in, final OutputStream out) throws IOException {
    checkNotNull(in);
    checkNotNull(out);

    final BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charsets.UTF_8));
    String line;
    while ((line = reader.readLine()) != null) {
      String[] components = line.split(",", 2);
      String objectString = components[0].trim();
      long objectSize = Long.valueOf(components[1].trim());
      ObjectMetadata objectName = LegacyObjectMetadata.fromMetadata(objectString, objectSize);
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
        ObjectMetadata objectName = LegacyObjectMetadata.fromBytes(buf);
        writer.write(String.format("%s,%s", objectName.getName(), objectName.getSize()));
        writer.newLine();
      }
    } finally {
      if (writer != null)
        writer.flush();
    }
  }

  public static void filter(InputStream in, OutputStream out, long minFilesize, long maxFilesize)
      throws IOException {
    checkNotNull(in);
    checkNotNull(out);
    checkArgument(minFilesize >= 0, "minFilesize must be >= 0 [%s]", minFilesize);
    checkArgument(minFilesize <= maxFilesize, "minFilesize must be <= maxFilesize [%s, %s]",
        minFilesize, maxFilesize);

    final byte[] buf = new byte[LegacyObjectMetadata.OBJECT_SIZE];
    while (in.read(buf) == LegacyObjectMetadata.OBJECT_SIZE) {
      ObjectMetadata objectName = LegacyObjectMetadata.fromBytes(buf);
      if (objectName.getSize() >= minFilesize && objectName.getSize() <= maxFilesize)
        out.write(objectName.toBytes());
    }
  }

  public static class ObjectFileOutputStream extends OutputStream {
    private final String prefix;
    private int index;
    private int written;
    private final int maxObjects;
    private final String suffix;
    private OutputStream out;

    public ObjectFileOutputStream(String prefix, int maxObjects, String suffix)
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
    public void write(byte[] b) throws IOException {
      if (this.written >= this.maxObjects) {
        this.index++;
        this.out = create();
        this.written = 0;
      }

      this.out.write(b);
      this.written++;
    }

    @Override
    public void write(int b) throws IOException {
      throw new IOException("ObjectFileOutputStream.write(b) should not be called");
    }

    @Override
    public void close() throws IOException {
      this.out.close();
    }
  }
}
