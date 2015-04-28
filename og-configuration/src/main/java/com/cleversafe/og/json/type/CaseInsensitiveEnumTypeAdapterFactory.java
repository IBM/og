/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.json.type;

import java.io.IOException;
import java.util.Locale;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * A type adapter factory that deserializes enums in a case insensitive way
 * 
 * @since 1.0
 */
public class CaseInsensitiveEnumTypeAdapterFactory implements TypeAdapterFactory {
  @Override
  public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
    @SuppressWarnings("unchecked")
    final Class<T> rawType = (Class<T>) type.getRawType();
    if (!rawType.isEnum()) {
      return null;
    }

    return new TypeAdapter<T>() {
      @Override
      public void write(final JsonWriter out, final T value) throws IOException {
        out.value(value.toString().toLowerCase(Locale.US));
      }

      @Override
      @SuppressWarnings("unchecked")
      public T read(final JsonReader in) throws IOException {
        final String s = in.nextString().toUpperCase(Locale.US);
        for (final Object enumEntry : rawType.getEnumConstants()) {
          if (enumEntry.toString().equals(s)) {
            return (T) enumEntry;
          }
        }
        throw new JsonSyntaxException(String.format("Could not parse into enum [%s]", s));
      }
    }.nullSafe();
  }
}
