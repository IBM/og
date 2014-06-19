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

import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import com.cleversafe.oom.operation.Response;

public class HttpResponse implements Response
{
   private final long requestId;
   private final int statusCode;
   private final SortedMap<String, String> headers;
   private final SortedMap<String, String> metadata;

   private HttpResponse(
         final long requestId,
         final int statusCode,
         final SortedMap<String, String> headers,
         final SortedMap<String, String> metadata)
   {
      checkArgument(requestId >= 0, "requestId must be >= 0 [%s]", requestId);
      this.requestId = requestId;
      checkArgument(statusCode >= 0, "statusCode must be >= 0 [%s]", statusCode);
      this.statusCode = statusCode;
      this.headers = checkNotNull(headers, "headers must not be null");
      this.metadata = checkNotNull(metadata, "metadata must not be null");
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
      private long requestId;
      private int statusCode;
      private final SortedMap<String, String> headers;
      private final SortedMap<String, String> metadata;

      public Builder()
      {
         this.headers = new TreeMap<String, String>();
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

      public Builder withMetaDataEntry(final String key, final String value)
      {
         this.metadata.put(key, value);
         return this;
      }

      public HttpResponse build()
      {
         return new HttpResponse(this.requestId, this.statusCode,
               Collections.unmodifiableSortedMap(this.headers),
               Collections.unmodifiableSortedMap(this.metadata));
      }
   }
}
