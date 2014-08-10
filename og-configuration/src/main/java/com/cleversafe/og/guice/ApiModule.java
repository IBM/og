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

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Metadata;
import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Request;
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
import com.cleversafe.og.http.Bodies;
import com.cleversafe.og.http.Scheme;
import com.cleversafe.og.soh.SOHWriteResponseBodyConsumer;
import com.cleversafe.og.supplier.CachingSupplier;
import com.cleversafe.og.supplier.Suppliers;
import com.cleversafe.og.supplier.RequestSupplier;
import com.cleversafe.og.supplier.UriSupplier;
import com.cleversafe.og.util.ResponseBodyConsumer;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
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
   public Supplier<Request> provideWrite(
         @WriteUri final Supplier<URI> uri,
         @WriteObjectName final Optional<CachingSupplier<String>> object,
         @WriteHeaders final Map<Supplier<String>, Supplier<String>> headers,
         final Supplier<Body> body,
         @WriteMetadata final Map<Supplier<String>, Supplier<String>> metadata,
         @Username final Optional<Supplier<String>> username,
         @Password final Optional<Supplier<String>> password)
   {
      return createRequestSupplier(Method.PUT, uri, object, headers, body, metadata, username,
            password);
   }

   @Provides
   @Singleton
   @WriteUri
   public Supplier<URI> providWriteUri(
         final Supplier<Scheme> scheme,
         @WriteHost final Supplier<String> host,
         final Optional<Supplier<Integer>> port,
         @UriRoot final Optional<Supplier<String>> uriRoot,
         @Container final Supplier<String> container,
         @WriteObjectName final Optional<CachingSupplier<String>> object)
   {
      return createUri(scheme, host, port, uriRoot, container, object);
   }

   @Provides
   @WriteMetadata
   public Map<Supplier<String>, Supplier<String>> provideWriteMetadata(
         final Api api,
         @Id final Supplier<String> id)
   {
      final Map<Supplier<String>, Supplier<String>> metadata = createMetadata(id);
      // SOH needs to use a special response procesor to extract the returned object id
      if (Api.SOH == api)
         metadata.put(Suppliers.of(Metadata.RESPONSE_BODY_CONSUMER.toString()),
               Suppliers.of(SOH_PUT_OBJECT));
      return metadata;
   }

   @Provides
   @Singleton
   @Read
   public Supplier<Request> provideRead(
         @ReadUri final Supplier<URI> uri,
         @ReadObjectName final CachingSupplier<String> object,
         @ReadHeaders final Map<Supplier<String>, Supplier<String>> headers,
         @ReadMetadata final Map<Supplier<String>, Supplier<String>> metadata,
         @Username final Optional<Supplier<String>> username,
         @Password final Optional<Supplier<String>> password)
   {
      return createRequestSupplier(Method.GET, uri, Optional.of(object), headers,
            Suppliers.of(Bodies.none()), metadata, username, password);
   }

   @Provides
   @Singleton
   @ReadUri
   public Supplier<URI> providReadUri(
         final Supplier<Scheme> scheme,
         @ReadHost final Supplier<String> host,
         final Optional<Supplier<Integer>> port,
         @UriRoot final Optional<Supplier<String>> uriRoot,
         @Container final Supplier<String> container,
         @ReadObjectName final CachingSupplier<String> object)
   {
      return createUri(scheme, host, port, uriRoot, container, Optional.of(object));
   }

   @Provides
   @ReadMetadata
   public Map<Supplier<String>, Supplier<String>> provideReadMetadata(@Id final Supplier<String> id)
   {
      return createMetadata(id);
   }

   @Provides
   @Singleton
   @Delete
   public Supplier<Request> provideDelete(
         @DeleteUri final Supplier<URI> uri,
         @DeleteObjectName final CachingSupplier<String> object,
         @DeleteHeaders final Map<Supplier<String>, Supplier<String>> headers,
         @DeleteMetadata final Map<Supplier<String>, Supplier<String>> metadata,
         @Username final Optional<Supplier<String>> username,
         @Password final Optional<Supplier<String>> password)
   {
      return createRequestSupplier(Method.DELETE, uri, Optional.of(object), headers,
            Suppliers.of(Bodies.none()), metadata, username, password);
   }

   @Provides
   @Singleton
   @DeleteUri
   public Supplier<URI> providDeleteUri(
         final Supplier<Scheme> scheme,
         @DeleteHost final Supplier<String> host,
         final Optional<Supplier<Integer>> port,
         @UriRoot final Optional<Supplier<String>> uriRoot,
         @Container final Supplier<String> container,
         @DeleteObjectName final CachingSupplier<String> object)
   {
      return createUri(scheme, host, port, uriRoot, container, Optional.of(object));
   }

   @Provides
   @DeleteMetadata
   public Map<Supplier<String>, Supplier<String>> provideDeleteMetadata(
         @Id final Supplier<String> id)
   {
      return createMetadata(id);
   }

   public Map<Supplier<String>, Supplier<String>> createMetadata(final Supplier<String> id)
   {
      final Map<Supplier<String>, Supplier<String>> metadata = Maps.newHashMap();
      metadata.put(Suppliers.of(Metadata.REQUEST_ID.toString()), id);
      return metadata;
   }

   private Supplier<Request> createRequestSupplier(
         final Method method,
         final Supplier<URI> uri,
         final Optional<CachingSupplier<String>> object,
         final Map<Supplier<String>, Supplier<String>> headers,
         final Supplier<Body> body,
         final Map<Supplier<String>, Supplier<String>> metadata,
         final Optional<Supplier<String>> username,
         final Optional<Supplier<String>> password)
   {
      final RequestSupplier.Builder b = new RequestSupplier.Builder(Suppliers.of(method), uri);

      if (object.isPresent())
         b.withMetadata(Suppliers.of(Metadata.OBJECT_NAME.toString()), new Supplier<String>()
         {
            @Override
            public String get()
            {
               return object.get().getCachedValue();
            }
         });

      for (final Entry<Supplier<String>, Supplier<String>> header : headers.entrySet())
      {
         b.withHeader(header.getKey(), header.getValue());
      }

      b.withBody(body);

      for (final Entry<Supplier<String>, Supplier<String>> m : metadata.entrySet())
      {
         b.withMetadata(m.getKey(), m.getValue());
      }

      if (username.isPresent() && password.isPresent())
      {
         b.withMetadata(Suppliers.of(Metadata.USERNAME.toString()), username.get());
         b.withMetadata(Suppliers.of(Metadata.PASSWORD.toString()), password.get());
      }

      return b.build();
   }

   private Supplier<URI> createUri(
         final Supplier<Scheme> scheme,
         final Supplier<String> host,
         final Optional<Supplier<Integer>> port,
         final Optional<Supplier<String>> uriRoot,
         final Supplier<String> container,
         final Optional<CachingSupplier<String>> object)
   {
      final List<Supplier<String>> path = Lists.newArrayList();
      if (uriRoot.isPresent())
         path.add(uriRoot.get());
      path.add(container);
      if (object.isPresent())
         path.add(object.get());

      final UriSupplier.Builder b = new UriSupplier.Builder(host, path).withScheme(scheme);

      if (port.isPresent())
         b.onPort(port.get());

      return b.build();
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
