/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.json.type;

import java.io.IOException;
import java.util.List;

import com.cleversafe.og.json.ChoiceConfig;
import com.cleversafe.og.json.SelectionConfig;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class SelectionConfigTypeAdapterFactory implements TypeAdapterFactory {
  @Override
  @SuppressWarnings("unchecked")
  public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
    final Class<T> rawType = (Class<T>) type.getRawType();
    if (!SelectionConfig.class.equals(rawType))
      return null;

    final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);

    return new TypeAdapter<T>() {
      @Override
      public void write(final JsonWriter out, final T value) throws IOException {
        delegate.write(out, value);
      }

      @Override
      @SuppressWarnings("rawtypes")
      public T read(final JsonReader in) throws IOException {
        switch (in.peek()) {
          case BOOLEAN:
            return (T) new SelectionConfig<Boolean>(in.nextBoolean());
          case NUMBER:
            return (T) new SelectionConfig<Double>(in.nextDouble());
          case STRING:
            return (T) new SelectionConfig<String>(in.nextString());
          case BEGIN_ARRAY: {
            SelectionConfig config = new SelectionConfig();
            config.choices = gson.getAdapter(new TypeToken<List<ChoiceConfig>>() {}).read(in);
            return (T) config;
          }
          default:
            return delegate.read(in);
        }
      }
    }.nullSafe();
  }
}
