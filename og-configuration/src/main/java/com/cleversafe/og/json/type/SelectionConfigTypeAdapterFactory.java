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
import java.util.List;

import com.cleversafe.og.json.ChoiceConfig;
import com.cleversafe.og.json.SelectionConfig;
import com.google.common.reflect.TypeParameter;
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
      public T read(final JsonReader in) throws IOException {
        final Class<?> genericType =
            (Class<?>) ((ParameterizedType) type.getType()).getActualTypeArguments()[0];

        switch (in.peek()) {
          case BOOLEAN:
          case NUMBER:
          case STRING:
            return (T) primitive(genericType, in);
          case BEGIN_ARRAY:
            return (T) list(genericType, in);
          default:
            return delegate.read(in);
        }
      }

      private <S> SelectionConfig<S> primitive(Class<S> clazz, JsonReader in) throws IOException {
        SelectionConfig<S> config = new SelectionConfig<S>();
        config.choices.add(gson.getAdapter(primitiveToken(clazz)).read(in));
        return config;
      }

      private <S> SelectionConfig<S> list(Class<S> clazz, JsonReader in) throws IOException {
        SelectionConfig<S> config = new SelectionConfig<S>();
        config.choices = gson.getAdapter(listToken(clazz)).read(in);
        return config;
      }

      /*
       * must use guava's TypeToken implementation to create a TypeToken instance with a dynamic
       * type; then convert back to gson's TypeToken implementation for use in calling code. See:
       * https://groups.google.com/forum/#!topic/guava-discuss/HdBuiO44uaw
       */
      private <S> TypeToken<ChoiceConfig<S>> primitiveToken(Class<S> clazz) {
        @SuppressWarnings("serial")
        com.google.common.reflect.TypeToken<ChoiceConfig<S>> choiceToken =
            new com.google.common.reflect.TypeToken<ChoiceConfig<S>>() {}.where(
                new TypeParameter<S>() {}, com.google.common.reflect.TypeToken.of(clazz));

        return (TypeToken<ChoiceConfig<S>>) TypeToken.get(choiceToken.getType());
      }

      private <S> TypeToken<List<ChoiceConfig<S>>> listToken(Class<S> clazz) {
        @SuppressWarnings("serial")
        com.google.common.reflect.TypeToken<List<ChoiceConfig<S>>> choiceToken =
            new com.google.common.reflect.TypeToken<List<ChoiceConfig<S>>>() {}.where(
                new TypeParameter<S>() {}, com.google.common.reflect.TypeToken.of(clazz));

        return (TypeToken<List<ChoiceConfig<S>>>) TypeToken.get(choiceToken.getType());
      }
    }.nullSafe();
  }
}
