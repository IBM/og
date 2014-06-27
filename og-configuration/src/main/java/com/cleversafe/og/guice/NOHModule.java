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
// Date: Apr 7, 2014
// ---------------------

package com.cleversafe.og.guice;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.cleversafe.og.guice.annotation.Delete;
import com.cleversafe.og.guice.annotation.DeleteHeaders;
import com.cleversafe.og.guice.annotation.DeleteHost;
import com.cleversafe.og.guice.annotation.DeleteObjectName;
import com.cleversafe.og.guice.annotation.Read;
import com.cleversafe.og.guice.annotation.ReadHeaders;
import com.cleversafe.og.guice.annotation.ReadHost;
import com.cleversafe.og.guice.annotation.ReadObjectName;
import com.cleversafe.og.guice.annotation.TestContainer;
import com.cleversafe.og.guice.annotation.TestEntity;
import com.cleversafe.og.guice.annotation.TestPort;
import com.cleversafe.og.guice.annotation.TestQueryParams;
import com.cleversafe.og.guice.annotation.TestScheme;
import com.cleversafe.og.guice.annotation.TestUriRoot;
import com.cleversafe.og.guice.annotation.TesttId;
import com.cleversafe.og.guice.annotation.Write;
import com.cleversafe.og.guice.annotation.WriteHeaders;
import com.cleversafe.og.guice.annotation.WriteHost;
import com.cleversafe.og.guice.annotation.WriteObjectName;
import com.cleversafe.og.http.Scheme;
import com.cleversafe.og.http.producer.RequestProducer;
import com.cleversafe.og.http.producer.UriProducer;
import com.cleversafe.og.http.util.HttpUtil;
import com.cleversafe.og.object.manager.ObjectManager;
import com.cleversafe.og.object.manager.ObjectNameConsumer;
import com.cleversafe.og.operation.Entity;
import com.cleversafe.og.operation.Method;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.operation.Response;
import com.cleversafe.og.util.ByteBufferConsumers;
import com.cleversafe.og.util.Entities;
import com.cleversafe.og.util.Operation;
import com.cleversafe.og.util.Pair;
import com.cleversafe.og.util.consumer.ByteBufferConsumer;
import com.cleversafe.og.util.consumer.Consumer;
import com.cleversafe.og.util.producer.Producer;
import com.cleversafe.og.util.producer.Producers;
import com.google.common.base.Function;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class NOHModule extends AbstractModule
{
   @Override
   protected void configure()
   {}

   @Provides
   @Singleton
   @Write
   public Producer<Request> provideWrite(
         @TesttId final Producer<Long> id,
         @TestScheme final Producer<Scheme> scheme,
         @WriteHost final Producer<String> host,
         @TestPort final Producer<Integer> port,
         @TestUriRoot final Producer<String> uriRoot,
         @TestContainer final Producer<String> container,
         @WriteObjectName final Producer<String> object,
         @TestQueryParams final Producer<Map<String, String>> queryParams,
         @WriteHeaders final List<Producer<Pair<String, String>>> headers,
         @TestEntity final Producer<Entity> entity)
   {
      final Producer<URI> uri =
            createUri(scheme, host, port, uriRoot, container, object, queryParams);
      return createRequestProducer(id, Method.PUT, uri, headers, entity,
            Collections.<String, String> emptyMap());
   }

   @Provides
   @Singleton
   @Read
   public Producer<Request> provideRead(
         @TesttId final Producer<Long> id,
         @TestScheme final Producer<Scheme> scheme,
         @ReadHost final Producer<String> host,
         @TestPort final Producer<Integer> port,
         @TestUriRoot final Producer<String> uriRoot,
         @TestContainer final Producer<String> container,
         @ReadObjectName final Producer<String> object,
         @TestQueryParams final Producer<Map<String, String>> queryParams,
         @ReadHeaders final List<Producer<Pair<String, String>>> headers)
   {
      final Producer<URI> uri =
            createUri(scheme, host, port, uriRoot, container, object, queryParams);
      return createRequestProducer(id, Method.GET, uri, headers, Producers.of(Entities.none()),
            Collections.<String, String> emptyMap());
   }

   @Provides
   @Singleton
   @Delete
   public Producer<Request> provideDelete(
         @TesttId final Producer<Long> id,
         @TestScheme final Producer<Scheme> scheme,
         @DeleteHost final Producer<String> host,
         @TestPort final Producer<Integer> port,
         @TestUriRoot final Producer<String> uriRoot,
         @TestContainer final Producer<String> container,
         @DeleteObjectName final Producer<String> object,
         @TestQueryParams final Producer<Map<String, String>> queryParams,
         @DeleteHeaders final List<Producer<Pair<String, String>>> headers)
   {
      final Producer<URI> uri =
            createUri(scheme, host, port, uriRoot, container, object, queryParams);
      return createRequestProducer(id, Method.DELETE, uri, headers, Producers.of(Entities.none()),
            Collections.<String, String> emptyMap());
   }

   private Producer<Request> createRequestProducer(
         final Producer<Long> id,
         final Method method,
         final Producer<URI> uri,
         final List<Producer<Pair<String, String>>> headers,
         final Producer<Entity> entity,
         final Map<String, String> metadata)
   {
      return new RequestProducer(id, Producers.of(method), uri, headers, entity,
            Producers.of(metadata));
   }

   private Producer<URI> createUri(
         final Producer<Scheme> scheme,
         final Producer<String> host,
         final Producer<Integer> port,
         final Producer<String> uriRoot,
         final Producer<String> container,
         final Producer<String> object,
         final Producer<Map<String, String>> queryParams)
   {
      final List<Producer<String>> parts = new ArrayList<Producer<String>>();
      if (uriRoot != null)
         parts.add(uriRoot);
      parts.add(container);
      parts.add(object);
      return UriProducer.custom()
            .withScheme(scheme)
            .toHost(host)
            .onPort(port)
            .atPath(parts)
            .withQueryParams(queryParams)
            .build();
   }

   @Provides
   @Singleton
   public List<Consumer<Response>> provideObjectNameConsumers(
         final ObjectManager objectManager,
         final Map<Long, Request> pendingRequests)
   {
      final List<Integer> sc = HttpUtil.SUCCESS_STATUS_CODES;
      final List<Consumer<Response>> list = new ArrayList<Consumer<Response>>();
      list.add(new ObjectNameConsumer(objectManager, pendingRequests, Operation.WRITE, sc));
      list.add(new ObjectNameConsumer(objectManager, pendingRequests, Operation.READ, sc));
      list.add(new ObjectNameConsumer(objectManager, pendingRequests, Operation.DELETE, sc));
      return list;
   }

   @Provides
   @Singleton
   public Function<String, ByteBufferConsumer> provideByteBufferConsumers()
   {
      return new Function<String, ByteBufferConsumer>()
      {
         @Override
         public ByteBufferConsumer apply(final String input)
         {
            return ByteBufferConsumers.noOp();
         }
      };
   }
}
