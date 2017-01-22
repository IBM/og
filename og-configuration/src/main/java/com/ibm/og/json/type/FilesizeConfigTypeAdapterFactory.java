/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.json.type;

import java.io.IOException;

import com.ibm.og.json.FilesizeConfig;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * A type adapter for file size that allows for either the full filesize configuration or a decimal
 * that represents average filesize, with all other defaults
 * 
 * @since 1.0
 */
public class FilesizeConfigTypeAdapterFactory implements TypeAdapterFactory {
  public FilesizeConfigTypeAdapterFactory() {}

  @Override
  @SuppressWarnings("unchecked")
  public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
    final Class<T> rawType = (Class<T>) type.getRawType();
    if (!FilesizeConfig.class.equals(rawType)) {
      return null;
    }

    final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);

    return new TypeAdapter<T>() {
      @Override
      public void write(final JsonWriter out, final T value) throws IOException {
        delegate.write(out, value);
      }

      @Override
      public T read(final JsonReader in) throws IOException {
        if (JsonToken.NUMBER == in.peek()) {
          return (T) new FilesizeConfig(in.nextDouble());
        }

        return delegate.read(in);
      }

    }.nullSafe();
  }
}
