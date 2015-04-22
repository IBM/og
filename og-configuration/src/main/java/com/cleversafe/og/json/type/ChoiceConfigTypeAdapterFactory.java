/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.json.type;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import com.cleversafe.og.json.ChoiceConfig;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class ChoiceConfigTypeAdapterFactory implements TypeAdapterFactory {
  private static final Map<Type, JsonToken> TOKENS = ImmutableMap.<Type, JsonToken>builder()
      .put(Byte.class, JsonToken.NUMBER).put(Double.class, JsonToken.NUMBER)
      .put(Float.class, JsonToken.NUMBER).put(Integer.class, JsonToken.NUMBER)
      .put(Long.class, JsonToken.NUMBER).put(Short.class, JsonToken.NUMBER)
      .put(Boolean.class, JsonToken.BOOLEAN).put(String.class, JsonToken.STRING).build();

  @Override
  @SuppressWarnings("unchecked")
  public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
    final Class<T> rawType = (Class<T>) type.getRawType();
    if (!ChoiceConfig.class.equals(rawType))
      return null;

    final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
    final Type genericType = ((ParameterizedType) type.getType()).getActualTypeArguments()[0];

    return new TypeAdapter<T>() {
      @Override
      public void write(final JsonWriter out, final T value) throws IOException {
        delegate.write(out, value);
      }

      @Override
      public T read(final JsonReader in) throws IOException {
        JsonToken genericToken = ChoiceConfigTypeAdapterFactory.TOKENS.get(genericType);

        if (genericToken != null && genericToken.equals(in.peek())) {
          switch (in.peek()) {
            case BOOLEAN:
              return (T) new ChoiceConfig<Boolean>(in.nextBoolean());
            case NUMBER:
              return (T) new ChoiceConfig<Double>(in.nextDouble());
            default:
              return (T) new ChoiceConfig<String>(in.nextString());
          }
        }
        return delegate.read(in);
      }
    }.nullSafe();
  }
}
