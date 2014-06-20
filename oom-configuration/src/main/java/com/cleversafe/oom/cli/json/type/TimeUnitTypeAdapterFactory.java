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

package com.cleversafe.oom.cli.json.type;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.cleversafe.oom.util.Units;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

// This factory is fragile - TimeUnit is not an ordinary enum in the sense
// that TimeUnit instances (eg TimeUnit.NANOSECONDS) are TimeUnit subtypes.
// This contrasts with a basic enum where the instance and the class are
// interchangeable
public class TimeUnitTypeAdapterFactory implements TypeAdapterFactory
{

   public TimeUnitTypeAdapterFactory()
   {}

   @Override
   public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type)
   {
      @SuppressWarnings("unchecked")
      final Class<T> rawType = (Class<T>) type.getRawType();
      // use this type adapter for the TimeUnit type and all of its elements (subtypes)
      if (rawType.equals(TimeUnit.class) || parentEquals(rawType, TimeUnit.class))
      {
         return new TypeAdapter<T>()
         {

            @Override
            public void write(final JsonWriter out, final Object value) throws IOException
            {
               if (value != null)
                  out.value(value.toString().toLowerCase());
               else
                  out.nullValue();
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
