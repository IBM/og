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
import java.util.ArrayList;
import java.util.List;

import com.cleversafe.og.json.FilesizeConfig;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class FilesizeConfigListTypeAdapterFactory implements TypeAdapterFactory
{
   private final TypeToken<List<FilesizeConfig>> matchingType;

   public FilesizeConfigListTypeAdapterFactory()
   {
      this.matchingType = new TypeToken<List<FilesizeConfig>>()
      {};
   }

   @Override
   public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type)
   {
      if (!this.matchingType.equals(type))
         return null;

      final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
      final TypeAdapter<FilesizeConfig> filesizeTypeAdapter = gson.getAdapter(FilesizeConfig.class);

      return new TypeAdapter<T>()
      {
         @Override
         public void write(final JsonWriter out, final T value) throws IOException
         {
            delegate.write(out, value);
         }

         @Override
         @SuppressWarnings("unchecked")
         public T read(final JsonReader in) throws IOException
         {
            final List<FilesizeConfig> filesize = new ArrayList<FilesizeConfig>(1);

            if (JsonToken.NUMBER == in.peek())
            {
               filesize.add(new FilesizeConfig(in.nextDouble()));
               return (T) filesize;
            }

            if (JsonToken.BEGIN_OBJECT == in.peek())
            {
               filesize.add(filesizeTypeAdapter.read(in));
               return (T) filesize;
            }

            return delegate.read(in);
         }
      }.nullSafe();
   }
}
