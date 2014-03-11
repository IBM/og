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

package com.cleversafe.oom.operation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedMap;

public class ResponseImpl implements Response
{
   private final long requestId;
   private final int statusCode;
   private final SortedMap<String, String> headers;
   private final SortedMap<String, String> metadata;

   public ResponseImpl(
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
}
