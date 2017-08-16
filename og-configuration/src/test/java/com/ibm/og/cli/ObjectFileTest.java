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
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import com.google.common.io.BaseEncoding;
import com.ibm.og.object.LegacyObjectMetadata;
import com.ibm.og.object.ObjectFileUtil;
import com.ibm.og.object.ObjectFileVersion;
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
    ObjectFile.write(in, out, true);
  }

  @Test
  public void write() throws IOException {
    final String objectMetadata = UUID.randomUUID().toString().replace("-", "") + "0000,0,0,99,3600";
    String versionHeaderString = "VERSION:2.0\n";
    final ByteBuffer inputByteBuffer = ByteBuffer.allocate(versionHeaderString.getBytes().length +
           objectMetadata.getBytes().length);
    inputByteBuffer.put(versionHeaderString.getBytes());
    inputByteBuffer.put(objectMetadata.getBytes());
    final InputStream in = new ByteArrayInputStream(inputByteBuffer.array());
    final ByteArrayOutputStream out = new ByteArrayOutputStream(ObjectFileVersion.VERSION_HEADER_LENGTH +
            LegacyObjectMetadata.OBJECT_SIZE);
    ObjectFile.write(in, out, true);

    ByteBuffer outputBuffer = ByteBuffer.wrap(out.toByteArray());
    outputBuffer.position(ObjectFileVersion.VERSION_HEADER_LENGTH);
    byte[] filteredBytes = new byte[LegacyObjectMetadata.OBJECT_SIZE];
    outputBuffer.get(filteredBytes, 0, LegacyObjectMetadata.OBJECT_SIZE);

    final ObjectMetadata object = LegacyObjectMetadata.fromBytes(filteredBytes);
    assertThat(objectMetadata, is(String.format("%s,%s,%s,%s,%s", object.getName(), object.getSize(),
        object.getContainerSuffix(), object.getNumberOfLegalHolds(), object.getRetention())));
  }

  @Test
  public void writeV1() throws IOException {
    final String objectName = UUID.randomUUID().toString().replace("-", "") + "0000";
    final String objectMetadata = objectName + ",0,0";
    //String versionHeaderString = "VERSION:2.0\n";
    final ByteBuffer inputByteBuffer = ByteBuffer.allocate(objectMetadata.getBytes().length);
    inputByteBuffer.put(objectMetadata.getBytes());
    final InputStream in = new ByteArrayInputStream(inputByteBuffer.array());
    final ByteArrayOutputStream out = new ByteArrayOutputStream(ObjectFileVersion.VERSION_HEADER_LENGTH +
            LegacyObjectMetadata.OBJECT_SIZE);
    ObjectFile.write(in, out, true);

    ByteBuffer outputBuffer = ByteBuffer.wrap(out.toByteArray());
    outputBuffer.position(ObjectFileVersion.VERSION_HEADER_LENGTH);
    byte[] filteredBytes = new byte[LegacyObjectMetadata.OBJECT_SIZE];
    outputBuffer.get(filteredBytes, 0, LegacyObjectMetadata.OBJECT_SIZE);

    final ObjectMetadata object = LegacyObjectMetadata.fromBytes(filteredBytes);
    final LegacyObjectMetadata expectedObject = LegacyObjectMetadata.fromMetadata(objectName, 0, 0, (byte)0, -1);
    assertThat(String.format("%s,%s,%s,%s,%s", expectedObject.getName(), expectedObject.getSize(),
            expectedObject.getContainerSuffix(), expectedObject.getNumberOfLegalHolds(), expectedObject.getRetention()),
            is(String.format("%s,%s,%s,%s,%s", object.getName(), object.getSize(),
            object.getContainerSuffix(), object.getNumberOfLegalHolds(), object.getRetention())));
  }

  @Test
  @UseDataProvider("provideInvalidStreams")
  public void invalidRead(final InputStream in, final OutputStream out) throws IOException {
    this.thrown.expect(NullPointerException.class);
    ObjectFile.read(in, out, true);
  }

  @Test
  public void read() throws IOException {
    final String objectString = UUID.randomUUID().toString().replace("-", "") + "0000";
    final LegacyObjectMetadata object = LegacyObjectMetadata.fromMetadata(objectString, 1024, 0, (byte)0, -1);
    ByteBuffer inputBuffer = ByteBuffer.allocate(ObjectFileVersion.VERSION_HEADER_LENGTH + LegacyObjectMetadata.OBJECT_SIZE);
    inputBuffer.put(ObjectFileVersion.fromMetadata((byte)2, (byte)0).getBytes());
    inputBuffer.put(object.toBytes());
    final InputStream in = new ByteArrayInputStream(inputBuffer.array());

    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectFile.read(in, out, true);
    ByteBuffer outBuffer = ByteBuffer.wrap(out.toByteArray());
    outBuffer.position(ObjectFileVersion.VERSION_HEADER_LENGTH);
    byte[] objectBuffer = new byte[LegacyObjectMetadata.OBJECT_SIZE];
    outBuffer.get(objectBuffer, 0, LegacyObjectMetadata.OBJECT_SIZE);

    assertThat(new String(out.toByteArray()), is(String.format("%s:%s.%s\n%s,%s,%s,%s,%s%n", "VERSION",
          LegacyObjectMetadata.MAJOR_VERSION, LegacyObjectMetadata.MINOR_VERSION, object.getName(),
        object.getSize(), object.getContainerSuffix(), object.getNumberOfLegalHolds(), object.getRetention())));
  }


  @Test
  public void readV1() throws IOException {
    final BaseEncoding ENCODING = BaseEncoding.base16().lowerCase();
    final String objectName = UUID.randomUUID().toString().replace("-", "") + "0000";
    ByteBuffer inputBuffer = ByteBuffer.allocate(ObjectFileUtil.OBJECT_SIZE_V1_0);
    inputBuffer.put(ENCODING.decode(objectName), 0, LegacyObjectMetadata.OBJECT_NAME_SIZE);
    inputBuffer.putLong(0);
    inputBuffer.putInt(0);

    final InputStream in = new ByteArrayInputStream(inputBuffer.array());

    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectFile.read(in, out, true);
    ByteBuffer outBuffer = ByteBuffer.wrap(out.toByteArray());
    outBuffer.position(ObjectFileVersion.VERSION_HEADER_LENGTH);
    byte[] objectBuffer = new byte[LegacyObjectMetadata.OBJECT_SIZE];
    outBuffer.get(objectBuffer, 0, LegacyObjectMetadata.OBJECT_SIZE);

    final LegacyObjectMetadata expectedObject = LegacyObjectMetadata.fromMetadata(objectName, 0, 0, (byte)0, -1);

    assertThat(new String(out.toByteArray()), is(String.format("%s:%s.%s\n%s,%s,%s,%s,%s%n", "VERSION",
            LegacyObjectMetadata.MAJOR_VERSION, LegacyObjectMetadata.MINOR_VERSION, expectedObject.getName(),
            expectedObject.getSize(), expectedObject.getContainerSuffix(), expectedObject.getNumberOfLegalHolds(),
            expectedObject.getRetention())));
  }

  @DataProvider
  public static Object[][] provideInvalidFilter() {
    final InputStream in = new ByteArrayInputStream(new byte[] {});
    final OutputStream out = new ByteArrayOutputStream();
    return new Object[][] {{null, out, 0, 0, 0, 0, new HashSet<Integer>(), 0, 100, -1L, -1L, NullPointerException.class},
        {in, null, 0, 0, 0, 0, new HashSet<Integer>(), 0, 100,-1L, -1L, NullPointerException.class},
        {in, out, -1, 0, 0, 0, new HashSet<Integer>(), 0, 100, -1L, 60L, IllegalArgumentException.class},
        {in, out, 0, -1, 0, 0, new HashSet<Integer>(), 0, 100, -1L, 1610612735L, IllegalArgumentException.class},
        {in, out, 10, 9, 0, 0, new HashSet<Integer>(), 0, 100, -1L, 3798720306000L, IllegalArgumentException.class},
        {in, out, 0, 0, -2, 2, new HashSet<Integer>(), 0, 100, -1L, 3798720306000L, IllegalArgumentException.class},
        {in, out, 0, 0, 4, 3, new HashSet<Integer>(), 0, 100, -1L, -1L, IllegalArgumentException.class}};
  }

  @Test
  @UseDataProvider("provideInvalidFilter")
  public void invalidFilter(final InputStream in, final OutputStream out, final long minFilesize,
      final long maxFilesize, final int minContainerSuffix, final int maxContainerSuffix, final Set<Integer> containerSuffixes,
      final int minLegalHolds, final int maxLegalHolds, final long minRetention, final long maxRetention,  final Class<Exception> expectedException) throws IOException {
    this.thrown.expect(expectedException);
    ObjectFile.filter(in, out, minFilesize, maxFilesize, minContainerSuffix, maxContainerSuffix, containerSuffixes,
            minLegalHolds, maxLegalHolds, minRetention, maxRetention, true);
  }

  @Test
  public void filter() throws IOException {
    final String s = UUID.randomUUID().toString().replace("-", "") + "0000";
    final ObjectMetadata o1 = LegacyObjectMetadata.fromMetadata(s, 1, 1, (byte)0, -1);
    final ObjectMetadata o2 = LegacyObjectMetadata.fromMetadata(s, 2, 2, (byte)0, -1);
    final ObjectMetadata o3 = LegacyObjectMetadata.fromMetadata(s, 3, 3, (byte)0, -1);
    final ByteArrayOutputStream source = new ByteArrayOutputStream();
    ObjectFileUtil.writeObjectFileVersion(source);
    source.write(o1.toBytes());
    source.write(o2.toBytes());
    source.write(o3.toBytes());

    final ByteArrayInputStream in = new ByteArrayInputStream(source.toByteArray());
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final Set<Integer> pointSuffixList = new HashSet<Integer>();
    ObjectFile.filter(in, out, 1, 2, 2, 3, new HashSet<Integer>(), 0, 100, -1L, -1L, true);
    assertThat(out.size(), is(ObjectFileVersion.VERSION_HEADER_LENGTH + LegacyObjectMetadata.OBJECT_SIZE));

    ByteBuffer outputBuffer = ByteBuffer.wrap(out.toByteArray());
    outputBuffer.position(ObjectFileVersion.VERSION_HEADER_LENGTH);
    byte[] filteredBytes = new byte[LegacyObjectMetadata.OBJECT_SIZE];
    outputBuffer.get(filteredBytes, 0, LegacyObjectMetadata.OBJECT_SIZE);
    final ObjectMetadata filtered = LegacyObjectMetadata.fromBytes(filteredBytes);
    assertThat(filtered.getName(), is(s));
    assertThat(filtered.getSize(), is(2L));
  }

  @Test
  public void filter_retention() throws IOException {
    final String s = UUID.randomUUID().toString().replace("-", "") + "0000";
    int retention = 30 * 365* 24* 3600;
    final ObjectMetadata o1 = LegacyObjectMetadata.fromMetadata(s, 1, 1, (byte)0, -1);
    final ObjectMetadata o2 = LegacyObjectMetadata.fromMetadata(s, 2, 2, (byte)0, 3600);
    final ObjectMetadata o3 = LegacyObjectMetadata.fromMetadata(s, 3, 3, (byte)0, retention-100);
    final ByteArrayOutputStream source = new ByteArrayOutputStream();
    ObjectFileUtil.writeObjectFileVersion(source);
    source.write(o1.toBytes());
    source.write(o2.toBytes());
    source.write(o3.toBytes());

    final ByteArrayInputStream in = new ByteArrayInputStream(source.toByteArray());
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final Set<Integer> pointSuffixList = new HashSet<Integer>();
    ObjectFile.filter(in, out, 1, 3, 1, 3, new HashSet<Integer>(), 0, 100, 3601L, retention, true);

    assertThat(out.size(), is(ObjectFileVersion.VERSION_HEADER_LENGTH + LegacyObjectMetadata.OBJECT_SIZE));
    ByteBuffer outputBuffer = ByteBuffer.wrap(out.toByteArray());
    outputBuffer.position(ObjectFileVersion.VERSION_HEADER_LENGTH);
    byte[] filteredBytes = new byte[LegacyObjectMetadata.OBJECT_SIZE];
    outputBuffer.get(filteredBytes, 0, LegacyObjectMetadata.OBJECT_SIZE);
    final ObjectMetadata filtered = LegacyObjectMetadata.fromBytes(filteredBytes);
    assertThat(filtered.getName(), is(s));
    assertThat(filtered.getSize(), is(3L));
  }

  @Test
  public void filter_legalhold() throws IOException {
    final String s = UUID.randomUUID().toString().replace("-", "") + "0000";
    int retention = 20 * 365* 24* 3600;
    final ObjectMetadata o1 = LegacyObjectMetadata.fromMetadata(s, 1, 1, (byte)99, -1);
    final ObjectMetadata o2 = LegacyObjectMetadata.fromMetadata(s, 2, 2, (byte)-1, 3600);
    final ObjectMetadata o3 = LegacyObjectMetadata.fromMetadata(s, 3, 3, (byte)-1, retention-100);
    final ByteArrayOutputStream source = new ByteArrayOutputStream();
    ObjectFileUtil.writeObjectFileVersion(source);
    source.write(o1.toBytes());
    source.write(o2.toBytes());
    source.write(o3.toBytes());

    final ByteArrayInputStream in = new ByteArrayInputStream(source.toByteArray());
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final Set<Integer> pointSuffixList = new HashSet<Integer>();
    ObjectFile.filter(in, out, 1, 3, 1, 3, new HashSet<Integer>(), 0, 100, -1L, -1L, true);
    assertThat(out.size(), is(ObjectFileVersion.VERSION_HEADER_LENGTH + LegacyObjectMetadata.OBJECT_SIZE));

    ByteBuffer outputBuffer = ByteBuffer.wrap(out.toByteArray());
    outputBuffer.position(ObjectFileVersion.VERSION_HEADER_LENGTH);
    byte[] filteredBytes = new byte[LegacyObjectMetadata.OBJECT_SIZE];
    outputBuffer.get(filteredBytes, 0, LegacyObjectMetadata.OBJECT_SIZE);
    final ObjectMetadata filtered = LegacyObjectMetadata.fromBytes(filteredBytes);
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
      final String suffix, final Class<Exception> expectedException) throws FileNotFoundException, IOException {
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
