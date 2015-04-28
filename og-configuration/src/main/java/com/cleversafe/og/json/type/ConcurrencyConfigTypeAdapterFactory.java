/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.json.type;

import java.io.IOException;

import com.cleversafe.og.json.ConcurrencyConfig;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * A type adapter factory for concurrency that allows for either the complete configuration or a
 * decimal that represents thread count for the default concurrency type
 * 
 * @since 1.0
 */
public class ConcurrencyConfigTypeAdapterFactory implements TypeAdapterFactory {
  public ConcurrencyConfigTypeAdapterFactory() {}

  @Override
  @SuppressWarnings("unchecked")
  public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
    final Class<T> rawType = (Class<T>) type.getRawType();
    if (!ConcurrencyConfig.class.equals(rawType))
      return null;

    final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);

    return new TypeAdapter<T>() {
      @Override
      public void write(final JsonWriter out, final T value) throws IOException {
        delegate.write(out, value);
      }

      @Override
      public T read(final JsonReader in) throws IOException {
        if (JsonToken.NUMBER == in.peek())
          return (T) new ConcurrencyConfig(in.nextDouble());

        return delegate.read(in);
      }

    }.nullSafe();
  }
}
