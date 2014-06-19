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
// Date: Mar 11, 2014
// ---------------------

package com.cleversafe.oom.http;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.cleversafe.oom.operation.Entity;
import com.cleversafe.oom.operation.Method;
import com.cleversafe.oom.operation.Request;
import com.google.common.collect.ImmutableSortedMap;

public class HttpRequest implements Request
{
   private final long id;
   private final Method method;
   private final URI uri;
   private final Map<String, String> headers;
   private final Entity entity;
   private final Map<String, String> metadata;

   public HttpRequest(
         final long id,
         final Method method,
         final URI uri,
         final Map<String, String> headers,
         final Entity entity,
         final Map<String, String> metadata)
   {
      checkArgument(id >= 0, "id must be >= 0 [%s]", id);
      this.id = id;
      this.method = checkNotNull(method, "method must not be null");
      this.uri = checkNotNull(uri, "uri must not be null");
      checkNotNull(headers, "headers must not be null");
      this.headers = Collections.unmodifiableMap(headers);
      this.entity = checkNotNull(entity, "entity must not be null");
      checkNotNull(metadata, "metadata must not be null");
      this.metadata = Collections.unmodifiableMap(metadata);

   }

   @Override
   public long getId()
   {
      return this.id;
   }

   @Override
   public Method getMethod()
   {
      return this.method;
   }

   @Override
   public URI getURI()
   {
      return this.uri;
   }

   @Override
   public String getHeader(final String key)
   {
      return this.headers.get(key);
   }

   @Override
   public Iterator<Entry<String, String>> headers()
   {
      return this.headers.entrySet().iterator();
   }

   @Override
   public Entity getEntity()
   {
      return this.entity;
   }

   @Override
   public String getMetaDataEntry(final String key)
   {
      return this.metadata.get(key);
   }

   @Override
   public Iterator<Entry<String, String>> metaData()
   {
      return this.metadata.entrySet().iterator();
   }

   public static class Builder
   {
      private long id;
      private Method method;
      private URI uri;
      private final ImmutableSortedMap.Builder<String, String> headersBuilder;
      private Entity entity;
      private final ImmutableSortedMap.Builder<String, String> metadataBuilder;

      public Builder()
      {
         this.headersBuilder = ImmutableSortedMap.naturalOrder();
         this.metadataBuilder = ImmutableSortedMap.naturalOrder();
      }

      public Builder withId(final long id)
      {
         this.id = id;
         return this;
      }

      public Builder withMethod(final Method method)
      {
         this.method = method;
         return this;
      }

      public Builder withURI(final URI uri)
      {
         this.uri = uri;
         return this;
      }

      public Builder withHeader(final String key, final String value)
      {
         this.headersBuilder.put(key, value);
         return this;
      }

      public Builder withEntity(final Entity entity)
      {
         this.entity = entity;
         return this;
      }

      public Builder withMetaDataEntry(final String key, final String value)
      {
         this.metadataBuilder.put(key, value);
         return this;
      }

      public HttpRequest build()
      {
         return new HttpRequest(this.id, this.method, this.uri, this.headersBuilder.build(),
               this.entity, this.metadataBuilder.build());
      }
   }
}
