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
// Date: Mar 19, 2014
// ---------------------

package com.cleversafe.oom.http.producer;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.cleversafe.oom.api.Producer;
import com.cleversafe.oom.http.HttpRequest;
import com.cleversafe.oom.operation.Entity;
import com.cleversafe.oom.operation.Method;
import com.cleversafe.oom.operation.Request;
import com.cleversafe.oom.util.Pair;

public class RequestProducer implements Producer<Request>
{
   private final Producer<Long> id;
   private final Producer<Method> method;
   private final Producer<URI> uri;
   private final List<Producer<Pair<String, String>>> headers;
   private final Producer<Entity> entity;
   // TODO this type is awkward, perhaps pass it in as just a map?
   private final Producer<Map<String, String>> metadata;

   public RequestProducer(
         final Producer<Long> id,
         final Producer<Method> method,
         final Producer<URI> uri,
         final List<Producer<Pair<String, String>>> headers,
         final Producer<Entity> entity,
         final Producer<Map<String, String>> metadata)
   {
      this.id = checkNotNull(id, "id must not be null");
      this.method = checkNotNull(method, "method must not be null");
      this.uri = checkNotNull(uri, "uri must not be null");
      this.headers = checkNotNull(headers, "headers must not be null");
      this.entity = checkNotNull(entity, "entity must not be null");
      this.metadata = checkNotNull(metadata, "metadata must not be null");
   }

   @Override
   public Request produce()
   {
      final HttpRequest.Builder context = new HttpRequest.Builder();
      context.withId(this.id.produce())
            .withMethod(this.method.produce())
            .withURI(this.uri.produce());

      for (final Producer<Pair<String, String>> producer : this.headers)
      {
         final Pair<String, String> pair = producer.produce();
         context.withHeader(pair.getKey(), pair.getValue());
      }

      context.withEntity(this.entity.produce());

      final Map<String, String> m = this.metadata.produce();
      for (final Entry<String, String> e : m.entrySet())
      {
         context.withMetaDataEntry(e.getKey(), e.getValue());
      }

      return context.build();
   }
}
