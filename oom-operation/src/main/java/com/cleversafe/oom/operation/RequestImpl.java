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

import java.net.URL;
import java.util.Iterator;
import java.util.SortedMap;

import com.cleversafe.oom.api.Entity;
import com.cleversafe.oom.api.Header;
import com.cleversafe.oom.api.MetaDataEntry;
import com.cleversafe.oom.api.Method;

public class RequestImpl implements Request
{
   private final long id;
   private final String customRequestKey;
   private final Method method;
   private final URL url;
   private final SortedMap<String, Header> headers;
   private final Entity entity;
   private final SortedMap<String, MetaDataEntry> metadata;

   public RequestImpl(
         final long id,
         final String customRequestKey,
         final Method method,
         final URL url,
         final SortedMap<String, Header> headers,
         final Entity entity,
         final SortedMap<String, MetaDataEntry> metadata)
   {
      checkArgument(id >= 0, "id must be >= 0 [%s]", id);
      this.id = id;
      this.customRequestKey = customRequestKey;
      this.method = checkNotNull(method, "method must not be null");
      this.url = checkNotNull(url, "url must not be null");
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
   public String getCustomRequestKey()
   {
      return this.customRequestKey;
   }

   @Override
   public Method getMethod()
   {
      return this.method;
   }

   @Override
   public URL getURL()
   {
      return this.url;
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
         private final Iterator<Header> parent = RequestImpl.this.headers.values().iterator();

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
   public Entity getEntity()
   {
      return this.entity;
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
               RequestImpl.this.metadata.values().iterator();

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
