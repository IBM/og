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
// Date: Jul 11, 2014
// ---------------------

package com.cleversafe.og.json.type;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.json.DistributionType;
import com.cleversafe.og.util.SizeUnit;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class CaseInsensitiveEnumTypeAdapterFactoryTest
{
   private CaseInsensitiveEnumTypeAdapterFactory typeAdapterFactory;
   private Gson gson;
   private JsonWriter writer;
   private JsonReader reader;

   @Before
   public void before()
   {
      this.typeAdapterFactory = new CaseInsensitiveEnumTypeAdapterFactory();
      this.gson = new GsonBuilder().create();
      this.writer = mock(JsonWriter.class);
      this.reader = mock(JsonReader.class);
   }

   @Test
   public void testNonEnum()
   {
      Assert.assertNull(this.typeAdapterFactory.create(this.gson, TypeToken.get(String.class)));
   }

   @Test
   public void testEnum() throws IOException
   {
      final TypeAdapter<DistributionType> typeAdapter =
            this.typeAdapterFactory.create(this.gson, TypeToken.get(DistributionType.class));

      Assert.assertNotNull(typeAdapter);

      typeAdapter.write(this.writer, DistributionType.NORMAL);
      verify(this.writer).value("normal");

      when(this.reader.nextString()).thenReturn("NormAL");
      Assert.assertEquals(DistributionType.NORMAL, typeAdapter.read(this.reader));
   }

   @Test(expected = JsonSyntaxException.class)
   public void testEnumReadFailure() throws IOException
   {
      final TypeAdapter<DistributionType> typeAdapter =
            this.typeAdapterFactory.create(this.gson, TypeToken.get(DistributionType.class));

      when(this.reader.nextString()).thenReturn("fakeDistribution");
      typeAdapter.read(this.reader);
   }

   @Test
   public void testSizeUnit() throws IOException
   {
      final TypeAdapter<SizeUnit> typeAdapter =
            this.typeAdapterFactory.create(this.gson, TypeToken.get(SizeUnit.class));

      Assert.assertNotNull(typeAdapter);

      typeAdapter.write(this.writer, SizeUnit.BYTES);
      verify(this.writer).value("bytes");

      when(this.reader.nextString()).thenReturn("ByteS");
      Assert.assertEquals(SizeUnit.BYTES, typeAdapter.read(this.reader));
   }

   @Test
   public void testTimeUnit() throws IOException
   {
      final TypeAdapter<TimeUnit> typeAdapter =
            this.typeAdapterFactory.create(this.gson, TypeToken.get(TimeUnit.class));

      Assert.assertNotNull(typeAdapter);

      typeAdapter.write(this.writer, TimeUnit.DAYS);
      verify(this.writer).value("days");

      when(this.reader.nextString()).thenReturn("DayS");
      Assert.assertEquals(TimeUnit.DAYS, typeAdapter.read(this.reader));
   }
}
