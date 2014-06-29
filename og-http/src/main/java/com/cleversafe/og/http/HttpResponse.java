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

package com.cleversafe.og.http;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import com.cleversafe.og.operation.Entity;
import com.cleversafe.og.operation.Metadata;
import com.cleversafe.og.operation.Response;
import com.cleversafe.og.util.Entities;

public class HttpResponse implements Response
{
   private final long requestId;
   private final int statusCode;
   private final SortedMap<String, String> headers;
   private final Entity entity;
   private final SortedMap<String, String> metadata;

   private HttpResponse(
         final long requestId,
         final int statusCode,
         final SortedMap<String, String> headers,
         final Entity entity,
         final SortedMap<String, String> metadata)
   {
      checkArgument(requestId >= 0, "requestId must be >= 0 [%s]", requestId);
      this.requestId = requestId;
      checkArgument(statusCode >= 0, "statusCode must be >= 0 [%s]", statusCode);
      this.statusCode = statusCode;
      this.headers = checkNotNull(headers);
      this.entity = checkNotNull(entity);
      this.metadata = checkNotNull(metadata);
   }

   @Override
   public long getRequestId()
   {
      return this.requestId;
   }

   @Override
   public int getStatusCode()
   {
      return this.statusCode;
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
   public String getMetadata(final Metadata key)
   {
      return this.metadata.get(key.toString());
   }

   @Override
   public String getMetadata(final String key)
   {
      return this.metadata.get(key);
   }

   @Override
   public Iterator<Entry<String, String>> metadata()
   {
      return this.metadata.entrySet().iterator();
   }

   public static Builder custom()
   {
      return new Builder();
   }

   public static class Builder
   {
      private long requestId;
      private int statusCode;
      private final SortedMap<String, String> headers;
      private Entity entity;
      private final SortedMap<String, String> metadata;

      private Builder()
      {
         this.headers = new TreeMap<String, String>();
         this.entity = Entities.none();
         this.metadata = new TreeMap<String, String>();
      }

      public Builder withRequestId(final long requestId)
      {
         this.requestId = requestId;
         return this;
      }

      public Builder withStatusCode(final int statusCode)
      {
         this.statusCode = statusCode;
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

      public Builder withMetadata(final Metadata key, final String value)
      {
         return withMetadata(key.toString(), value);
      }

      public Builder withMetadata(final String key, final String value)
      {
         this.metadata.put(key, value);
         return this;
      }

      public HttpResponse build()
      {
         return new HttpResponse(this.requestId, this.statusCode,
               Collections.unmodifiableSortedMap(this.headers), this.entity,
               Collections.unmodifiableSortedMap(this.metadata));
      }
   }
}
