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
import org.junit.runner.RunWith;

import com.cleversafe.og.object.LegacyObjectName;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class ObjectFileTest
{
   @Rule
   public ExpectedException thrown = ExpectedException.none();
   private File nonExistent;
   private File exists;

   @Before
   public void before()
   {
      this.nonExistent = new File("/nonexistent");
      this.exists = new File(Application.getResource("objectfile.jsap"));
   }

   @Test
   public void getInputStreamNullInput() throws FileNotFoundException
   {
      final InputStream in = ObjectFile.getInputStream(null);
      assertThat(in, is(System.in));
   }

   @Test(expected = FileNotFoundException.class)
   public void getInputStreamMissingInput() throws FileNotFoundException
   {
      ObjectFile.getInputStream(this.nonExistent);
   }

   @Test
   public void getInputStream() throws FileNotFoundException
   {
      final InputStream in = ObjectFile.getInputStream(this.exists);
      assertThat(in, is(not(System.in)));
   }

   @Test
   public void getOutputStreamNullOutput() throws FileNotFoundException
   {
      final OutputStream out = ObjectFile.getOutputStream(null);
      assertThat(out, is((OutputStream) System.out));
   }

   @Test(expected = FileNotFoundException.class)
   public void getOutputStreamMissingOutput() throws FileNotFoundException
   {
      ObjectFile.getOutputStream(this.nonExistent);
   }

   @Test
   public void getOutputStream() throws FileNotFoundException
   {
      final OutputStream out = ObjectFile.getOutputStream(this.exists);
      assertThat(out, is(not((OutputStream) System.out)));
   }

   @DataProvider
   public static Object[][] provideInvalidStreams()
   {
      return new Object[][]{
            {null, new ByteArrayOutputStream()},
            {new ByteArrayInputStream(new byte[]{}), null}
      };
   }

   @Test
   @UseDataProvider("provideInvalidStreams")
   public void invalidWrite(final InputStream in, final OutputStream out) throws IOException
   {
      this.thrown.expect(NullPointerException.class);
      ObjectFile.write(in, out);
   }

   @Test
   public void write() throws IOException
   {
      final String objectName = LegacyObjectName.forUUID(UUID.randomUUID()).toString();
      final InputStream in = new ByteArrayInputStream(objectName.getBytes());
      final ByteArrayOutputStream out = new ByteArrayOutputStream(18);
      ObjectFile.write(in, out);
      assertThat(objectName, is(LegacyObjectName.forBytes(out.toByteArray()).toString()));
   }

   @Test
   @UseDataProvider("provideInvalidStreams")
   public void invalidRead(final InputStream in, final OutputStream out) throws IOException
   {
      this.thrown.expect(NullPointerException.class);
      ObjectFile.read(in, out);
   }

   @Test
   public void read() throws IOException
   {
      final LegacyObjectName object = LegacyObjectName.forUUID(UUID.randomUUID());
      final InputStream in = new ByteArrayInputStream(object.toBytes());
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      ObjectFile.read(in, out);
      assertThat(new String(out.toByteArray()), is(object.toString() + "\n"));
   }
}
