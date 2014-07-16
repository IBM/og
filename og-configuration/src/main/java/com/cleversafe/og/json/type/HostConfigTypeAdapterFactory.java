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
// Date: Jul 16, 2014
// ---------------------

package com.cleversafe.og.json.type;

import java.io.IOException;

import com.cleversafe.og.json.HostConfig;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class HostConfigTypeAdapterFactory implements TypeAdapterFactory
{
   public HostConfigTypeAdapterFactory()
   {}

   @Override
   @SuppressWarnings("unchecked")
   public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type)
   {
      final Class<T> rawType = (Class<T>) type.getRawType();
      if (!HostConfig.class.equals(rawType))
         return null;

      final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);

      return new TypeAdapter<T>()
      {
         @Override
         public void write(final JsonWriter out, final T value) throws IOException
         {
            delegate.write(out, value);
         }

         @Override
         public T read(final JsonReader in) throws IOException
         {
            if (JsonToken.STRING == in.peek())
               return (T) new HostConfig(in.nextString());

            return delegate.read(in);
         }

      }.nullSafe();
   }
}
