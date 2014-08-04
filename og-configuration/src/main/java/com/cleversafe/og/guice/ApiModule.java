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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.cleversafe.og.guice.annotation.Container;
import com.cleversafe.og.guice.annotation.Delete;
import com.cleversafe.og.guice.annotation.DeleteHeaders;
import com.cleversafe.og.guice.annotation.DeleteHost;
import com.cleversafe.og.guice.annotation.DeleteMetadata;
import com.cleversafe.og.guice.annotation.DeleteObjectName;
import com.cleversafe.og.guice.annotation.DeleteUri;
import com.cleversafe.og.guice.annotation.Id;
import com.cleversafe.og.guice.annotation.Password;
import com.cleversafe.og.guice.annotation.Read;
import com.cleversafe.og.guice.annotation.ReadHeaders;
import com.cleversafe.og.guice.annotation.ReadHost;
import com.cleversafe.og.guice.annotation.ReadMetadata;
import com.cleversafe.og.guice.annotation.ReadObjectName;
import com.cleversafe.og.guice.annotation.ReadUri;
import com.cleversafe.og.guice.annotation.UriRoot;
import com.cleversafe.og.guice.annotation.Username;
import com.cleversafe.og.guice.annotation.Write;
import com.cleversafe.og.guice.annotation.WriteHeaders;
import com.cleversafe.og.guice.annotation.WriteHost;
import com.cleversafe.og.guice.annotation.WriteMetadata;
import com.cleversafe.og.guice.annotation.WriteObjectName;
import com.cleversafe.og.guice.annotation.WriteUri;
import com.cleversafe.og.http.Api;
import com.cleversafe.og.http.Scheme;
import com.cleversafe.og.operation.Entity;
import com.cleversafe.og.operation.Metadata;
import com.cleversafe.og.operation.Method;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.producer.CachingProducer;
import com.cleversafe.og.producer.Producer;
import com.cleversafe.og.producer.Producers;
import com.cleversafe.og.producer.RequestProducer;
import com.cleversafe.og.producer.UriProducer;
import com.cleversafe.og.soh.SOHWriteResponseBodyConsumer;
import com.cleversafe.og.util.Entities;
import com.cleversafe.og.util.ResponseBodyConsumer;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
         final Producer<Entity> entity,
         @WriteMetadata final Map<Producer<String>, Producer<String>> metadata,
         @Username final Producer<String> username,
         @Password final Producer<String> password)
   {
      return createRequestProducer(Method.PUT, uri, object, headers, entity, metadata, username,
            password);
   }

   @Provides
   @Singleton
   @WriteUri
   public Producer<URI> providWriteUri(
         final Producer<Scheme> scheme,
         @WriteHost final Producer<String> host,
         final Producer<Integer> port,
         @UriRoot final Producer<String> uriRoot,
         @Container final Producer<String> container,
         @WriteObjectName final CachingProducer<String> object)
   {
      return createUri(scheme, host, port, uriRoot, container, object);
   }

   @Provides
   @WriteMetadata
   public Map<Producer<String>, Producer<String>> provideWriteMetadata(
         final Api api,
         @Id final Producer<String> id)
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
         @Username final Producer<String> username,
         @Password final Producer<String> password)
   {
      return createRequestProducer(Method.GET, uri, object, headers, Producers.of(Entities.none()),
            metadata, username, password);
   }

   @Provides
   @Singleton
   @ReadUri
   public Producer<URI> providReadUri(
         final Producer<Scheme> scheme,
         @ReadHost final Producer<String> host,
         final Producer<Integer> port,
         @UriRoot final Producer<String> uriRoot,
         @Container final Producer<String> container,
         @ReadObjectName final CachingProducer<String> object)
   {
      return createUri(scheme, host, port, uriRoot, container, object);
   }

   @Provides
   @ReadMetadata
   public Map<Producer<String>, Producer<String>> provideReadMetadata(@Id final Producer<String> id)
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
         @Username final Producer<String> username,
         @Password final Producer<String> password)
   {
      return createRequestProducer(Method.DELETE, uri, object, headers,
            Producers.of(Entities.none()), metadata, username, password);
   }

   @Provides
   @Singleton
   @DeleteUri
   public Producer<URI> providDeleteUri(
         final Producer<Scheme> scheme,
         @DeleteHost final Producer<String> host,
         final Producer<Integer> port,
         @UriRoot final Producer<String> uriRoot,
         @Container final Producer<String> container,
         @DeleteObjectName final CachingProducer<String> object)
   {
      return createUri(scheme, host, port, uriRoot, container, object);
   }

   @Provides
   @DeleteMetadata
   public Map<Producer<String>, Producer<String>> provideDeleteMetadata(
         @Id final Producer<String> id)
   {
      return createMetadata(id);
   }

   public Map<Producer<String>, Producer<String>> createMetadata(final Producer<String> id)
   {
      final Map<Producer<String>, Producer<String>> metadata = Maps.newHashMap();
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
         b.withMetadata(Producers.of(Metadata.OBJECT_NAME.toString()), new Producer<String>()
         {
            @Override
            public String produce()
            {
               return object.getCachedValue();
            }
         });

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
      {
         b.withMetadata(Producers.of(Metadata.USERNAME.toString()), username);
         b.withMetadata(Producers.of(Metadata.PASSWORD.toString()), password);
      }

      return b.build();
   }

   private Producer<URI> createUri(
         final Producer<Scheme> scheme,
         final Producer<String> host,
         final Producer<Integer> port,
         final Producer<String> uriRoot,
         final Producer<String> container,
         final Producer<String> object)
   {
      final List<Producer<String>> path = Lists.newArrayList();
      if (uriRoot != null)
         path.add(uriRoot);
      path.add(container);
      if (object != null)
         path.add(object);

      return new UriProducer.Builder(host, path)
            .withScheme(scheme)
            .onPort(port)
            .build();
   }

   @Provides
   @Singleton
   public Map<String, ResponseBodyConsumer> provideResponseBodyConsumers()
   {
      final Map<String, ResponseBodyConsumer> consumers = Maps.newHashMap();
      consumers.put(SOH_PUT_OBJECT, new SOHWriteResponseBodyConsumer());

      return consumers;
   }
}
