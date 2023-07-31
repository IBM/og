/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import com.google.common.io.BaseEncoding;
import com.ibm.og.object.LegacyObjectMetadata;
import com.ibm.og.object.ObjectFileHeader;
import com.ibm.og.object.ObjectFileUtil;
import com.ibm.og.object.ObjectFileVersion;
import com.ibm.og.object.ObjectMetadata;
import com.ibm.og.object.RandomObjectPopulator;
import com.ibm.og.util.ObjectManagerUtils;
import org.junit.After;
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
  static final String prefix = "id_";
  static final String suffix = ".object";

  @Before
  public void before() throws IOException {
    this.nonExistent = new File("/nonexistent");
    this.exists = this.folder.newFile();
  }

  protected File[] getIdFiles(String prefix, String suffix) {
    final File dir = new File(".");
    final File[] files = dir.listFiles(new ObjectManagerUtils.IdFilter(prefix, suffix));
    return files;
  }


  @After
  public void deleteTempFiles() {
    final File[] files = getIdFiles(this.prefix, suffix);
    for (final File id : files) {
      id.delete();
    }
  }

  protected ObjectMetadata generateIdV3WithVersion() {
    return LegacyObjectMetadata.fromMetadata(UUID.randomUUID().toString().replace("-", "") + "0000",
            0, -1, (byte)0, -1,
            UUID.randomUUID().toString().replace("-", ""));
  }

  protected ObjectMetadata generateIdV3WithoutVersion() {
    return LegacyObjectMetadata.fromMetadata(UUID.randomUUID().toString().replace("-", "") + "0000",
            0, -1, (byte)0, -1, null);
  }

  protected ObjectMetadata generateIdV2() {
    return LegacyObjectMetadata.fromMetadata(UUID.randomUUID().toString().replace("-", "") + "0000",
            0, -1, (byte) 0, -1, null);
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
    return new Object[][] {{null, "id_0.object"}};
  }

  @DataProvider
  public static Object[][] provideInvalidStreamsForWrite() {
    return new Object[][] {{null, new ByteArrayOutputStream()},
        {new ByteArrayInputStream(new byte[] {}), null}};

  }

  @Test
  @UseDataProvider("provideInvalidStreamsForWrite")
  public void invalidWrite(final InputStream in, final OutputStream out) throws IOException {
    this.thrown.expect(NullPointerException.class);
    ObjectFile.write(in, out);
  }

  @Test
  public void writeV3WithoutVersion() throws IOException {
    final String objectMetadata = UUID.randomUUID().toString().replace("-", "") + "0000,0,0,99,3600";
    String versionHeaderString = "VERSION:3.0\n";
    String fileHeaderString = "7,18,0,8,4,1,4\n";
    final ByteBuffer inputByteBuffer = ByteBuffer.allocate(versionHeaderString.getBytes().length +
            fileHeaderString.getBytes().length + objectMetadata.getBytes().length);
    inputByteBuffer.put(versionHeaderString.getBytes());
    inputByteBuffer.put(fileHeaderString.getBytes());
    inputByteBuffer.put(objectMetadata.getBytes());
    final InputStream in = new ByteArrayInputStream(inputByteBuffer.array());
    final ObjectFile.ObjectFileOutputStream out = new ObjectFile.ObjectFileOutputStream(this.prefix, RandomObjectPopulator.MAX_OBJECT_ARG, this.suffix, false, true);
    ObjectFile.write(in, out);
    out.close();
    int index=0;
    File outputFile = new File(this.prefix+Integer.toString(index) + this.suffix);
    byte[] bytes = new byte[(int)outputFile.length()];
    FileInputStream fis = new FileInputStream(outputFile);
    fis.read(bytes, 0, bytes.length);
    ByteBuffer outputBuffer = ByteBuffer.wrap(bytes);
    outputBuffer.position(ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH);
    byte[] filteredBytes = new byte[LegacyObjectMetadata.OBJECT_SIZE];
    outputBuffer.get(filteredBytes, 0, LegacyObjectMetadata.OBJECT_SIZE);

    final ObjectMetadata object = LegacyObjectMetadata.fromBytes(filteredBytes, false);
    assertThat(objectMetadata, is(String.format("%s,%s,%s,%s,%s", object.getName(), object.getSize(),
            object.getContainerSuffix(), object.getNumberOfLegalHolds(), object.getRetention())));
  }

  @Test
  public void writeV3WithVersion() throws IOException {
    final String objectMetadata = UUID.randomUUID().toString().replace("-", "") + "0000," +
            UUID.randomUUID().toString().replace("-", "") +
            ",0,0,99,3600";
    String versionHeaderString = "VERSION:3.0\n";
    String fileHeaderString = "7,18,16,8,4,1,4\n";
    final ByteBuffer inputByteBuffer = ByteBuffer.allocate(versionHeaderString.getBytes().length +
            fileHeaderString.getBytes().length + objectMetadata.getBytes().length);
    inputByteBuffer.put(versionHeaderString.getBytes());
    inputByteBuffer.put(fileHeaderString.getBytes());
    inputByteBuffer.put(objectMetadata.getBytes());
    final InputStream in = new ByteArrayInputStream(inputByteBuffer.array());
    final ObjectFile.ObjectFileOutputStream out = new ObjectFile.ObjectFileOutputStream(this.prefix, RandomObjectPopulator.MAX_OBJECT_ARG, this.suffix, false, true);
    ObjectFile.write(in, out);
    out.close();
    int index=0;
    File outputFile = new File(this.prefix+Integer.toString(index) + this.suffix);
    byte[] bytes = new byte[(int)outputFile.length()];
    FileInputStream fis = new FileInputStream(outputFile);
    fis.read(bytes, 0, bytes.length);
    ByteBuffer outputBuffer = ByteBuffer.wrap(bytes);
    outputBuffer.position(ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH);
    byte[] filteredBytes = new byte[LegacyObjectMetadata.OBJECT_SIZE + LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE];
    outputBuffer.get(filteredBytes, 0, LegacyObjectMetadata.OBJECT_SIZE + LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE);

    final ObjectMetadata object = LegacyObjectMetadata.fromBytes(filteredBytes, true);
    assertThat(objectMetadata, is(String.format("%s,%s,%s,%s,%s,%s", object.getName(), object.getVersion(), object.getSize(),
            object.getContainerSuffix(), object.getNumberOfLegalHolds(), object.getRetention())));
  }

  @Test
  public void writeV2() throws IOException {
    final String objectMetadata = UUID.randomUUID().toString().replace("-", "") + "0000,0,0,99,3600";
    String versionHeaderString = "VERSION:2.0\n";
    final ByteBuffer inputByteBuffer = ByteBuffer.allocate(versionHeaderString.getBytes().length +
           objectMetadata.getBytes().length);
    inputByteBuffer.put(versionHeaderString.getBytes());
    inputByteBuffer.put(objectMetadata.getBytes());
    final InputStream in = new ByteArrayInputStream(inputByteBuffer.array());
//    final ByteArrayOutputStream out = new ByteArrayOutputStream(ObjectFileVersion.VERSION_HEADER_LENGTH +
//            LegacyObjectMetadata.OBJECT_SIZE);
    final ObjectFile.ObjectFileOutputStream out = new ObjectFile.ObjectFileOutputStream(this.prefix, RandomObjectPopulator.MAX_OBJECT_ARG, this.suffix, false, true);
    ObjectFile.write(in, out);
    out.close();
    int index=0;
    File outputFile = new File(this.prefix+Integer.toString(index) + this.suffix);
    byte[] bytes = new byte[(int)outputFile.length()];
    FileInputStream fis = new FileInputStream(outputFile);
    fis.read(bytes, 0, bytes.length);
    ByteBuffer outputBuffer = ByteBuffer.wrap(bytes);
    outputBuffer.position(ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH);
    byte[] filteredBytes = new byte[LegacyObjectMetadata.OBJECT_SIZE];
    outputBuffer.get(filteredBytes, 0, LegacyObjectMetadata.OBJECT_SIZE);

    final ObjectMetadata object = LegacyObjectMetadata.fromBytes(filteredBytes, false);
    assertThat(objectMetadata, is(String.format("%s,%s,%s,%s,%s", object.getName(), object.getSize(),
        object.getContainerSuffix(), object.getNumberOfLegalHolds(), object.getRetention())));
  }

  @Test
  public void writeV1() throws IOException {
    final String objectName = UUID.randomUUID().toString().replace("-", "") + "0000";
    final String objectMetadata = objectName + ",0,0";
    final ByteBuffer inputByteBuffer = ByteBuffer.allocate(objectMetadata.getBytes().length);
    inputByteBuffer.put(objectMetadata.getBytes());
    final InputStream in = new ByteArrayInputStream(inputByteBuffer.array());
    final ObjectFile.ObjectFileOutputStream out = new ObjectFile.ObjectFileOutputStream(this.prefix, RandomObjectPopulator.MAX_OBJECT_ARG, this.suffix, false, true);
    ObjectFile.write(in, out);
    out.close();
    int index=0;
    File outputFile = new File(this.prefix+Integer.toString(index) + this.suffix);
    byte[] bytes = new byte[(int)outputFile.length()];
    FileInputStream fis = new FileInputStream(outputFile);
    fis.read(bytes, 0, bytes.length);
    ByteBuffer outputBuffer = ByteBuffer.wrap(bytes);
    outputBuffer.position(ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH);
    byte[] filteredBytes = new byte[LegacyObjectMetadata.OBJECT_SIZE];
    outputBuffer.get(filteredBytes, 0, LegacyObjectMetadata.OBJECT_SIZE);

    final ObjectMetadata object = LegacyObjectMetadata.fromBytes(filteredBytes, false);
    final LegacyObjectMetadata expectedObject = LegacyObjectMetadata.fromMetadata(objectName, 0,
            0, (byte)0, -1, null);
    assertThat(String.format("%s,%s,%s,%s,%s", expectedObject.getName(), expectedObject.getSize(),
            expectedObject.getContainerSuffix(), expectedObject.getNumberOfLegalHolds(), expectedObject.getRetention()),
            is(String.format("%s,%s,%s,%s,%s", object.getName(), object.getSize(),
            object.getContainerSuffix(), object.getNumberOfLegalHolds(), object.getRetention())));
  }

  @Test
  @UseDataProvider("provideInvalidStreams")
  public void invalidRead(final InputStream in, final String out) throws IOException {
    this.thrown.expect(NullPointerException.class);
    ObjectFile.read(in, out, true);
  }

  @Test
  public void read() throws IOException {
    final String objectString = UUID.randomUUID().toString().replace("-", "") + "0000";
    final LegacyObjectMetadata object = LegacyObjectMetadata.fromMetadata(objectString, 1024,
            0, (byte)0, -1, null);
    ByteBuffer inputBuffer = ByteBuffer.allocate(ObjectFileVersion.VERSION_HEADER_LENGTH + LegacyObjectMetadata.OBJECT_SIZE);
    inputBuffer.put(ObjectFileVersion.fromMetadata((byte)2, (byte)0).getBytes());
    inputBuffer.put(object.toBytes(false));
    final InputStream in = new ByteArrayInputStream(inputBuffer.array());

    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final String outputFilename = "id_0.object";
    ObjectFile.read(in, outputFilename, true);


    byte[] bytes = new byte[(int)new File(outputFilename).length()];
    final FileInputStream fis = new FileInputStream(outputFilename);
    fis.read(bytes);
    fis.close();
    out.write(bytes);
    ByteBuffer outBuffer = ByteBuffer.wrap(bytes);
    outBuffer.position(ObjectFileVersion.VERSION_HEADER_LENGTH);
    byte[] objectBuffer = new byte[LegacyObjectMetadata.OBJECT_SIZE];
    outBuffer.get(objectBuffer, 0, LegacyObjectMetadata.OBJECT_SIZE);

    assertThat(new String(out.toByteArray()), is(String.format("%s:%s.%s\n%s,%s,%s,%s,%s%n", "VERSION",
          2, 0, object.getName(),
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
    final String outputFilename = "id_0.object";
    ObjectFile.read(in, outputFilename, true);

    byte[] bytes = new byte[(int)new File(outputFilename).length()];
    final FileInputStream fis = new FileInputStream(outputFilename);
    fis.read(bytes);
    fis.close();
    //final ByteArrayOutputStream out = new ByteArrayOutputStream(fis.read(bytes));
    out.write(bytes);
    ByteBuffer outBuffer = ByteBuffer.wrap(bytes);
    outBuffer.position(ObjectFileVersion.VERSION_HEADER_LENGTH);
    byte[] objectBuffer = new byte[LegacyObjectMetadata.OBJECT_SIZE];
    outBuffer.get(objectBuffer, 0, LegacyObjectMetadata.OBJECT_SIZE);

    final LegacyObjectMetadata expectedObject = LegacyObjectMetadata.fromMetadata(objectName, 0,
            0, (byte)0, -1, null);

    assertThat(new String(out.toByteArray()), is(String.format("%s:%s.%s\n%s,%s,%s,%s,%s%n", "VERSION",
            1, 0, expectedObject.getName(),
            expectedObject.getSize(), expectedObject.getContainerSuffix(), expectedObject.getNumberOfLegalHolds(),
            expectedObject.getRetention())));
  }


  @Test
  public void readV2V3MixedObjectFiles() throws IOException {
    this.thrown.expect(IllegalArgumentException.class);
    final String objectString = UUID.randomUUID().toString().replace("-", "") + "0000";
    final LegacyObjectMetadata object = LegacyObjectMetadata.fromMetadata(objectString, 1024,
            0, (byte)0, -1, null);
    ByteBuffer inputBuffer = ByteBuffer.allocate(ObjectFileVersion.VERSION_HEADER_LENGTH + LegacyObjectMetadata.OBJECT_SIZE);
    inputBuffer.put(ObjectFileVersion.fromMetadata((byte)2, (byte)0).getBytes());
    inputBuffer.put(object.toBytes(false));

    final InputStream in = new ByteArrayInputStream(inputBuffer.array());

    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final String outputFilename = "id_0.object";
    ObjectFile.read(in, outputFilename, true);

    ByteBuffer inputBuffer2 = ByteBuffer.allocate(ObjectFileVersion.VERSION_HEADER_LENGTH +
            ObjectFileHeader.HEADER_LENGTH + LegacyObjectMetadata.OBJECT_SIZE);

    final String objectString2 = UUID.randomUUID().toString().replace("-", "") + "0000";
    final LegacyObjectMetadata object2 = LegacyObjectMetadata.fromMetadata(objectString2, 1024,
            0, (byte)0, -1, null);

    inputBuffer2.put(ObjectFileVersion.fromMetadata((byte)3, (byte)0).getBytes());
    inputBuffer2.put(ObjectFileHeader.fromMetadata(LegacyObjectMetadata.OBJECT_NAME_SIZE,
            LegacyObjectMetadata.OBJECT_VERSION_MIN_SIZE,  LegacyObjectMetadata.OBJECT_SIZE_SIZE,
            LegacyObjectMetadata.OBJECT_SUFFIX_SIZE, LegacyObjectMetadata.OBJECT_LEGAL_HOLDS_SIZE,
            LegacyObjectMetadata.OBJECT_RETENTION_SIZE).getBytes());
    inputBuffer2.put(object2.toBytes(false));

    final InputStream in2 = new ByteArrayInputStream(inputBuffer2.array());
    ObjectFile.read(in2, outputFilename, false);
  }


  @Test
  public void readV3V2MixedObjectFiles() throws IOException {
    this.thrown.expect(IllegalArgumentException.class);
    final String objectString = UUID.randomUUID().toString().replace("-", "") + "0000";
    final LegacyObjectMetadata object = LegacyObjectMetadata.fromMetadata(objectString, 1024,
            0, (byte)0, -1, null);

    ByteBuffer inputBuffer = ByteBuffer.allocate(ObjectFileVersion.VERSION_HEADER_LENGTH +
            ObjectFileHeader.HEADER_LENGTH + LegacyObjectMetadata.OBJECT_SIZE);
    inputBuffer.put(ObjectFileVersion.fromMetadata((byte)3, (byte)0).getBytes());
    inputBuffer.put(ObjectFileHeader.fromMetadata(LegacyObjectMetadata.OBJECT_NAME_SIZE,
            LegacyObjectMetadata.OBJECT_VERSION_MIN_SIZE,  LegacyObjectMetadata.OBJECT_SIZE_SIZE,
            LegacyObjectMetadata.OBJECT_SUFFIX_SIZE, LegacyObjectMetadata.OBJECT_LEGAL_HOLDS_SIZE,
            LegacyObjectMetadata.OBJECT_RETENTION_SIZE).getBytes());
    inputBuffer.put(object.toBytes(false));

    final InputStream in = new ByteArrayInputStream(inputBuffer.array());

    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final String outputFilename = "id_0.object";
    ObjectFile.read(in, outputFilename, true);



    final String objectString2 = UUID.randomUUID().toString().replace("-", "") + "0000";
    final LegacyObjectMetadata object2 = LegacyObjectMetadata.fromMetadata(objectString2, 1024,
            0, (byte)0, -1, null);

    ByteBuffer inputBuffer2 = ByteBuffer.allocate(ObjectFileVersion.VERSION_HEADER_LENGTH + LegacyObjectMetadata.OBJECT_SIZE);
    inputBuffer2.put(ObjectFileVersion.fromMetadata((byte)2, (byte)0).getBytes());
    inputBuffer2.put(object2.toBytes(false));

    final InputStream in2 = new ByteArrayInputStream(inputBuffer2.array());
    ObjectFile.read(in2, outputFilename, false);

  }

  @Test
  public void readV3V3WithAndWithoutObjectVersion() throws IOException {
    this.thrown.expect(IllegalArgumentException.class);
    final ObjectMetadata object = generateIdV3WithoutVersion();

    ByteBuffer inputBuffer = ByteBuffer.allocate(ObjectFileVersion.VERSION_HEADER_LENGTH +
            ObjectFileHeader.HEADER_LENGTH + LegacyObjectMetadata.OBJECT_SIZE);
    inputBuffer.put(ObjectFileVersion.fromMetadata((byte)3, (byte)0).getBytes());
    inputBuffer.put(ObjectFileHeader.fromMetadata(LegacyObjectMetadata.OBJECT_NAME_SIZE,
            LegacyObjectMetadata.OBJECT_VERSION_MIN_SIZE,  LegacyObjectMetadata.OBJECT_SIZE_SIZE,
            LegacyObjectMetadata.OBJECT_SUFFIX_SIZE, LegacyObjectMetadata.OBJECT_LEGAL_HOLDS_SIZE,
            LegacyObjectMetadata.OBJECT_RETENTION_SIZE).getBytes());
    inputBuffer.put(object.toBytes(false));

    final InputStream in = new ByteArrayInputStream(inputBuffer.array());

    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final String outputFilename = "id_0.object";
    ObjectFile.read(in, outputFilename, true);

    final ObjectMetadata object2 = generateIdV3WithVersion();

    ByteBuffer inputBuffer2 = ByteBuffer.allocate(ObjectFileVersion.VERSION_HEADER_LENGTH +
            ObjectFileHeader.HEADER_LENGTH + LegacyObjectMetadata.OBJECT_SIZE + LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE);
    inputBuffer2.put(ObjectFileVersion.fromMetadata((byte)3, (byte)0).getBytes());
    inputBuffer2.put(ObjectFileHeader.fromMetadata(LegacyObjectMetadata.OBJECT_NAME_SIZE,
            LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE,  LegacyObjectMetadata.OBJECT_SIZE_SIZE,
            LegacyObjectMetadata.OBJECT_SUFFIX_SIZE, LegacyObjectMetadata.OBJECT_LEGAL_HOLDS_SIZE,
            LegacyObjectMetadata.OBJECT_RETENTION_SIZE).getBytes());
    inputBuffer2.put(object2.toBytes(true));

    final InputStream in2 = new ByteArrayInputStream(inputBuffer2.array());
    ObjectFile.read(in2, outputFilename, false);

  }

  @Test
  public void readV3V3WithAndWithoutObjectVersionOutputToConsole() throws IOException {
    //this.thrown.expect(IllegalArgumentException.class);
    final ObjectMetadata object = generateIdV3WithoutVersion();

    ByteBuffer inputBuffer = ByteBuffer.allocate(ObjectFileVersion.VERSION_HEADER_LENGTH +
            ObjectFileHeader.HEADER_LENGTH + LegacyObjectMetadata.OBJECT_SIZE);
    inputBuffer.put(ObjectFileVersion.fromMetadata((byte)3, (byte)0).getBytes());
    inputBuffer.put(ObjectFileHeader.fromMetadata(LegacyObjectMetadata.OBJECT_NAME_SIZE,
            LegacyObjectMetadata.OBJECT_VERSION_MIN_SIZE,  LegacyObjectMetadata.OBJECT_SIZE_SIZE,
            LegacyObjectMetadata.OBJECT_SUFFIX_SIZE, LegacyObjectMetadata.OBJECT_LEGAL_HOLDS_SIZE,
            LegacyObjectMetadata.OBJECT_RETENTION_SIZE).getBytes());
    inputBuffer.put(object.toBytes(false));

    final InputStream in = new ByteArrayInputStream(inputBuffer.array());

    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    //final String outputFilename = "id_0.object";
    ObjectFile.read(in, null, true);

    final ObjectMetadata object2 = generateIdV3WithVersion();

    ByteBuffer inputBuffer2 = ByteBuffer.allocate(ObjectFileVersion.VERSION_HEADER_LENGTH +
            ObjectFileHeader.HEADER_LENGTH + LegacyObjectMetadata.OBJECT_SIZE + LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE);
    inputBuffer2.put(ObjectFileVersion.fromMetadata((byte)3, (byte)0).getBytes());
    inputBuffer2.put(ObjectFileHeader.fromMetadata(LegacyObjectMetadata.OBJECT_NAME_SIZE,
            LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE,  LegacyObjectMetadata.OBJECT_SIZE_SIZE,
            LegacyObjectMetadata.OBJECT_SUFFIX_SIZE, LegacyObjectMetadata.OBJECT_LEGAL_HOLDS_SIZE,
            LegacyObjectMetadata.OBJECT_RETENTION_SIZE).getBytes());
    inputBuffer2.put(object2.toBytes(true));

    final InputStream in2 = new ByteArrayInputStream(inputBuffer2.array());
    ObjectFile.read(in2, null, false);

  }

  @DataProvider
  public static Object[][] provideInvalidFilter() throws FileNotFoundException, IOException {
    final InputStream in = new ByteArrayInputStream(new byte[] {});
    //final OutputStream out = new ByteArrayOutputStream();
    final String outputFile = new String("filtered");
    ObjectFile.ObjectFileOutputStream out = (ObjectFile.ObjectFileOutputStream)ObjectFile.getOutputStream(false, RandomObjectPopulator.MAX_OBJECT_ARG, false, true, outputFile);
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
  public void invalidFilter(final InputStream in, final ObjectFile.ObjectFileOutputStream out, final long minFilesize,
                            final long maxFilesize, final int minContainerSuffix, final int maxContainerSuffix, final Set<Integer> containerSuffixes,
                            final int minLegalHolds, final int maxLegalHolds, final long minRetention, final long maxRetention, final Class<Exception> expectedException) throws IOException {
    this.thrown.expect(expectedException);
    ObjectFile.filter(in, out, minFilesize, maxFilesize, minContainerSuffix, maxContainerSuffix, containerSuffixes,
            minLegalHolds, maxLegalHolds, minRetention, maxRetention, true, false);
  }

  @Test
  public void filter() throws IOException {
    final String s = UUID.randomUUID().toString().replace("-", "") + "0000";
    final ObjectMetadata o1 = LegacyObjectMetadata.fromMetadata(s, 1, 1, (byte)0,
            -1, null);
    final ObjectMetadata o2 = LegacyObjectMetadata.fromMetadata(s, 2, 2, (byte)0,
            -1, null);
    final ObjectMetadata o3 = LegacyObjectMetadata.fromMetadata(s, 3, 3, (byte)0,
            -1, null);
    final ByteArrayOutputStream source = new ByteArrayOutputStream();
    ObjectFileUtil.writeObjectFileVersion(source, 2, 0);
    source.write(o1.toBytes(false));
    source.write(o2.toBytes(false));
    source.write(o3.toBytes(false));

    final ByteArrayInputStream in = new ByteArrayInputStream(source.toByteArray());
    final String outputFile = new String("id_");
    ObjectFile.ObjectFileOutputStream out = (ObjectFile.ObjectFileOutputStream)ObjectFile.getOutputStream(false, RandomObjectPopulator.MAX_OBJECT_ARG, false, true, outputFile);

    final Set<Integer> pointSuffixList = new HashSet<Integer>();
    ObjectFile.filter(in, out, 1, 2, 2, 3,
            new HashSet<Integer>(), 0, 100, -1L,
            -1L, true, false);
    out.close();
    long size = new File("id_0.object").length();
    assertThat((int)size, is(ObjectFileVersion.VERSION_HEADER_LENGTH +
            ObjectFileHeader.HEADER_LENGTH + LegacyObjectMetadata.OBJECT_SIZE));
    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File("id_0.object")));
    byte[] buffer = new byte[(int)size];
    bis.read(buffer);
    ByteBuffer outputBuffer = ByteBuffer.wrap(buffer);
    outputBuffer.position(ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH);
    byte[] filteredBytes = new byte[LegacyObjectMetadata.OBJECT_SIZE];
    outputBuffer.get(filteredBytes, 0, LegacyObjectMetadata.OBJECT_SIZE);
    final ObjectMetadata filtered = LegacyObjectMetadata.fromBytes(filteredBytes, false);
    assertThat(filtered.getName(), is(s));
    assertThat(filtered.getSize(), is(2L));
  }

  @Test
  public void filterV3() throws IOException {
    final String name = UUID.randomUUID().toString().replace("-", "") + "0000";
    final String version = UUID.randomUUID().toString().replace("-", "") + "";
    final ObjectMetadata o1 = LegacyObjectMetadata.fromMetadata(name, 1, 1, (byte)0,
            -1, version);
    final ObjectMetadata o2 = LegacyObjectMetadata.fromMetadata(name, 2, 2, (byte)0,
            -1, version);
    final ObjectMetadata o3 = LegacyObjectMetadata.fromMetadata(name, 3, 3, (byte)0,
            -1, version);
    final ByteArrayOutputStream source = new ByteArrayOutputStream();
    ObjectFileUtil.writeObjectFileVersion(source, 3, 0);
    ObjectFileUtil.writeObjectFileHeader(source, LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE);
    source.write(o1.toBytes(true));
    source.write(o2.toBytes(true));
    source.write(o3.toBytes(true));

    final ByteArrayInputStream in = new ByteArrayInputStream(source.toByteArray());
    //final ByteArrayOutputStream out = new ByteArrayOutputStream();
    //final String out = "filtered";
    final String outputFile = new String("id_");
    ObjectFile.ObjectFileOutputStream out = (ObjectFile.ObjectFileOutputStream)ObjectFile.getOutputStream(false, RandomObjectPopulator.MAX_OBJECT_ARG, false, true, outputFile);

    final Set<Integer> pointSuffixList = new HashSet<Integer>();
    ObjectFile.filter(in, out, 1, 2, 2, 3,
            new HashSet<Integer>(), 0, 100, -1L,
            -1L, true, false);
    out.close();
    long size = new File("id_0.object").length();
    assertThat((int)size, is(ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH + LegacyObjectMetadata.OBJECT_SIZE + LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE));
    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File("id_0.object")));
    byte[] buffer = new byte[(int)size];
    bis.read(buffer);
    ByteBuffer outputBuffer = ByteBuffer.wrap(buffer);
    outputBuffer.position(ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH);
    byte[] filteredBytes = new byte[LegacyObjectMetadata.OBJECT_SIZE + LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE];
    outputBuffer.get(filteredBytes, 0, LegacyObjectMetadata.OBJECT_SIZE + LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE);
    final ObjectMetadata filtered = LegacyObjectMetadata.fromBytes(filteredBytes, true);
    assertThat(filtered.getName(), is(name));
    assertThat(filtered.getSize(), is(2L));
  }

  @Test
  public void filterV3WithoutVersion() throws IOException {
    final String name = UUID.randomUUID().toString().replace("-", "") + "0000";
    final String version = UUID.randomUUID().toString().replace("-", "") + "";
    final ObjectMetadata o1 = LegacyObjectMetadata.fromMetadata(name, 1, 1, (byte)0,
            -1, version);
    final ObjectMetadata o2 = LegacyObjectMetadata.fromMetadata(name, 2, 2, (byte)0,
            -1, version);
    final ObjectMetadata o3 = LegacyObjectMetadata.fromMetadata(name, 3, 3, (byte)0,
            -1, version);
    final ByteArrayOutputStream source = new ByteArrayOutputStream();
    ObjectFileUtil.writeObjectFileVersion(source, 3, 0);
    ObjectFileUtil.writeObjectFileHeader(source, LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE);
    source.write(o1.toBytes(true));
    source.write(o2.toBytes(true));
    source.write(o3.toBytes(true));

    final ByteArrayInputStream in = new ByteArrayInputStream(source.toByteArray());
    //final ByteArrayOutputStream out = new ByteArrayOutputStream();
    //final String out = "filtered";
    final String outputFile = new String("id_");
    ObjectFile.ObjectFileOutputStream out = (ObjectFile.ObjectFileOutputStream)ObjectFile.getOutputStream(false,
            RandomObjectPopulator.MAX_OBJECT_ARG, false, true, outputFile);

    final Set<Integer> pointSuffixList = new HashSet<Integer>();
    ObjectFile.filter(in, out, 1, 2, 2, 3,
            new HashSet<Integer>(), 0, 100, -1L,
            -1L, true, false);
    out.close();
    long size = new File("id_0.object").length();
    assertThat((int)size, is(ObjectFileVersion.VERSION_HEADER_LENGTH +
            ObjectFileHeader.HEADER_LENGTH + LegacyObjectMetadata.OBJECT_SIZE + LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE));
    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File("id_0.object")));
    byte[] buffer = new byte[(int)size];
    bis.read(buffer);
    ByteBuffer outputBuffer = ByteBuffer.wrap(buffer);
    outputBuffer.position(ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH);
    byte[] filteredBytes = new byte[LegacyObjectMetadata.OBJECT_SIZE + LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE];
    outputBuffer.get(filteredBytes, 0, LegacyObjectMetadata.OBJECT_SIZE + LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE);
    final ObjectMetadata filtered = LegacyObjectMetadata.fromBytes(filteredBytes, true);
    assertThat(filtered.getName(), is(name));
    assertThat(filtered.getSize(), is(2L));
  }


  @Test
  public void filterV2V3() throws IOException {
    final String name = UUID.randomUUID().toString().replace("-", "") + "0000";
    final ObjectMetadata o1 = LegacyObjectMetadata.fromMetadata(name, 1, 1, (byte)0,
            -1, null);
    final ObjectMetadata o2 = LegacyObjectMetadata.fromMetadata(name, 2, 2, (byte)0,
            -1, null);
    final ObjectMetadata o3 = LegacyObjectMetadata.fromMetadata(name, 3, 3, (byte)0,
            -1, null);
    final ByteArrayOutputStream source = new ByteArrayOutputStream();
    ObjectFileUtil.writeObjectFileVersion(source, 2, 0);
    source.write(o1.toBytes(false));
    source.write(o2.toBytes(false));
    source.write(o3.toBytes(false));

    final String name2 = UUID.randomUUID().toString().replace("-", "") + "0000";
    final String version = UUID.randomUUID().toString().replace("-", "") + "";
    final ObjectMetadata o4 = LegacyObjectMetadata.fromMetadata(name2, 1, 1, (byte)0,
            -1, version);
    final ObjectMetadata o5 = LegacyObjectMetadata.fromMetadata(name2, 2, 2, (byte)0,
            -1, version);
    final ObjectMetadata o6 = LegacyObjectMetadata.fromMetadata(name2, 3, 3, (byte)0,
            -1, version);
    final ByteArrayOutputStream source2 = new ByteArrayOutputStream();
    ObjectFileUtil.writeObjectFileVersion(source2, 3, 0);
    ObjectFileUtil.writeObjectFileHeader(source2, LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE);
    source2.write(o4.toBytes(true));
    source2.write(o5.toBytes(true));
    source2.write(o6.toBytes(true));

    final ByteArrayInputStream in = new ByteArrayInputStream(source.toByteArray());
    final ByteArrayInputStream in2 = new ByteArrayInputStream(source2.toByteArray());

    final String outputFile = new String("id_");
    ObjectFile.ObjectFileOutputStream out = (ObjectFile.ObjectFileOutputStream)ObjectFile.getOutputStream(
            false, RandomObjectPopulator.MAX_OBJECT_ARG, false, true, outputFile);

    ObjectFile.filter(in, out, 1, 2, 2, 3,
            new HashSet<Integer>(), 0, 100, -1L,
            -1L, true, false);

    ObjectFile.filter(in2, out, 1, 2, 2, 3,
            new HashSet<Integer>(), 0, 100, -1L,
            -1L, false, false);

    out.close();
    long size = new File("id_0.object").length();
    assertThat((int)size, is(ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH +
            2 * (LegacyObjectMetadata.OBJECT_SIZE + LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE)));
    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File("id_0.object")));
    byte[] buffer = new byte[(int)size];
    bis.read(buffer);
    ByteBuffer outputBuffer = ByteBuffer.wrap(buffer);
    outputBuffer.position(ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH);
    byte[] filteredBytes = new byte[LegacyObjectMetadata.OBJECT_SIZE + LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE];
    outputBuffer.get(filteredBytes, 0, LegacyObjectMetadata.OBJECT_SIZE + LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE);
    final ObjectMetadata filtered = LegacyObjectMetadata.fromBytes(filteredBytes, true);
    assertThat(filtered.getName(), is(name));
    assertThat(filtered.getSize(), is(2L));

  }

  @Test
  public void filter_retention() throws IOException {
    final String s = UUID.randomUUID().toString().replace("-", "") + "0000";
    int retention = 30 * 365* 24* 3600;
    final ObjectMetadata o1 = LegacyObjectMetadata.fromMetadata(s, 1, 1, (byte)0,
            -1, null);
    final ObjectMetadata o2 = LegacyObjectMetadata.fromMetadata(s, 2, 2, (byte)0,
            3600, null);
    final ObjectMetadata o3 = LegacyObjectMetadata.fromMetadata(s, 3, 3, (byte)0,
            retention-100, null);
    final ByteArrayOutputStream source = new ByteArrayOutputStream();
    ObjectFileUtil.writeObjectFileVersion(source, 2, 0);
    source.write(o1.toBytes(false));
    source.write(o2.toBytes(false));
    source.write(o3.toBytes(false));

    final ByteArrayInputStream in = new ByteArrayInputStream(source.toByteArray());
    final String outputFile = new String("id_");
    ObjectFile.ObjectFileOutputStream out = (ObjectFile.ObjectFileOutputStream)ObjectFile.getOutputStream(
            false, RandomObjectPopulator.MAX_OBJECT_ARG, false, true, outputFile);

    final Set<Integer> pointSuffixList = new HashSet<Integer>();
    ObjectFile.filter(in, out, 1, 3, 1, 3,
            new HashSet<Integer>(), 0, 100, 3601L, retention,
            true, false);
    out.close();
    long size = new File("id_0.object").length();
    assertThat((int)size, is(ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH + LegacyObjectMetadata.OBJECT_SIZE));
    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File("id_0.object")));
    byte[] buffer = new byte[(int)size];
    bis.read(buffer);
    ByteBuffer outputBuffer = ByteBuffer.wrap(buffer);
    outputBuffer.position(ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH);
    byte[] filteredBytes = new byte[LegacyObjectMetadata.OBJECT_SIZE];
    outputBuffer.get(filteredBytes, 0, LegacyObjectMetadata.OBJECT_SIZE);
    final ObjectMetadata filtered = LegacyObjectMetadata.fromBytes(filteredBytes, false);
    assertThat(filtered.getName(), is(s));
    assertThat(filtered.getSize(), is(3L));
  }

  @Test
  public void filter_legalhold() throws IOException {
    final String s = UUID.randomUUID().toString().replace("-", "") + "0000";
    int retention = 20 * 365* 24* 3600;
    final ObjectMetadata o1 = LegacyObjectMetadata.fromMetadata(s, 1, 1, (byte)99,
            -1, null);
    final ObjectMetadata o2 = LegacyObjectMetadata.fromMetadata(s, 2, 2, (byte)-1,
            3600, null);
    final ObjectMetadata o3 = LegacyObjectMetadata.fromMetadata(s, 3, 3, (byte)-1,
            retention-100, null);
    final ByteArrayOutputStream source = new ByteArrayOutputStream();
    ObjectFileUtil.writeObjectFileVersion(source, 2, 0);
    source.write(o1.toBytes(false));
    source.write(o2.toBytes(false));
    source.write(o3.toBytes(false));

    final ByteArrayInputStream in = new ByteArrayInputStream(source.toByteArray());
    final String outputFile = new String("id_");
    ObjectFile.ObjectFileOutputStream out = (ObjectFile.ObjectFileOutputStream)ObjectFile.getOutputStream(false, RandomObjectPopulator.MAX_OBJECT_ARG, false, true, outputFile);

    final Set<Integer> pointSuffixList = new HashSet<Integer>();
    ObjectFile.filter(in, out, 1, 3, 1, 3,
            new HashSet<Integer>(), 0, 100, -1L, -1L,
            true, false);
    out.close();
    long size = new File("id_0.object").length();
    assertThat((int)size, is(ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH + LegacyObjectMetadata.OBJECT_SIZE));

    //ByteBuffer outputBuffer = ByteBuffer.wrap(out.toByteArray());
    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File("id_0.object")));
    byte[] buffer = new byte[(int)size];
    bis.read(buffer);
    ByteBuffer outputBuffer = ByteBuffer.wrap(buffer);
    outputBuffer.position(ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH);
    byte[] filteredBytes = new byte[LegacyObjectMetadata.OBJECT_SIZE];
    outputBuffer.get(filteredBytes, 0, LegacyObjectMetadata.OBJECT_SIZE);
    final ObjectMetadata filtered = LegacyObjectMetadata.fromBytes(filteredBytes, false);
    assertThat(filtered.getName(), is(s));
    assertThat(filtered.getSize(), is(1L));
  }


  @Test
  public void shuffleTestV2() throws FileNotFoundException, IOException {
    final ObjectMetadata object1 = generateIdV2();
    final ObjectMetadata object2 = generateIdV2();
    final ObjectMetadata object3 = generateIdV2();
    OutputStream os = new FileOutputStream("id_0.object");
    ObjectFileUtil.writeObjectFileVersion(os, 2, 0);
    os.write(object1.toBytes(false));
    os.write(object2.toBytes(false));
    os.write(object3.toBytes(false));
    os.close();

    final ObjectMetadata object4 = generateIdV2();
    final ObjectMetadata object5 = generateIdV2();
    final ObjectMetadata object6 = generateIdV2();
    os = new FileOutputStream("id_1.object");
    ObjectFileUtil.writeObjectFileVersion(os, 2, 0);
    os.write(object4.toBytes(false));
    os.write(object5.toBytes(false));
    os.write(object6.toBytes(false));
    os.close();

    ObjectFile.shuffle("id_", ".", 4, -1);

    // check if the output file id_2.object exists
    File f = new File("id_2.object");
    assertThat(f.exists(), is(true));

    int size = ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH +
            4 * LegacyObjectMetadata.OBJECT_SIZE;
    assertThat((int)f.length(), is(size));

    BufferedInputStream is = new BufferedInputStream(new FileInputStream(f));

    ObjectFileVersion version = ObjectFileUtil.readObjectFileVersion(is);
    ObjectFileHeader header = ObjectFileUtil.readObjectFileHeader(is);
    is.skip(ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH);
    byte[] bytes = ObjectFileUtil.allocateObjectBuffer(3, 0, header);
    byte[] outBytes = new byte[LegacyObjectMetadata.OBJECT_SIZE];

    List<String> objectNames = new ArrayList<String>();
    objectNames.add(object2.getName());
    objectNames.add(object3.getName());
    objectNames.add(object5.getName());
    objectNames.add(object6.getName());

    is.read(bytes);
    ObjectMetadata objectMetadata = ObjectFileUtil.getObjectFromInputBuffer(3, 0, bytes,
            outBytes, false);
    assertThat(objectNames.contains(objectMetadata.getName()), is(true));
    is.read(bytes);
    objectMetadata = ObjectFileUtil.getObjectFromInputBuffer(3, 0, bytes,
            outBytes, false);
    assertThat(objectNames.contains(objectMetadata.getName()), is(true));

    is.read(bytes);
    objectMetadata = ObjectFileUtil.getObjectFromInputBuffer(3, 0, bytes,
            outBytes, false);
    assertThat(objectNames.contains(objectMetadata.getName()), is(true));

    is.read(bytes);
    objectMetadata = ObjectFileUtil.getObjectFromInputBuffer(3, 0, bytes,
            outBytes, false);
    assertThat(objectNames.contains(objectMetadata.getName()), is(true));

    assertThat(objectNames.contains(object1.getName()), is(false));
    assertThat(objectNames.contains(object4.getName()), is(false));

    is.close();
  }

  @Test
  public void shuffleTestV2V3Mixed() throws IOException {
    final ObjectMetadata object1 = generateIdV2();
    final ObjectMetadata object2 = generateIdV2();
    final ObjectMetadata object3 = generateIdV2();
    OutputStream os = new FileOutputStream("id_0.object");
    ObjectFileUtil.writeObjectFileVersion(os, 2, 0);
    os.write(object1.toBytes(false));
    os.write(object2.toBytes(false));
    os.write(object3.toBytes(false));
    os.close();

    final ObjectMetadata object4 = generateIdV3WithoutVersion();
    final ObjectMetadata object5 = generateIdV3WithoutVersion();
    final ObjectMetadata object6 = generateIdV3WithoutVersion();
    os = new FileOutputStream("id_1.object");
    ObjectFileUtil.writeObjectFileVersion(os, 3, 0);
    ObjectFileUtil.writeObjectFileHeader(os, 0);
    os.write(object4.toBytes(false));
    os.write(object5.toBytes(false));
    os.write(object6.toBytes(false));
    os.close();

    ObjectFile.shuffle("id_", ".", 4, -1);

    // check if the output file id_2.object exists
    File f = new File("id_2.object");
    assertThat(f.exists(), is(true));

    int size = ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH +
            4 * LegacyObjectMetadata.OBJECT_SIZE;
    assertThat((int)f.length(), is(size));

    BufferedInputStream is = new BufferedInputStream(new FileInputStream(f));

    ObjectFileVersion version = ObjectFileUtil.readObjectFileVersion(is);
    ObjectFileHeader header = ObjectFileUtil.readObjectFileHeader(is);
    is.skip(ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH);
    byte[] bytes = ObjectFileUtil.allocateObjectBuffer(3, 0, header);
    byte[] outBytes = new byte[LegacyObjectMetadata.OBJECT_SIZE];

    List<String> objectNames = new ArrayList<String>();
    objectNames.add(object2.getName());
    objectNames.add(object3.getName());
    objectNames.add(object5.getName());
    objectNames.add(object6.getName());

    is.read(bytes);
    ObjectMetadata objectMetadata = ObjectFileUtil.getObjectFromInputBuffer(3, 0, bytes,
            outBytes, false);
    assertThat(objectNames.contains(objectMetadata.getName()), is(true));
    is.read(bytes);
    objectMetadata = ObjectFileUtil.getObjectFromInputBuffer(3, 0, bytes,
            outBytes, false);
    assertThat(objectNames.contains(objectMetadata.getName()), is(true));

    is.read(bytes);
    objectMetadata = ObjectFileUtil.getObjectFromInputBuffer(3, 0, bytes,
            outBytes, false);
    assertThat(objectNames.contains(objectMetadata.getName()), is(true));

    is.read(bytes);
    objectMetadata = ObjectFileUtil.getObjectFromInputBuffer(3, 0, bytes,
            outBytes, false);
    assertThat(objectNames.contains(objectMetadata.getName()), is(true));

    assertThat(objectNames.contains(object1.getName()), is(false));
    assertThat(objectNames.contains(object4.getName()), is(false));

    is.close();


  }


  @Test
  public void shuffleTestV2V3VwithVersionMixed() throws IOException {
    final ObjectMetadata object1 = generateIdV2();
    final ObjectMetadata object2 = generateIdV2();
    final ObjectMetadata object3 = generateIdV2();
    OutputStream os = new FileOutputStream("id_0.object");
    ObjectFileUtil.writeObjectFileVersion(os, 2, 0);
    os.write(object1.toBytes(false));
    os.write(object2.toBytes(false));
    os.write(object3.toBytes(false));
    os.close();

    final ObjectMetadata object4 = generateIdV3WithVersion();
    final ObjectMetadata object5 = generateIdV3WithVersion();
    final ObjectMetadata object6 = generateIdV3WithVersion();
    os = new FileOutputStream("id_1.object");
    ObjectFileUtil.writeObjectFileVersion(os, 3, 0);
    ObjectFileUtil.writeObjectFileHeader(os, LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE);
    os.write(object4.toBytes(true));
    os.write(object5.toBytes(true));
    os.write(object6.toBytes(true));
    os.close();

    ObjectFile.shuffle("id_", ".", 4, -1);

    // check if the output file id_2.object exists
    File f = new File("id_2.object");
    assertThat(f.exists(), is(true));

    int size = ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH +
            4 * (LegacyObjectMetadata.OBJECT_SIZE + LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE);
    assertThat((int)f.length(), is(size));

    BufferedInputStream is = new BufferedInputStream(new FileInputStream(f));

    ObjectFileVersion version = ObjectFileUtil.readObjectFileVersion(is);
    ObjectFileHeader header = ObjectFileUtil.readObjectFileHeader(is);
    is.skip(ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH);
    byte[] bytes = ObjectFileUtil.allocateObjectBuffer(3, 0, header);
    byte[] outBytes = new byte[LegacyObjectMetadata.OBJECT_SIZE + LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE];

    List<String> objectNames = new ArrayList<String>();
    objectNames.add(object2.getName());
    objectNames.add(object3.getName());
    objectNames.add(object5.getName());
    objectNames.add(object6.getName());

    is.read(bytes);
    ObjectMetadata objectMetadata = ObjectFileUtil.getObjectFromInputBuffer(3, 0, bytes,
            outBytes, true);
    assertThat(objectNames.contains(objectMetadata.getName()), is(true));
    is.read(bytes);
    objectMetadata = ObjectFileUtil.getObjectFromInputBuffer(3, 0, bytes,
            outBytes, true);
    assertThat(objectNames.contains(objectMetadata.getName()), is(true));

    is.read(bytes);
    objectMetadata = ObjectFileUtil.getObjectFromInputBuffer(3, 0, bytes,
            outBytes, true);
    assertThat(objectNames.contains(objectMetadata.getName()), is(true));

    is.read(bytes);
    objectMetadata = ObjectFileUtil.getObjectFromInputBuffer(3, 0, bytes,
            outBytes, true);
    assertThat(objectNames.contains(objectMetadata.getName()), is(true));

    assertThat(objectNames.contains(object1.getName()), is(false));
    assertThat(objectNames.contains(object4.getName()), is(false));

    is.close();


  }

  @Test
  public void shuffleTestV3WithoutVersion() throws FileNotFoundException, IOException {
    final ObjectMetadata object1 = generateIdV3WithoutVersion();
    final ObjectMetadata object2 = generateIdV3WithoutVersion();
    final ObjectMetadata object3 = generateIdV3WithoutVersion();
    OutputStream os = new FileOutputStream("id_0.object");
    ObjectFileUtil.writeObjectFileVersion(os, 3, 0);
    ObjectFileUtil.writeObjectFileHeader(os, 0);
    os.write(object1.toBytes(false));
    os.write(object2.toBytes(false));
    os.write(object3.toBytes(false));
    os.close();

    final ObjectMetadata object4 = generateIdV3WithoutVersion();
    final ObjectMetadata object5 = generateIdV3WithoutVersion();
    final ObjectMetadata object6 = generateIdV3WithoutVersion();
    os = new FileOutputStream("id_1.object");
    ObjectFileUtil.writeObjectFileVersion(os, 3, 0);
    ObjectFileUtil.writeObjectFileHeader(os, 0);
    os.write(object4.toBytes(false));
    os.write(object5.toBytes(false));
    os.write(object6.toBytes(false));
    os.close();

    ObjectFile.shuffle("id_", ".", 4, -1);

    // check if the output file id_2.object exists
    File f = new File("id_2.object");
    assertThat(f.exists(), is(true));

    int size = ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH +
            4 * LegacyObjectMetadata.OBJECT_SIZE;
    assertThat((int)f.length(), is(size));

    BufferedInputStream is = new BufferedInputStream(new FileInputStream(f));

    ObjectFileVersion version = ObjectFileUtil.readObjectFileVersion(is);
    ObjectFileHeader header = ObjectFileUtil.readObjectFileHeader(is);
    is.skip(ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH);
    byte[] bytes = ObjectFileUtil.allocateObjectBuffer(3, 0, header);
    byte[] outBytes = new byte[LegacyObjectMetadata.OBJECT_SIZE];

    List<String> objectNames = new ArrayList<String>();
    objectNames.add(object2.getName());
    objectNames.add(object3.getName());
    objectNames.add(object5.getName());
    objectNames.add(object6.getName());

    is.read(bytes);
    ObjectMetadata objectMetadata = ObjectFileUtil.getObjectFromInputBuffer(3, 0, bytes,
            outBytes, false);
    assertThat(objectNames.contains(objectMetadata.getName()), is(true));
    is.read(bytes);
    objectMetadata = ObjectFileUtil.getObjectFromInputBuffer(3, 0, bytes,
            outBytes, false);
    assertThat(objectNames.contains(objectMetadata.getName()), is(true));

    is.read(bytes);
    objectMetadata = ObjectFileUtil.getObjectFromInputBuffer(3, 0, bytes,
            outBytes, false);
    assertThat(objectNames.contains(objectMetadata.getName()), is(true));

    is.read(bytes);
    objectMetadata = ObjectFileUtil.getObjectFromInputBuffer(3, 0, bytes,
            outBytes, false);
    assertThat(objectNames.contains(objectMetadata.getName()), is(true));

    assertThat(objectNames.contains(object1.getName()), is(false));
    assertThat(objectNames.contains(object4.getName()), is(false));

    is.close();
  }


  @Test
  public void shuffleTestV3WithVersion() throws FileNotFoundException, IOException {
    final ObjectMetadata object1 = generateIdV3WithVersion();
    final ObjectMetadata object2 = generateIdV3WithVersion();
    final ObjectMetadata object3 = generateIdV3WithVersion();
    OutputStream os = new FileOutputStream("id_0.object");
    ObjectFileUtil.writeObjectFileVersion(os, 3, 0);
    ObjectFileUtil.writeObjectFileHeader(os, LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE);
    os.write(object1.toBytes(true));
    os.write(object2.toBytes(true));
    os.write(object3.toBytes(true));
    os.close();

    final ObjectMetadata object4 = generateIdV3WithVersion();
    final ObjectMetadata object5 = generateIdV3WithVersion();
    final ObjectMetadata object6 = generateIdV3WithVersion();
    os = new FileOutputStream("id_1.object");
    ObjectFileUtil.writeObjectFileVersion(os, 3, 0);
    ObjectFileUtil.writeObjectFileHeader(os, LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE);
    os.write(object4.toBytes(true));
    os.write(object5.toBytes(true));
    os.write(object6.toBytes(true));
    os.close();

    ObjectFile.shuffle("id_", ".", 4, -1);

    // check if the output file id_2.object exists
    File f = new File("id_2.object");
    assertThat(f.exists(), is(true));

    int size = ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH +
            4 * (LegacyObjectMetadata.OBJECT_SIZE + LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE);
    assertThat((int)f.length(), is(size));

    BufferedInputStream is = new BufferedInputStream(new FileInputStream(f));

    ObjectFileVersion version = ObjectFileUtil.readObjectFileVersion(is);
    ObjectFileHeader header = ObjectFileUtil.readObjectFileHeader(is);
    is.skip(ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH);
    byte[] bytes = ObjectFileUtil.allocateObjectBuffer(3, 0, header);
    byte[] outBytes = new byte[LegacyObjectMetadata.OBJECT_SIZE + LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE];

    List<String> objectNames = new ArrayList<String>();
    objectNames.add(object2.getName());
    objectNames.add(object3.getName());
    objectNames.add(object5.getName());
    objectNames.add(object6.getName());

    is.read(bytes);
    ObjectMetadata objectMetadata = ObjectFileUtil.getObjectFromInputBuffer(3, 0, bytes,
            outBytes, true);
    assertThat(objectNames.contains(objectMetadata.getName()), is(true));
    is.read(bytes);
    objectMetadata = ObjectFileUtil.getObjectFromInputBuffer(3, 0, bytes,
            outBytes, true);
    assertThat(objectNames.contains(objectMetadata.getName()), is(true));

    is.read(bytes);
    objectMetadata = ObjectFileUtil.getObjectFromInputBuffer(3, 0, bytes,
            outBytes, true);
    assertThat(objectNames.contains(objectMetadata.getName()), is(true));

    is.read(bytes);
    objectMetadata = ObjectFileUtil.getObjectFromInputBuffer(3, 0, bytes,
            outBytes, true);
    assertThat(objectNames.contains(objectMetadata.getName()), is(true));

    assertThat(objectNames.contains(object1.getName()), is(false));
    assertThat(objectNames.contains(object4.getName()), is(false));

    is.close();
  }


  @Test
  public void shuffleTestV3WithoutandWithVersion() throws FileNotFoundException, IOException {
    final ObjectMetadata object1 = generateIdV3WithoutVersion();
    final ObjectMetadata object2 = generateIdV3WithoutVersion();
    final ObjectMetadata object3 = generateIdV3WithoutVersion();
    OutputStream os = new FileOutputStream("id_0.object");
    ObjectFileUtil.writeObjectFileVersion(os, 3, 0);
    ObjectFileUtil.writeObjectFileHeader(os, 0);
    os.write(object1.toBytes(false));
    os.write(object2.toBytes(false));
    os.write(object3.toBytes(false));
    os.close();

    final ObjectMetadata object4 = generateIdV3WithVersion();
    final ObjectMetadata object5 = generateIdV3WithVersion();
    final ObjectMetadata object6 = generateIdV3WithVersion();
    os = new FileOutputStream("id_1.object");
    ObjectFileUtil.writeObjectFileVersion(os, 3, 0);
    ObjectFileUtil.writeObjectFileHeader(os, LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE);
    os.write(object4.toBytes(true));
    os.write(object5.toBytes(true));
    os.write(object6.toBytes(true));
    os.close();

    ObjectFile.shuffle("id_", ".", 4, -1);

    // check if the output file id_2.object exists
    File f = new File("id_2.object");
    assertThat(f.exists(), is(true));

    int size = ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH +
            4 * (LegacyObjectMetadata.OBJECT_SIZE + LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE);
    assertThat((int)f.length(), is(size));

    BufferedInputStream is = new BufferedInputStream(new FileInputStream(f));

    ObjectFileVersion version = ObjectFileUtil.readObjectFileVersion(is);
    ObjectFileHeader header = ObjectFileUtil.readObjectFileHeader(is);
    is.skip(ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH);
    byte[] bytes = ObjectFileUtil.allocateObjectBuffer(3, 0, header);
    byte[] outBytes = new byte[LegacyObjectMetadata.OBJECT_SIZE + LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE];

    List<String> objectNames = new ArrayList<String>();
    objectNames.add(object2.getName());
    objectNames.add(object3.getName());
    objectNames.add(object5.getName());
    objectNames.add(object6.getName());

    is.read(bytes);
    ObjectMetadata objectMetadata = ObjectFileUtil.getObjectFromInputBuffer(3, 0, bytes,
            outBytes, true);
    assertThat(objectNames.contains(objectMetadata.getName()), is(true));
    is.read(bytes);
    objectMetadata = ObjectFileUtil.getObjectFromInputBuffer(3, 0, bytes,
            outBytes, true);
    assertThat(objectNames.contains(objectMetadata.getName()), is(true));

    is.read(bytes);
    objectMetadata = ObjectFileUtil.getObjectFromInputBuffer(3, 0, bytes,
            outBytes, true);
    assertThat(objectNames.contains(objectMetadata.getName()), is(true));

    is.read(bytes);
    objectMetadata = ObjectFileUtil.getObjectFromInputBuffer(3, 0, bytes,
            outBytes, true);
    assertThat(objectNames.contains(objectMetadata.getName()), is(true));

    assertThat(objectNames.contains(object1.getName()), is(false));
    assertThat(objectNames.contains(object4.getName()), is(false));

    is.close();
  }


  @Test
  public void shuffleTestV3WithAndWithoutVersion() throws FileNotFoundException, IOException {

    final ObjectMetadata object1 = generateIdV3WithVersion();
    final ObjectMetadata object2 = generateIdV3WithVersion();
    final ObjectMetadata object3 = generateIdV3WithVersion();
    OutputStream os = new FileOutputStream("id_0.object");
    ObjectFileUtil.writeObjectFileVersion(os, 3, 0);
    ObjectFileUtil.writeObjectFileHeader(os, LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE);
    os.write(object1.toBytes(true));
    os.write(object2.toBytes(true));
    os.write(object3.toBytes(true));
    os.close();

    final ObjectMetadata object4 = generateIdV3WithoutVersion();
    final ObjectMetadata object5 = generateIdV3WithoutVersion();
    final ObjectMetadata object6 = generateIdV3WithoutVersion();
    os = new FileOutputStream("id_1.object");
    ObjectFileUtil.writeObjectFileVersion(os, 3, 0);
    ObjectFileUtil.writeObjectFileHeader(os, 0);
    os.write(object4.toBytes(false));
    os.write(object5.toBytes(false));
    os.write(object6.toBytes(false));
    os.close();


    ObjectFile.shuffle("id_", ".", 4, -1);

    // check if the output file id_2.object exists
    File f = new File("id_2.object");
    assertThat(f.exists(), is(true));

    int size = ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH +
            4 * (LegacyObjectMetadata.OBJECT_SIZE + LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE);
    assertThat((int)f.length(), is(size));

    BufferedInputStream is = new BufferedInputStream(new FileInputStream(f));

    ObjectFileVersion version = ObjectFileUtil.readObjectFileVersion(is);
    ObjectFileHeader header = ObjectFileUtil.readObjectFileHeader(is);
    is.skip(ObjectFileVersion.VERSION_HEADER_LENGTH + ObjectFileHeader.HEADER_LENGTH);
    byte[] bytes = ObjectFileUtil.allocateObjectBuffer(3, 0, header);
    byte[] outBytes = new byte[LegacyObjectMetadata.OBJECT_SIZE + LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE];

    List<String> objectNames = new ArrayList<String>();
    objectNames.add(object2.getName());
    objectNames.add(object3.getName());
    objectNames.add(object5.getName());
    objectNames.add(object6.getName());

    is.read(bytes);
    ObjectMetadata objectMetadata = ObjectFileUtil.getObjectFromInputBuffer(3, 0, bytes,
            outBytes, true);
    assertThat(objectNames.contains(objectMetadata.getName()), is(true));
    is.read(bytes);
    objectMetadata = ObjectFileUtil.getObjectFromInputBuffer(3, 0, bytes,
            outBytes, true);
    assertThat(objectNames.contains(objectMetadata.getName()), is(true));

    is.read(bytes);
    objectMetadata = ObjectFileUtil.getObjectFromInputBuffer(3, 0, bytes,
            outBytes, true);
    assertThat(objectNames.contains(objectMetadata.getName()), is(true));

    is.read(bytes);
    objectMetadata = ObjectFileUtil.getObjectFromInputBuffer(3, 0, bytes,
            outBytes, true);
    assertThat(objectNames.contains(objectMetadata.getName()), is(true));

    assertThat(objectNames.contains(object1.getName()), is(false));
    assertThat(objectNames.contains(object4.getName()), is(false));

    is.close();
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
    new ObjectFile.ObjectFileOutputStream(prefix, maxObjects, suffix, false, true);
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
    final OutputStream out = new ObjectFile.ObjectFileOutputStream(prefixFilename, maxObjects, suffix, true, true);
    final ObjectMetadata o = LegacyObjectMetadata
        .fromMetadata(UUID.randomUUID().toString().replace("-", "") + "0000",
                0, 0, (byte)-1, -1, null);

    for (int i = 0; i < numObjects; i++) {
      out.write(o.toBytes(false));
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
