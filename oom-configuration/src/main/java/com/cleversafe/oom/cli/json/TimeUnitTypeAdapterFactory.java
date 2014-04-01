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
// Date: Apr 1, 2014
// ---------------------

package com.cleversafe.oom.cli.json;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.oom.util.Units;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class TimeUnitTypeAdapterFactory implements TypeAdapterFactory
{
   private static Logger _logger = LoggerFactory.getLogger(TimeUnitTypeAdapterFactory.class);

   public TimeUnitTypeAdapterFactory()
   {}

   @Override
   public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type)
   {
      @SuppressWarnings("unchecked")
      final Class<T> rawType = (Class<T>) type.getRawType();
      if (rawType.equals(TimeUnit.class) || parentEquals(rawType, TimeUnit.class))
      {
         return new TypeAdapter<T>()
         {

            @Override
            public void write(final JsonWriter out, final Object value) throws IOException
            {
               if (value != null)
                  out.value(value.toString().toLowerCase());
            }

            @SuppressWarnings("unchecked")
            @Override
            public T read(final JsonReader in) throws IOException
            {
               return (T) Units.time(in.nextString());
            }
         };
      }
      return null;
   }

   private boolean parentEquals(final Class<?> rawType, final Class<?> compare)
   {
      final Class<?> parent = rawType.getSuperclass();
      return parent != null && parent.equals(compare);
   }
}
