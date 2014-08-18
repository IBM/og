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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.json.DistributionType;
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
   public void nonEnumCreate()
   {
      assertThat(this.typeAdapterFactory.create(this.gson, TypeToken.get(String.class)),
            nullValue());
   }

   @Test
   public void isEnum() throws IOException
   {
      final TypeAdapter<DistributionType> typeAdapter =
            this.typeAdapterFactory.create(this.gson, TypeToken.get(DistributionType.class));

      assertThat(typeAdapter, notNullValue());

      typeAdapter.write(this.writer, DistributionType.NORMAL);
      verify(this.writer).value("normal");

      when(this.reader.nextString()).thenReturn("NormAL");
      assertThat(typeAdapter.read(this.reader), is(DistributionType.NORMAL));
   }

   @Test(expected = JsonSyntaxException.class)
   public void nonEnumRead() throws IOException
   {
      final TypeAdapter<DistributionType> typeAdapter =
            this.typeAdapterFactory.create(this.gson, TypeToken.get(DistributionType.class));

      when(this.reader.nextString()).thenReturn("fakeDistribution");
      typeAdapter.read(this.reader);
   }
}
