//
// Copyright (C) 2005-2011 Cleversafe, Inc. All rights reserved.
//
// Contact Information:
// Cleversafe, Inc.
// 222 South Riverside Plaza
// Suite 1700
// Chicago, IL 60606, USA
//
// licensing@cleversafe.com
//
// END-OF-HEADER
//
// -----------------------
// @author: rveitch
//
// Date: Aug 22, 2014
// ---------------------

package com.cleversafe.og.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import com.cleversafe.og.object.LegacyObjectMetadata;
import com.cleversafe.og.object.ObjectMetadata;
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
    assertThat(in, is(System.in));
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

  @Test(expected = FileNotFoundException.class)
  public void getOutputStreamMissingOutput() throws FileNotFoundException {
    ObjectFile.getOutputStream(this.nonExistent);
  }

  @Test
  public void getOutputStream() throws FileNotFoundException {
    final OutputStream out = ObjectFile.getOutputStream(this.exists);
    assertThat(out, is(not((OutputStream) System.out)));
  }

  @DataProvider
  public static Object[][] provideInvalidStreams() {
    return new Object[][] { {null, new ByteArrayOutputStream()},
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
    final String objectMetadata = UUID.randomUUID().toString().replace("-", "") + "0000,0";
    final InputStream in = new ByteArrayInputStream(objectMetadata.getBytes());
    final ByteArrayOutputStream out = new ByteArrayOutputStream(LegacyObjectMetadata.OBJECT_SIZE);
    ObjectFile.write(in, out);
    ObjectMetadata object = LegacyObjectMetadata.fromBytes(out.toByteArray());
    assertThat(objectMetadata, is(String.format("%s,%s", object.getName(), object.getSize())));
  }

  @Test
  @UseDataProvider("provideInvalidStreams")
  public void invalidRead(final InputStream in, final OutputStream out) throws IOException {
    this.thrown.expect(NullPointerException.class);
    ObjectFile.read(in, out);
  }

  @Test
  public void read() throws IOException {
    String objectString = UUID.randomUUID().toString().replace("-", "") + "0000";
    final LegacyObjectMetadata object = LegacyObjectMetadata.fromMetadata(objectString, 1024);
    final InputStream in = new ByteArrayInputStream(object.toBytes());
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectFile.read(in, out);
    assertThat(new String(out.toByteArray()),
        is(String.format("%s,%s%n", object.getName(), object.getSize())));
  }

  @DataProvider
  public static Object[][] provideInvalidFilter() {
    InputStream in = new ByteArrayInputStream(new byte[] {});
    OutputStream out = new ByteArrayOutputStream();
    return new Object[][] { {null, out, 0, 0, NullPointerException.class},
        {in, null, 0, 0, NullPointerException.class},
        {in, out, -1, 0, IllegalArgumentException.class},
        {in, out, 0, -1, IllegalArgumentException.class},
        {in, out, 10, 9, IllegalArgumentException.class}};
  }

  @Test
  @UseDataProvider("provideInvalidFilter")
  public void invalidFilter(final InputStream in, final OutputStream out, long minFilesize,
      long maxFilesize, Class<Exception> expectedException) throws IOException {
    this.thrown.expect(expectedException);
    ObjectFile.filter(in, out, minFilesize, maxFilesize);
  }

  @Test
  public void filter() throws IOException {
    String s = UUID.randomUUID().toString().replace("-", "") + "0000";
    ObjectMetadata o1 = LegacyObjectMetadata.fromMetadata(s, 1);
    ObjectMetadata o2 = LegacyObjectMetadata.fromMetadata(s, 2);
    ObjectMetadata o3 = LegacyObjectMetadata.fromMetadata(s, 3);
    ByteArrayOutputStream source = new ByteArrayOutputStream();
    source.write(o1.toBytes());
    source.write(o2.toBytes());
    source.write(o3.toBytes());

    ByteArrayInputStream in = new ByteArrayInputStream(source.toByteArray());
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectFile.filter(in, out, 2, 2);

    assertThat(out.size(), is(LegacyObjectMetadata.OBJECT_SIZE));
    ObjectMetadata filtered = LegacyObjectMetadata.fromBytes(out.toByteArray());
    assertThat(filtered.getName(), is(s));
    assertThat(filtered.getSize(), is(2L));
  }
}
