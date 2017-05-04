/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import com.ibm.og.object.LegacyObjectMetadata;
import com.ibm.og.object.ObjectMetadata;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class ObjectFileTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private File nonExistent;
  private File exists;

  @Before
  public void before() throws IOException {
    this.nonExistent = new File("/nonexistent");
    this.exists = this.folder.newFile();
  }

  @Test
  public void getInputStreamNullInput() throws FileNotFoundException {
    final InputStream in = ObjectFile.getInputStream(null);
    assertThat(in, is(notNullValue()));
  }

  @Test(expected = FileNotFoundException.class)
  public void getInputStreamMissingInput() throws FileNotFoundException {
    ObjectFile.getInputStream(this.nonExistent);
  }

  @Test
  public void getInputStream() throws FileNotFoundException {
    final InputStream in = ObjectFile.getInputStream(this.exists);
    assertThat(in, is(not(System.in)));
  }

  @Test
  public void getOutputStreamNullOutput() throws FileNotFoundException {
    final OutputStream out = ObjectFile.getOutputStream(null);
    assertThat(out, is((OutputStream) System.out));
  }

  @Test
  public void getOutputStream() throws FileNotFoundException {
    final OutputStream out = ObjectFile.getOutputStream(this.exists.toString());
    assertThat(out, is(not((OutputStream) System.out)));
  }

  @DataProvider
  public static Object[][] provideInvalidStreams() {
    return new Object[][] {{null, new ByteArrayOutputStream()},
        {new ByteArrayInputStream(new byte[] {}), null}};
  }

  @Test
  @UseDataProvider("provideInvalidStreams")
  public void invalidWrite(final InputStream in, final OutputStream out) throws IOException {
    this.thrown.expect(NullPointerException.class);
    ObjectFile.write(in, out);
  }

  @Test
  public void write() throws IOException {
    final String objectMetadata = UUID.randomUUID().toString().replace("-", "") + "0000,0,0,99,3600";
    final InputStream in = new ByteArrayInputStream(objectMetadata.getBytes());
    final ByteArrayOutputStream out = new ByteArrayOutputStream(LegacyObjectMetadata.OBJECT_SIZE);
    ObjectFile.write(in, out);
    final ObjectMetadata object = LegacyObjectMetadata.fromBytes(out.toByteArray());
    assertThat(objectMetadata, is(String.format("%s,%s,%s,%s,%s", object.getName(), object.getSize(),
        object.getContainerSuffix(), object.getNumberOfLegalHolds(), object.getRetention())));
  }

  @Test
  @UseDataProvider("provideInvalidStreams")
  public void invalidRead(final InputStream in, final OutputStream out) throws IOException {
    this.thrown.expect(NullPointerException.class);
    ObjectFile.read(in, out);
  }

  @Test
  public void read() throws IOException {
    final String objectString = UUID.randomUUID().toString().replace("-", "") + "0000";
    final LegacyObjectMetadata object = LegacyObjectMetadata.fromMetadata(objectString, 1024, 0, (byte)-1, -1);
    final InputStream in = new ByteArrayInputStream(object.toBytes());
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectFile.read(in, out);
    assertThat(new String(out.toByteArray()), is(String.format("%s,%s,%s,%s,%s%n", object.getName(),
        object.getSize(), object.getContainerSuffix(), object.getNumberOfLegalHolds(), object.getRetention())));
  }

  @DataProvider
  public static Object[][] provideInvalidFilter() {
    final InputStream in = new ByteArrayInputStream(new byte[] {});
    final OutputStream out = new ByteArrayOutputStream();
    return new Object[][] {{null, out, 0, 0, 0, 0, new HashSet<Integer>(), false, -1L, -1L, NullPointerException.class},
        {in, null, 0, 0, 0, 0, new HashSet<Integer>(), false, -1L, -1L, NullPointerException.class},
        {in, out, -1, 0, 0, 0, new HashSet<Integer>(), false, -1L, 60L, IllegalArgumentException.class},
        {in, out, 0, -1, 0, 0, new HashSet<Integer>(), false, -1L, 1610612735L, IllegalArgumentException.class},
        {in, out, 10, 9, 0, 0, new HashSet<Integer>(), false, -1L, 3798720306000L, IllegalArgumentException.class},
        {in, out, 0, 0, -2, 2, new HashSet<Integer>(), false, -1L, 3798720306000L, IllegalArgumentException.class},
        {in, out, 0, 0, 4, 3, new HashSet<Integer>(), false, -1L, -1L, IllegalArgumentException.class}};
  }

  @Test
  @UseDataProvider("provideInvalidFilter")
  public void invalidFilter(final InputStream in, final OutputStream out, final long minFilesize,
      final long maxFilesize, final int minContainerSuffix, final int maxContainerSuffix, final Set<Integer> containerSuffixes,
      final boolean legalholds, final long minRetention, final long maxRetention,  final Class<Exception> expectedException) throws IOException {
    this.thrown.expect(expectedException);
    ObjectFile.filter(in, out, minFilesize, maxFilesize, minContainerSuffix, maxContainerSuffix, containerSuffixes,
            legalholds, minRetention, maxRetention);
  }

  @Test
  public void filter() throws IOException {
    final String s = UUID.randomUUID().toString().replace("-", "") + "0000";
    final ObjectMetadata o1 = LegacyObjectMetadata.fromMetadata(s, 1, 1, (byte)-1, -1);
    final ObjectMetadata o2 = LegacyObjectMetadata.fromMetadata(s, 2, 2, (byte)-1, -1);
    final ObjectMetadata o3 = LegacyObjectMetadata.fromMetadata(s, 3, 3, (byte)-1, -1);
    final ByteArrayOutputStream source = new ByteArrayOutputStream();
    source.write(o1.toBytes());
    source.write(o2.toBytes());
    source.write(o3.toBytes());

    final ByteArrayInputStream in = new ByteArrayInputStream(source.toByteArray());
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final Set<Integer> pointSuffixList = new HashSet<Integer>();
    ObjectFile.filter(in, out, 1, 2, 2, 3, new HashSet<Integer>(), false, -1L, -1L);

    assertThat(out.size(), is(LegacyObjectMetadata.OBJECT_SIZE));
    final ObjectMetadata filtered = LegacyObjectMetadata.fromBytes(out.toByteArray());
    assertThat(filtered.getName(), is(s));
    assertThat(filtered.getSize(), is(2L));
  }

  @Test
  public void filter_retention() throws IOException {
    final String s = UUID.randomUUID().toString().replace("-", "") + "0000";
    long retention = 70L * 365L* 24L* 3600L;
    final ObjectMetadata o1 = LegacyObjectMetadata.fromMetadata(s, 1, 1, (byte)-1, -1L);
    final ObjectMetadata o2 = LegacyObjectMetadata.fromMetadata(s, 2, 2, (byte)-1, 3600L);
    final ObjectMetadata o3 = LegacyObjectMetadata.fromMetadata(s, 3, 3, (byte)-1, retention-100);
    final ByteArrayOutputStream source = new ByteArrayOutputStream();
    source.write(o1.toBytes());
    source.write(o2.toBytes());
    source.write(o3.toBytes());

    final ByteArrayInputStream in = new ByteArrayInputStream(source.toByteArray());
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final Set<Integer> pointSuffixList = new HashSet<Integer>();
    ObjectFile.filter(in, out, 1, 3, 1, 3, new HashSet<Integer>(), false, 3601L, retention);

    assertThat(out.size(), is(LegacyObjectMetadata.OBJECT_SIZE));
    final ObjectMetadata filtered = LegacyObjectMetadata.fromBytes(out.toByteArray());
    assertThat(filtered.getName(), is(s));
    assertThat(filtered.getSize(), is(3L));
  }

  @Test
  public void filter_legalhold() throws IOException {
    final String s = UUID.randomUUID().toString().replace("-", "") + "0000";
    long retention = 70L * 365L* 24L* 3600L;
    final ObjectMetadata o1 = LegacyObjectMetadata.fromMetadata(s, 1, 1, (byte)99, -1L);
    final ObjectMetadata o2 = LegacyObjectMetadata.fromMetadata(s, 2, 2, (byte)-1, 3600L);
    final ObjectMetadata o3 = LegacyObjectMetadata.fromMetadata(s, 3, 3, (byte)-1, retention-100);
    final ByteArrayOutputStream source = new ByteArrayOutputStream();
    source.write(o1.toBytes());
    source.write(o2.toBytes());
    source.write(o3.toBytes());

    final ByteArrayInputStream in = new ByteArrayInputStream(source.toByteArray());
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final Set<Integer> pointSuffixList = new HashSet<Integer>();
    ObjectFile.filter(in, out, 1, 3, 1, 3, new HashSet<Integer>(), true, -1L, -1L);

    assertThat(out.size(), is(LegacyObjectMetadata.OBJECT_SIZE));
    final ObjectMetadata filtered = LegacyObjectMetadata.fromBytes(out.toByteArray());
    assertThat(filtered.getName(), is(s));
    assertThat(filtered.getSize(), is(1L));
  }


  @DataProvider
  public static Object[][] provideInvalidObjectFileOutputStream() {
    final String prefix = "id";
    final String suffix = ".object";
    return new Object[][] {{null, 1, suffix, NullPointerException.class},
        {prefix, -1, suffix, IllegalArgumentException.class},
        {prefix, 0, suffix, IllegalArgumentException.class},
        {prefix, 1, null, NullPointerException.class}};
  }

  @Test
  @SuppressWarnings("resource")
  @UseDataProvider("provideInvalidObjectFileOutputStream")
  public void invalidObjectFileOutputStream(final String prefix, final int maxObjects,
      final String suffix, final Class<Exception> expectedException) throws FileNotFoundException {
    this.thrown.expect(expectedException);
    new ObjectFile.ObjectFileOutputStream(prefix, maxObjects, suffix);
  }

  @DataProvider
  public static Object[][] provideObjectFileOutputStream() {
    return new Object[][] {{10, 5, 1}, {10, 10, 1}, {10, 15, 2}, {10, 25, 3}};
  }

  @Test
  @UseDataProvider("provideObjectFileOutputStream")
  public void objectFileOutputStream(final int maxObjects, final int numObjects,
      final int fileCount) throws IOException {
    final String prefix = "id";
    final String suffix = ".object";
    final String prefixFilename = new File(this.folder.getRoot().toString(), prefix).toString();
    final OutputStream out = new ObjectFile.ObjectFileOutputStream(prefixFilename, maxObjects, suffix);
    final ObjectMetadata o = LegacyObjectMetadata
        .fromMetadata(UUID.randomUUID().toString().replace("-", "") + "0000", 0, 0, (byte)-1, -1);

    for (int i = 0; i < numObjects; i++) {
      out.write(o.toBytes());
    }
    out.close();

    final List<String> objectFiles = Arrays.asList(this.folder.getRoot().list(new FilenameFilter() {
      @Override
      public boolean accept(final File dir, final String name) {
        return name.endsWith(suffix);
      }
    }));

    assertThat(objectFiles.size(), is(fileCount));
    int persistedObjects = 0;
    for (int i = 0; i < fileCount; i++) {
      final String filename = String.format("%s%d%s", prefix, i, suffix);
      assertThat(objectFiles.contains(filename), is(true));
      persistedObjects +=
          new File(this.folder.getRoot(), filename).length() / LegacyObjectMetadata.OBJECT_SIZE;
    }

    assertThat(persistedObjects, is(numObjects));
  }
}
