/* Copyright (c) IBM Corporation 2020. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.object;

import com.google.common.io.BaseEncoding;
import com.ibm.og.util.ObjectManagerUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertArrayEquals;

public class ObjectFileUtilTest {
  private static final Logger _logger = LoggerFactory.getLogger(ObjectFileUtilTest.class);
  final String dirName = "ObjectFileUtilTest";
  static final String prefix = "id_";
  static final String suffix = ".object";
  final protected UUID vaultId = new UUID(0, 0);


  protected File[] getIdFiles(String prefix, String suffix) {
    final File dir = new File(".");
    final File[] files = dir.listFiles(new ObjectManagerUtils.IdFilter(prefix, suffix));
    return files;
  }

  @Before
  public void copyIdFiles() {
    final File[] files = getIdFiles(this.prefix, this.suffix);
    final File directory = new File(this.dirName);
    if (!(directory.exists() && directory.isDirectory())) {
      directory.mkdir();
    }
    for (final File id : files) {
      id.renameTo(new File(directory, id.getName()));
    }
  }

  private void deleteTempFiles() {
    File[] files = getIdFiles(this.prefix, suffix);
    for (final File id : files) {
      id.delete();
    }

    files = getIdFiles(this.prefix, ".tmp");
    for (final File id : files) {
      id.delete();
    }

  }

  @After
  public void restoreIdFiles() {
    deleteTempFiles();
    final File directory = new File(this.dirName);
    final File[] files = directory.listFiles();
    final File parentDir = directory.getParentFile();
    for (final File id : files) {
      id.renameTo(new File(parentDir, id.getName()));
    }
    directory.delete();
  }

  @Test
  public void objectFileHeaderFromMetadata() {
    final UUID objectName = UUID.randomUUID();
    final String objectString = objectString(objectName);
    final UUID objectVersion = UUID.randomUUID();
    final String objectVersionString = objectVersionString(objectVersion);


    final long objectSize = Long.MAX_VALUE;
    final int containerSuffix = Integer.MAX_VALUE;
    final int legalHolds = 10;
    final int retention = 3600;

    byte[] expectedBytes = new byte[ObjectFileHeader.HEADER_LENGTH];
    ByteBuffer eb = ByteBuffer.wrap(expectedBytes);
    eb.put((byte)ObjectFileHeader.HEADER_LENGTH);
    eb.put((byte)objectString.getBytes().length);
    eb.put((byte)objectVersionString.getBytes().length);
    eb.put((byte)LegacyObjectMetadata.OBJECT_SIZE_SIZE);
    eb.put((byte)LegacyObjectMetadata.OBJECT_SUFFIX_SIZE);
    eb.put((byte)LegacyObjectMetadata.OBJECT_LEGAL_HOLDS_SIZE);
    eb.put((byte)LegacyObjectMetadata.OBJECT_RETENTION_SIZE);

    ObjectFileHeader header = ObjectFileHeader.fromMetadata(objectString.getBytes().length, objectVersionString.getBytes().length,
            LegacyObjectMetadata.OBJECT_SIZE_SIZE, LegacyObjectMetadata.OBJECT_SUFFIX_SIZE,
            LegacyObjectMetadata.OBJECT_LEGAL_HOLDS_SIZE, LegacyObjectMetadata.OBJECT_RETENTION_SIZE);

    assertArrayEquals(expectedBytes, header.getBytes())
    ;

  }


  @Test
  public void testUpgradeV3WithoutVersion() throws IOException {
   RandomObjectPopulator rop =
            new RandomObjectPopulator(this.vaultId, RandomObjectPopulatorTest.MAX_OBJECTS);
    for (int i=0; i<RandomObjectPopulatorTest.MAX_OBJECTS; i++) {
      rop.add(Util.generateIdV3WithoutVersion());
    }
    rop.shutdown();
    // filesize before upgrading
    File beforeUpgradeFile = new File("id_0.object");
    long size1 = beforeUpgradeFile.length();
    long lastModified1 = beforeUpgradeFile.length();

    ObjectFileUtil.upgrade(new File("id_0.object"), true);
    File afterUpgradeFile = new File("id_0.object");
    long size2 = afterUpgradeFile.length();
    long lastModified2 = afterUpgradeFile.length();

    Assert.assertTrue(size2 == (size1 + (LegacyObjectMetadata.OBJECT_VERSION_MAX_SIZE * RandomObjectPopulatorTest.MAX_OBJECTS)));
    Assert.assertTrue(lastModified1 != lastModified2);
  }

  @Test
  public void testUpgradeV3WithVersion() throws IOException {
    RandomObjectPopulator rop =
            new RandomObjectPopulator(this.vaultId, RandomObjectPopulatorTest.MAX_OBJECTS);
    for (int i=0; i<RandomObjectPopulatorTest.MAX_OBJECTS; i++) {
      rop.add(Util.generateIdV3WithVersion());
    }
    rop.shutdown();
    // filesize before upgrading
    File beforeUpgradeFile = new File("id_0.object");
    long size1 = beforeUpgradeFile.length();
    long lastModified1 = beforeUpgradeFile.lastModified();

    ObjectFileUtil.upgrade(new File("id_0.object"), true);
    long size2 = new File("id_0.object").length();
    long lastModified2 = beforeUpgradeFile.lastModified();

    Assert.assertTrue(size2 == size1);
    Assert.assertTrue(lastModified1 == lastModified2);
  }


  @Test
  public void testUpgradeV2WithVersion() throws IOException {
    final OutputStream out = new BufferedOutputStream(new FileOutputStream( "id_0.object"));
    ObjectFileVersion version = ObjectFileVersion.fromMetadata((byte)2, (byte)0);
    out.write(version.getBytes());
    final BaseEncoding ENCODING = BaseEncoding.base16().lowerCase();
    for (int i = 0; i < 4; i++) {
      ByteBuffer objectBuffer = ByteBuffer.allocate(35);
      String objectId = UUID.randomUUID().toString().replace("-", "") + "0000";
      objectBuffer.put(ENCODING.decode(objectId));
      objectBuffer.putLong(99L);
      objectBuffer.putInt(5000);
      objectBuffer.put((byte)0);
      objectBuffer.putInt((byte)-1);

      out.write(objectBuffer.array());
    }
    out.close();
    // filesize before upgrading
    long size1 = new File("id_0.object").length();

    ObjectFileUtil.upgrade(new File("id_0.object"), true);
    long size2 = new File("id_0.object").length();

    Assert.assertTrue(size2 == size1 + (ObjectFileHeader.HEADER_LENGTH + 4*16));
  }

  @Test
  public void testUpgradeV2WithoutVersion() throws IOException {
    final OutputStream out = new BufferedOutputStream(new FileOutputStream( "id_0.object"));
    ObjectFileVersion version = ObjectFileVersion.fromMetadata((byte)2, (byte)0);
    out.write(version.getBytes());
    final BaseEncoding ENCODING = BaseEncoding.base16().lowerCase();
    for (int i = 0; i < 4; i++) {
      ByteBuffer objectBuffer = ByteBuffer.allocate(35);
      String objectId = UUID.randomUUID().toString().replace("-", "") + "0000";
      objectBuffer.put(ENCODING.decode(objectId));
      objectBuffer.putLong(99L);
      objectBuffer.putInt(5000);
      objectBuffer.put((byte)0);
      objectBuffer.putInt((byte)-1);

      out.write(objectBuffer.array());
    }
    out.close();
    // filesize before upgrading
    long size1 = new File("id_0.object").length();

    ObjectFileUtil.upgrade(new File("id_0.object"), false);
    long size2 = new File("id_0.object").length();

    Assert.assertTrue(size2 == size1 + (ObjectFileHeader.HEADER_LENGTH));
  }

  @Test
  public void testUpgradeV1WithVersion() throws IOException {
    final OutputStream out = new BufferedOutputStream(new FileOutputStream( "id_0.object"));
    final BaseEncoding ENCODING = BaseEncoding.base16().lowerCase();
    for (int i = 0; i < 4; i++) {
      ByteBuffer objectBuffer = ByteBuffer.allocate(30);
      String objectId = UUID.randomUUID().toString().replace("-", "") + "0000";
      objectBuffer.put(ENCODING.decode(objectId));
      objectBuffer.putLong(99L);
      objectBuffer.putInt(5000);
      out.write(objectBuffer.array());
    }
    out.close();
    // filesize before upgrading
    long size1 = new File("id_0.object").length();

    ObjectFileUtil.upgrade(new File("id_0.object"), true);
    long size2 = new File("id_0.object").length();

    Assert.assertTrue(size2 == size1 + ( ObjectFileVersion.VERSION_HEADER_LENGTH +
            ObjectFileHeader.HEADER_LENGTH +4*(16+1+4)));
  }


  @Test
  public void testUpgradeV1WithoutVersion() throws IOException {
    final OutputStream out = new BufferedOutputStream(new FileOutputStream( "id_0.object"));
    final BaseEncoding ENCODING = BaseEncoding.base16().lowerCase();
    for (int i = 0; i < 4; i++) {
      ByteBuffer objectBuffer = ByteBuffer.allocate(30);
      String objectId = UUID.randomUUID().toString().replace("-", "") + "0000";
      objectBuffer.put(ENCODING.decode(objectId));
      objectBuffer.putLong(99L);
      objectBuffer.putInt(5000);
      out.write(objectBuffer.array());
    }
    out.close();
    // filesize before upgrading
    long size1 = new File("id_0.object").length();

    ObjectFileUtil.upgrade(new File("id_0.object"), false);
    long size2 = new File("id_0.object").length();

    Assert.assertTrue(size2 == size1 + ( ObjectFileVersion.VERSION_HEADER_LENGTH +
            ObjectFileHeader.HEADER_LENGTH +4*(1+4)));
  }


  @Test
  public void testNoUpgradeScenarioWithNoVersion() throws IOException {
    final OutputStream out = new BufferedOutputStream(new FileOutputStream( "id_0.object"));
    final BaseEncoding ENCODING = BaseEncoding.base16().lowerCase();
    for (int i = 0; i < 4; i++) {
      ByteBuffer objectBuffer = ByteBuffer.allocate(30);
      String objectId = UUID.randomUUID().toString().replace("-", "") + "0000";
      objectBuffer.put(ENCODING.decode(objectId));
      objectBuffer.putLong(99L);
      objectBuffer.putInt(5000);
      out.write(objectBuffer.array());
    }
    out.close();
    // filesize before upgrading
    File beforeUpgradeFile =  new File("id_0.object");
    long size1 = beforeUpgradeFile.length();
    ObjectFileUtil.upgrade(new File("id_0.object"), false);
    long size2 = new File("id_0.object").length();
    Assert.assertTrue(size2 == size1 + ( ObjectFileVersion.VERSION_HEADER_LENGTH +
            ObjectFileHeader.HEADER_LENGTH +4*(1+4)));

  }


  @Test
  public void testNoUpgradeScenarioWithVersion() throws IOException {
    final OutputStream out = new BufferedOutputStream(new FileOutputStream( "id_0.object"));
    final BaseEncoding ENCODING = BaseEncoding.base16().lowerCase();
    for (int i = 0; i < 4; i++) {
      ByteBuffer objectBuffer = ByteBuffer.allocate(30);
      String objectId = UUID.randomUUID().toString().replace("-", "") + "0000";

      objectBuffer.put(ENCODING.decode(objectId));
      objectBuffer.putLong(99L);
      objectBuffer.putInt(5000);
      out.write(objectBuffer.array());
    }
    out.close();
    // filesize before upgrading
    File beforeUpgradeFile =  new File("id_0.object");
    long size1 = beforeUpgradeFile.length();
    ObjectFileUtil.upgrade(new File("id_0.object"), false);
    long size2 = new File("id_0.object").length();
    Assert.assertTrue(size2 == size1 + ( ObjectFileVersion.VERSION_HEADER_LENGTH +
            ObjectFileHeader.HEADER_LENGTH +4*(1+4)));

  }

  private String objectVersionString(final UUID objectVersion) {
    return objectVersion.toString().replace("-", "");
  }

  private String objectString(final UUID objectName) {
    return objectName.toString().replace("-", "") + "0000";
  }
}
