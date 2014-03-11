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
import java.util.SortedMap;

public class ResponseImpl implements Response
{
   private final long requestId;
   private final int statusCode;
   private final SortedMap<String, Header> headers;
   private final SortedMap<String, MetaDataEntry> metadata;

   public ResponseImpl(
         final long requestId,
         final int statusCode,
         final SortedMap<String, Header> headers,
         final SortedMap<String, MetaDataEntry> metadata)
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
      final Header header = this.headers.get(key);
      if (header != null)
         return header.getValue();
      return null;
   }

   @Override
   public Iterator<Header> headers()
   {
      return new Iterator<Header>()
      {
         private final Iterator<Header> parent = ResponseImpl.this.headers.values().iterator();

         @Override
         public boolean hasNext()
         {
            return this.parent.hasNext();
         }

         @Override
         public Header next()
         {
            return this.parent.next();
         }

         @Override
         public void remove()
         {
            throw new UnsupportedOperationException(
                  "header iterator does not support remove operation");
         }
      };
   }

   @Override
   public String getMetaDataEntry(final String key)
   {
      final MetaDataEntry metadataEntry = this.metadata.get(key);
      if (metadataEntry != null)
         return metadataEntry.getValue();
      return null;
   }

   @Override
   public Iterator<MetaDataEntry> metaData()
   {
      return new Iterator<MetaDataEntry>()
      {
         private final Iterator<MetaDataEntry> parent =
               ResponseImpl.this.metadata.values().iterator();

         @Override
         public boolean hasNext()
         {
            return this.parent.hasNext();
         }

         @Override
         public MetaDataEntry next()
         {
            return this.parent.next();
         }

         @Override
         public void remove()
         {
            throw new UnsupportedOperationException(
                  "metadata iterator does not support remove operation");
         }
      };
   }
}
