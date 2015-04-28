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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * A type adapter factory that abstracts over the notion of a selection from many choices
 * 
 * @see ChoiceConfigTypeAdapterFactory
 * @since 1.0
 */
public class SelectionConfigTypeAdapterFactory implements TypeAdapterFactory {

  @Override
  @SuppressWarnings("unchecked")
  public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
    final Class<T> rawType = (Class<T>) type.getRawType();
    if (!SelectionConfig.class.equals(rawType)) {
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
        final Class<?> genericType =
            (Class<?>) ((ParameterizedType) type.getType()).getActualTypeArguments()[0];
        final JsonElement element = gson.getAdapter(JsonElement.class).read(in);

        if (element.isJsonObject()) {
          final JsonObject object = element.getAsJsonObject();
          if (object.entrySet().size() <= 2 && object.has("choices")) {
            return delegate.fromJsonTree(object);
          } else {
            return (T) choice(genericType, object);
          }
        } else if (element.isJsonArray()) {
          return (T) choiceList(genericType, element.getAsJsonArray());
        }

        return (T) choice(genericType, element);
      }

      private <S> SelectionConfig<S> choice(final Class<S> clazz, final JsonElement element)
          throws IOException {
        final SelectionConfig<S> config = new SelectionConfig<S>();
        config.choices.add(gson.getAdapter(choiceToken(clazz)).fromJsonTree(element));
        return config;
      }

      private <S> SelectionConfig<S> choiceList(final Class<S> clazz, final JsonArray array)
          throws IOException {
        final SelectionConfig<S> config = new SelectionConfig<S>();
        config.choices = gson.getAdapter(choiceListToken(clazz)).fromJsonTree(array);
        return config;
      }

      // must use guava's TypeToken implementation to create a TypeToken instance with a dynamic
      // type; then convert back to gson's TypeToken implementation for use in calling code. See:
      // https://groups.google.com/forum/#!topic/guava-discuss/HdBuiO44uaw
      private <S> TypeToken<ChoiceConfig<S>> choiceToken(final Class<S> clazz) {
        @SuppressWarnings("serial")
        final com.google.common.reflect.TypeToken<ChoiceConfig<S>> choiceToken =
            new com.google.common.reflect.TypeToken<ChoiceConfig<S>>() {}.where(
                new TypeParameter<S>() {}, com.google.common.reflect.TypeToken.of(clazz));

        return (TypeToken<ChoiceConfig<S>>) TypeToken.get(choiceToken.getType());
      }

      private <S> TypeToken<List<ChoiceConfig<S>>> choiceListToken(final Class<S> clazz) {
        @SuppressWarnings("serial")
        final com.google.common.reflect.TypeToken<List<ChoiceConfig<S>>> choiceToken =
            new com.google.common.reflect.TypeToken<List<ChoiceConfig<S>>>() {}.where(
                new TypeParameter<S>() {}, com.google.common.reflect.TypeToken.of(clazz));

        return (TypeToken<List<ChoiceConfig<S>>>) TypeToken.get(choiceToken.getType());
      }
    }.nullSafe();
  }
}
