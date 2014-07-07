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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.cleversafe.og.http.util.HttpUtil;
import com.cleversafe.og.operation.Entity;
import com.cleversafe.og.operation.Metadata;
import com.cleversafe.og.operation.Response;
import com.cleversafe.og.util.Entities;

public class HttpResponse implements Response
{
   private final int statusCode;
   private final Map<String, String> headers;
   private final Entity entity;
   private final Map<String, String> metadata;

   private HttpResponse(final Builder builder)
   {
      this.statusCode = builder.statusCode;
      checkArgument(HttpUtil.VALID_STATUS_CODES.contains(this.statusCode),
            "statusCode must be a valid status code [%s]", this.statusCode);
      checkNotNull(builder.headers);
      // defensive copy
      this.headers = new LinkedHashMap<String, String>();
      for (final Entry<String, String> h : builder.headers.entrySet())
      {
         this.headers.put(h.getKey(), h.getValue());
      }
      this.entity = checkNotNull(builder.entity);
      checkNotNull(builder.metadata);
      // defensive copy
      this.metadata = new LinkedHashMap<String, String>();
      for (final Entry<String, String> m : builder.metadata.entrySet())
      {
         this.metadata.put(m.getKey(), m.getValue());
      }
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

   public static class Builder
   {
      private int statusCode;
      private final Map<String, String> headers;
      private Entity entity;
      private final Map<String, String> metadata;

      public Builder()
      {
         this.headers = new LinkedHashMap<String, String>();
         this.entity = Entities.none();
         this.metadata = new LinkedHashMap<String, String>();
      }

      public Builder withStatusCode(final int statusCode)
      {
         this.statusCode = statusCode;
         return this;
      }

      public Builder withHeader(final String key, final String value)
      {
         this.headers.put(checkNotNull(key), checkNotNull(value));
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
         this.metadata.put(checkNotNull(key), checkNotNull(value));
         return this;
      }

      public HttpResponse build()
      {
         return new HttpResponse(this);
      }
   }
}
