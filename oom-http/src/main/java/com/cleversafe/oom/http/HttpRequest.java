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

import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.cleversafe.oom.operation.Entity;
import com.cleversafe.oom.operation.Method;
import com.cleversafe.oom.operation.Request;

public class HttpRequest implements Request
{
   private final long id;
   private final String customRequestKey;
   private final Method method;
   private final URL url;
   private final Map<String, String> headers;
   private final Entity entity;
   private final Map<String, String> metadata;

   private HttpRequest(
         final long id,
         final String customRequestKey,
         final Method method,
         final URL url,
         final Map<String, String> headers,
         final Entity entity,
         final Map<String, String> metadata)
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
}
