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
// Date: Mar 21, 2014
// ---------------------

package com.cleversafe.oom.http;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.cleversafe.oom.operation.Entity;
import com.cleversafe.oom.operation.Method;
import com.cleversafe.oom.operation.Request;
import com.cleversafe.oom.operation.RequestContext;

public class HttpRequestContext implements RequestContext
{
   private long id;
   private String customRequestKey;
   private Method method;
   private URI uri;
   private final Map<String, String> headers;
   private Entity entity;
   private final Map<String, String> metadata;

   public HttpRequestContext()
   {
      this.headers = new HashMap<String, String>();
      this.metadata = new HashMap<String, String>();
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

   @Override
   public RequestContext withId(final long id)
   {
      this.id = id;
      return this;
   }

   @Override
   public RequestContext withCustomRequestKey(final String customRequestKey)
   {
      this.customRequestKey = customRequestKey;
      return this;
   }

   @Override
   public RequestContext withMethod(final Method method)
   {
      this.method = method;
      return this;
   }

   @Override
   public RequestContext withURI(final URI uri)
   {
      this.uri = uri;
      return this;
   }

   @Override
   public RequestContext withHeader(final String key, final String value)
   {
      this.headers.put(key, value);
      return this;
   }

   @Override
   public RequestContext withHeaders(final Map<String, String> headers)
   {
      this.headers.putAll(headers);
      return this;
   }

   @Override
   public RequestContext withEntity(final Entity entity)
   {
      this.entity = entity;
      return this;
   }

   @Override
   public RequestContext withMetaDataEntry(final String key, final String value)
   {
      this.metadata.put(key, value);
      return this;
   }

   @Override
   public RequestContext withMetaData(final Map<String, String> metadata)
   {
      this.metadata.putAll(metadata);
      return this;
   }

   @Override
   public Request build()
   {
      return new HttpRequest(this.id, this.customRequestKey, this.method, this.uri, this.headers,
            this.entity, this.metadata);
   }
}
