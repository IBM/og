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
import java.util.Locale;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.cleversafe.oom.operation.Entity;
import com.cleversafe.oom.operation.EntityType;
import com.cleversafe.oom.operation.Method;
import com.cleversafe.oom.operation.Request;
import com.cleversafe.oom.util.Entities;

public class HttpRequest implements Request
{
   private final long id;
   private final Method method;
   private final URI uri;
   private final SortedMap<String, String> headers;
   private final Entity entity;
   private final SortedMap<String, String> metadata;
   private static final DateTimeFormatter RFC1123 =
         DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss zzz").withLocale(Locale.US);

   private HttpRequest(
         final long id,
         final Method method,
         final URI uri,
         final SortedMap<String, String> headers,
         final Entity entity,
         final SortedMap<String, String> metadata)
   {
      checkArgument(id >= 0, "id must be >= 0 [%s]", id);
      this.id = id;
      this.method = checkNotNull(method, "method must not be null");
      this.uri = checkNotNull(uri, "uri must not be null");
      this.headers = checkNotNull(headers, "headers must not be null");
      this.entity = checkNotNull(entity, "entity must not be null");
      this.metadata = checkNotNull(metadata, "metadata must not be null");
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

   public static Builder custom()
   {
      return new Builder();
   }

   public static class Builder
   {
      private long id;
      private Method method;
      private URI uri;
      private final SortedMap<String, String> headers;
      private Entity entity;
      private final SortedMap<String, String> metadata;

      private Builder()
      {
         this.headers = new TreeMap<String, String>();
         this.headers.put("Date", RFC1123.print(new DateTime()));
         this.entity = Entities.of(EntityType.NONE, 0);
         this.metadata = new TreeMap<String, String>();
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
         this.headers.put(key, value);
         return this;
      }

      public Builder withEntity(final Entity entity)
      {
         this.entity = entity;
         return this;
      }

      public Builder withMetaDataEntry(final String key, final String value)
      {
         this.metadata.put(key, value);
         return this;
      }

      public HttpRequest build()
      {
         return new HttpRequest(this.id, this.method, this.uri,
               Collections.unmodifiableSortedMap(this.headers), this.entity,
               Collections.unmodifiableSortedMap(this.metadata));
      }
   }
}
