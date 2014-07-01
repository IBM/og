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
import java.util.Map.Entry;

import com.cleversafe.og.consumer.ReadObjectNameConsumer;
import com.cleversafe.og.consumer.WriteObjectNameConsumer;
import com.cleversafe.og.guice.annotation.Delete;
import com.cleversafe.og.guice.annotation.DeleteHeaders;
import com.cleversafe.og.guice.annotation.DeleteHost;
import com.cleversafe.og.guice.annotation.DeleteMetadata;
import com.cleversafe.og.guice.annotation.DeleteObjectName;
import com.cleversafe.og.guice.annotation.DeleteUri;
import com.cleversafe.og.guice.annotation.Read;
import com.cleversafe.og.guice.annotation.ReadHeaders;
import com.cleversafe.og.guice.annotation.ReadHost;
import com.cleversafe.og.guice.annotation.ReadMetadata;
import com.cleversafe.og.guice.annotation.ReadObjectName;
import com.cleversafe.og.guice.annotation.ReadUri;
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
import com.cleversafe.og.guice.annotation.WriteMetadata;
import com.cleversafe.og.guice.annotation.WriteObjectName;
import com.cleversafe.og.guice.annotation.WriteUri;
import com.cleversafe.og.http.Scheme;
import com.cleversafe.og.http.producer.RequestProducer;
import com.cleversafe.og.http.producer.UriProducer;
import com.cleversafe.og.http.util.Api;
import com.cleversafe.og.http.util.HttpUtil;
import com.cleversafe.og.object.manager.ObjectManager;
import com.cleversafe.og.operation.Entity;
import com.cleversafe.og.operation.Metadata;
import com.cleversafe.og.operation.Method;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.operation.Response;
import com.cleversafe.og.soh.object.consumer.SOHWriteByteBufferConsumer;
import com.cleversafe.og.soh.object.consumer.SOHWriteObjectNameConsumer;
import com.cleversafe.og.util.ByteBufferConsumers;
import com.cleversafe.og.util.Entities;
import com.cleversafe.og.util.Pair;
import com.cleversafe.og.util.consumer.ByteBufferConsumer;
import com.cleversafe.og.util.consumer.Consumer;
import com.cleversafe.og.util.producer.CachingProducer;
import com.cleversafe.og.util.producer.Producer;
import com.cleversafe.og.util.producer.Producers;
import com.google.common.base.Function;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class ApiModule extends AbstractModule
{
   private static final String SOH_PUT_OBJECT = "soh.put_object";

   @Override
   protected void configure()
   {}

   @Provides
   @Singleton
   @Write
   public Producer<Request> provideWrite(
         @TesttId final Producer<Long> id,
         @WriteUri final Producer<URI> uri,
         @WriteObjectName final CachingProducer<String> object,
         @WriteHeaders final List<Producer<Pair<String, String>>> headers,
         @TestEntity final Producer<Entity> entity,
         @WriteMetadata final Map<String, String> metadata)
   {
      return createRequestProducer(id, Method.PUT, uri, object, headers, entity, metadata);
   }

   @Provides
   @Singleton
   @WriteUri
   public Producer<URI> providWriteUri(
         @TestScheme final Producer<Scheme> scheme,
         @WriteHost final Producer<String> host,
         @TestPort final Producer<Integer> port,
         @TestUriRoot final Producer<String> uriRoot,
         @TestContainer final Producer<String> container,
         @WriteObjectName final CachingProducer<String> object,
         @TestQueryParams final Map<String, String> queryParameters)
   {
      return createUri(scheme, host, port, uriRoot, container, object, queryParameters);
   }

   @Provides
   @WriteMetadata
   public Map<String, String> provideWriteMetadata(final Api api)
   {
      final Map<String, String> metadata = new HashMap<String, String>();
      // SOH needs to use a special response procesor to extract the returned object id
      if (Api.SOH == api)
         metadata.put(Metadata.RESPONSE_BODY_PROCESSOR.toString(), SOH_PUT_OBJECT);
      return metadata;
   }

   @Provides
   @Singleton
   @Read
   public Producer<Request> provideRead(
         @TesttId final Producer<Long> id,
         @ReadUri final Producer<URI> uri,
         @ReadObjectName final CachingProducer<String> object,
         @ReadHeaders final List<Producer<Pair<String, String>>> headers,
         @ReadMetadata final Map<String, String> metadata)
   {
      return createRequestProducer(id, Method.GET, uri, object, headers,
            Producers.of(Entities.none()), metadata);
   }

   @Provides
   @Singleton
   @ReadUri
   public Producer<URI> providReadUri(
         @TestScheme final Producer<Scheme> scheme,
         @ReadHost final Producer<String> host,
         @TestPort final Producer<Integer> port,
         @TestUriRoot final Producer<String> uriRoot,
         @TestContainer final Producer<String> container,
         @ReadObjectName final CachingProducer<String> object,
         @TestQueryParams final Map<String, String> queryParameters)
   {
      return createUri(scheme, host, port, uriRoot, container, object, queryParameters);
   }

   @Provides
   @ReadMetadata
   public Map<String, String> provideReadMetadata()
   {
      return new HashMap<String, String>();
   }

   @Provides
   @Singleton
   @Delete
   public Producer<Request> provideDelete(
         @TesttId final Producer<Long> id,
         @DeleteUri final Producer<URI> uri,
         @DeleteObjectName final CachingProducer<String> object,
         @DeleteHeaders final List<Producer<Pair<String, String>>> headers,
         @DeleteMetadata final Map<String, String> metadata)
   {
      return createRequestProducer(id, Method.DELETE, uri, object, headers,
            Producers.of(Entities.none()),
            metadata);
   }

   @Provides
   @Singleton
   @DeleteUri
   public Producer<URI> providDeleteUri(
         @TestScheme final Producer<Scheme> scheme,
         @DeleteHost final Producer<String> host,
         @TestPort final Producer<Integer> port,
         @TestUriRoot final Producer<String> uriRoot,
         @TestContainer final Producer<String> container,
         @DeleteObjectName final CachingProducer<String> object,
         @TestQueryParams final Map<String, String> queryParameters)
   {
      return createUri(scheme, host, port, uriRoot, container, object, queryParameters);
   }

   @Provides
   @DeleteMetadata
   public Map<String, String> provideDeleteMetadata()
   {
      return new HashMap<String, String>();
   }

   private Producer<Request> createRequestProducer(
         final Producer<Long> id,
         final Method method,
         final Producer<URI> uri,
         final CachingProducer<String> object,
         final List<Producer<Pair<String, String>>> headers,
         final Producer<Entity> entity,
         final Map<String, String> metadata)
   {
      final RequestProducer.Builder b = RequestProducer.custom()
            .withId(id)
            .withMethod(method)
            .withUri(uri);

      if (object != null)
         b.withObject(object);

      for (final Producer<Pair<String, String>> header : headers)
      {
         b.withHeader(header);
      }

      b.withEntity(entity);

      for (final Entry<String, String> m : metadata.entrySet())
      {
         b.withMetadata(m.getKey(), m.getValue());
      }

      return b.build();
   }

   private Producer<URI> createUri(
         final Producer<Scheme> scheme,
         final Producer<String> host,
         final Producer<Integer> port,
         final Producer<String> uriRoot,
         final Producer<String> container,
         final Producer<String> object,
         final Map<String, String> queryParameters)
   {
      final List<Producer<String>> parts = new ArrayList<Producer<String>>();
      if (uriRoot != null)
         parts.add(uriRoot);
      parts.add(container);
      if (object != null)
         parts.add(object);
      final UriProducer.Builder b = UriProducer.custom()
            .withScheme(scheme)
            .toHost(host)
            .onPort(port)
            .atPath(parts);

      for (final Entry<String, String> e : queryParameters.entrySet())
      {
         b.withQueryParameter(e.getKey(), e.getValue());
      }
      return b.build();
   }

   @Provides
   @Singleton
   public List<Consumer<Response>> provideObjectNameConsumers(
         final Api api,
         final ObjectManager objectManager,
         final Map<Long, Request> pendingRequests)
   {
      final List<Integer> sc = HttpUtil.SUCCESS_STATUS_CODES;
      final List<Consumer<Response>> list = new ArrayList<Consumer<Response>>();
      // SOH writes must consume writes by processing response metadata
      if (Api.SOH == api)
         list.add(new SOHWriteObjectNameConsumer(objectManager, pendingRequests, sc));
      else
         list.add(new WriteObjectNameConsumer(objectManager, pendingRequests, sc));
      list.add(new ReadObjectNameConsumer(objectManager, pendingRequests, sc));
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
            if (SOH_PUT_OBJECT.equals(input))
            {
               return new SOHWriteByteBufferConsumer();
            }
            return ByteBufferConsumers.noOp();
         }
      };
   }
}
