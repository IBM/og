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
import java.util.HashMap;
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
import com.cleversafe.og.operation.EntityType;
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

// TODO this module could probably be used for all named object apis
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
      final List<Producer<String>> parts = new ArrayList<Producer<String>>();
      addUriRoot(parts, uriRoot);
      parts.add(container);
      parts.add(object);
      final Producer<URI> writeURI = UriProducer.custom()
            .withScheme(scheme)
            .toHost(host)
            .onPort(port)
            .atPath(parts)
            .withQueryParams(queryParams)
            .build();
      final Map<String, String> metadata = new HashMap<String, String>();

      return new RequestProducer(id,
            Producers.of(Method.PUT),
            writeURI,
            headers,
            entity,
            Producers.of(metadata));
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
      final List<Producer<String>> parts = new ArrayList<Producer<String>>();
      addUriRoot(parts, uriRoot);
      parts.add(container);
      parts.add(object);
      final Producer<URI> readURI = UriProducer.custom()
            .withScheme(scheme)
            .toHost(host)
            .onPort(port)
            .atPath(parts)
            .withQueryParams(queryParams)
            .build();
      final Map<String, String> metadata = new HashMap<String, String>();

      return new RequestProducer(id,
            Producers.of(Method.GET),
            readURI,
            headers,
            Producers.of(Entities.of(EntityType.NONE, 0)),
            Producers.of(metadata));
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
      final List<Producer<String>> parts = new ArrayList<Producer<String>>();
      addUriRoot(parts, uriRoot);
      parts.add(container);
      parts.add(object);
      final Producer<URI> deleteURI = UriProducer.custom()
            .withScheme(scheme)
            .toHost(host)
            .onPort(port)
            .atPath(parts)
            .withQueryParams(queryParams)
            .build();
      final Map<String, String> metadata = new HashMap<String, String>();

      return new RequestProducer(id,
            Producers.of(Method.DELETE),
            deleteURI,
            headers,
            Producers.of(Entities.of(EntityType.NONE, 0)),
            Producers.of(metadata));
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

   // TODO better way to do this? Maybe uriRoot should never be null and/or should be propagated
   // all the way to URIProducer
   private void addUriRoot(final List<Producer<String>> parts, final Producer<String> uriRoot)
   {
      if (uriRoot != null)
         parts.add(uriRoot);
   }
}
