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

import com.cleversafe.og.json.ChoiceConfig;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * A generic choice type adapter factory that can be abstracted over arbitrary types
 * 
 * @see SelectionConfigTypeAdapterFactory
 * @since 1.0
 */
public class ChoiceConfigTypeAdapterFactory implements TypeAdapterFactory {

  @Override
  @SuppressWarnings("unchecked")
  public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
    final Class<T> rawType = (Class<T>) type.getRawType();
    if (!ChoiceConfig.class.equals(rawType))
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
        final Class<?> genericType =
            (Class<?>) ((ParameterizedType) type.getType()).getActualTypeArguments()[0];
        final TypeAdapter<JsonElement> jsonElementAdapter = gson.getAdapter(JsonElement.class);

        // the tree api is used here rather than the stream api so that the full object can be
        // inspected and we can differentiate between a ChoiceConfig<T> object or the underlying T
        // object itself. With the stream api there would be no way to rewind the stream once this
        // determination is made
        //
        // this logic allows the user to configure a choice of T in both the standard form, or
        // compactly if the default choice weight is sufficient e.g.
        //
        // standard form
        // {"choice": {fields for T object}, "weight": 1.0} <- weight is optional here
        //
        // compact form where default weight is acceptable
        // {fields for T object}
        JsonElement element = jsonElementAdapter.read(in);
        if (element.isJsonObject()) {
          JsonObject object = element.getAsJsonObject();
          if (object.entrySet().size() <= 2 && object.has("choice")) {
            return delegate.fromJsonTree(element);
          }
        }

        return (T) new ChoiceConfig(gson.getAdapter(TypeToken.get(genericType)).fromJsonTree(
            element));
      }
    }.nullSafe();
  }
}
