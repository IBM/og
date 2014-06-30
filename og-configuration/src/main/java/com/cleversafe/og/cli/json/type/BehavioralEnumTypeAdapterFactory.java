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
// Date: Jun 28, 2014
// ---------------------

package com.cleversafe.og.cli.json.type;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

public class BehavioralEnumTypeAdapterFactory<S> implements TypeAdapterFactory
{
   private final TypeAdapter<S> typeAdapter;
   private final Class<S> type;

   public BehavioralEnumTypeAdapterFactory(final Class<S> type, final TypeAdapter<S> typeAdapter)
   {
      this.typeAdapter = checkNotNull(typeAdapter);
      this.type = checkNotNull(type);
   }

   @SuppressWarnings("unchecked")
   @Override
   public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type)
   {
      final Class<T> rawType = (Class<T>) type.getRawType();
      if (rawType.equals(this.type) || parentEquals(rawType, this.type))
         return (TypeAdapter<T>) this.typeAdapter;
      return null;
   }

   private boolean parentEquals(final Class<?> rawType, final Class<?> compare)
   {
      final Class<?> parent = rawType.getSuperclass();
      return parent != null && parent.equals(compare);
   }
}
