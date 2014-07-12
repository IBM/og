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
import com.cleversafe.og.guice.annotation.TestId;
import com.cleversafe.og.guice.annotation.TestPassword;
import com.cleversafe.og.guice.annotation.TestPort;
import com.cleversafe.og.guice.annotation.TestQueryParams;
import com.cleversafe.og.guice.annotation.TestScheme;
import com.cleversafe.og.guice.annotation.TestUriRoot;
import com.cleversafe.og.guice.annotation.TestUsername;
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
         @WriteUri final Producer<URI> uri,
         @WriteObjectName final CachingProducer<String> object,
         @WriteHeaders final Map<Producer<String>, Producer<String>> headers,
         @TestEntity final Producer<Entity> entity,
         @WriteMetadata final Map<Producer<String>, Producer<String>> metadata,
         @TestUsername final Producer<String> username,
         @TestPassword final Producer<String> password)
   {
      return createRequestProducer(Method.PUT, uri, object, headers, entity, metadata, username,
            password);
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
   public Map<Producer<String>, Producer<String>> provideWriteMetadata(
         final Api api,
         @TestId final Producer<String> id)
   {
      final Map<Producer<String>, Producer<String>> metadata = createMetadata(id);
      // SOH needs to use a special response procesor to extract the returned object id
      if (Api.SOH == api)
         metadata.put(Producers.of(Metadata.RESPONSE_BODY_PROCESSOR.toString()),
               Producers.of(SOH_PUT_OBJECT));
      return metadata;
   }

   @Provides
   @Singleton
   @Read
   public Producer<Request> provideRead(
         @ReadUri final Producer<URI> uri,
         @ReadObjectName final CachingProducer<String> object,
         @ReadHeaders final Map<Producer<String>, Producer<String>> headers,
         @ReadMetadata final Map<Producer<String>, Producer<String>> metadata,
         @TestUsername final Producer<String> username,
         @TestPassword final Producer<String> password)
   {
      return createRequestProducer(Method.GET, uri, object, headers, Producers.of(Entities.none()),
            metadata, username, password);
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
   public Map<Producer<String>, Producer<String>> provideReadMetadata(
         @TestId final Producer<String> id)
   {
      return createMetadata(id);
   }

   @Provides
   @Singleton
   @Delete
   public Producer<Request> provideDelete(
         @DeleteUri final Producer<URI> uri,
         @DeleteObjectName final CachingProducer<String> object,
         @DeleteHeaders final Map<Producer<String>, Producer<String>> headers,
         @DeleteMetadata final Map<Producer<String>, Producer<String>> metadata,
         @TestUsername final Producer<String> username,
         @TestPassword final Producer<String> password)
   {
      return createRequestProducer(Method.DELETE, uri, object, headers,
            Producers.of(Entities.none()), metadata, username, password);
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
   public Map<Producer<String>, Producer<String>> provideDeleteMetadata(
         @TestId final Producer<String> id)
   {
      return createMetadata(id);
   }

   public Map<Producer<String>, Producer<String>> createMetadata(final Producer<String> id)
   {
      final Map<Producer<String>, Producer<String>> metadata =
            new HashMap<Producer<String>, Producer<String>>();
      metadata.put(Producers.of(Metadata.REQUEST_ID.toString()), id);
      return metadata;
   }

   private Producer<Request> createRequestProducer(
         final Method method,
         final Producer<URI> uri,
         final CachingProducer<String> object,
         final Map<Producer<String>, Producer<String>> headers,
         final Producer<Entity> entity,
         final Map<Producer<String>, Producer<String>> metadata,
         final Producer<String> username,
         final Producer<String> password)
   {
      final RequestProducer.Builder b = new RequestProducer.Builder(Producers.of(method), uri);

      if (object != null)
         b.withObject(object);

      for (final Entry<Producer<String>, Producer<String>> header : headers.entrySet())
      {
         b.withHeader(header.getKey(), header.getValue());
      }

      b.withEntity(entity);

      for (final Entry<Producer<String>, Producer<String>> m : metadata.entrySet())
      {
         b.withMetadata(m.getKey(), m.getValue());
      }

      if (username != null && password != null)
         b.withCredentials(username, password);

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
      final List<Producer<String>> path = new ArrayList<Producer<String>>();
      if (uriRoot != null)
         path.add(uriRoot);
      path.add(container);
      if (object != null)
         path.add(object);
      final UriProducer.Builder b = new UriProducer.Builder(host, path)
            .withScheme(scheme)
            .onPort(port);

      for (final Entry<String, String> e : queryParameters.entrySet())
      {
         b.withQueryParameter(e.getKey(), e.getValue());
      }
      return b.build();
   }

   @Provides
   @Singleton
   public List<Consumer<Pair<Request, Response>>> provideObjectNameConsumers(
         final ObjectManager objectManager)
   {
      final List<Integer> sc = HttpUtil.SUCCESS_STATUS_CODES;
      final List<Consumer<Pair<Request, Response>>> list =
            new ArrayList<Consumer<Pair<Request, Response>>>();
      list.add(new WriteObjectNameConsumer(objectManager, sc));
      list.add(new ReadObjectNameConsumer(objectManager, sc));
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
