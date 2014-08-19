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

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.guice.annotation.Container;
import com.cleversafe.og.guice.annotation.Delete;
import com.cleversafe.og.guice.annotation.DeleteHeaders;
import com.cleversafe.og.guice.annotation.DeleteHost;
import com.cleversafe.og.guice.annotation.DeleteObjectName;
import com.cleversafe.og.guice.annotation.DeleteUri;
import com.cleversafe.og.guice.annotation.Password;
import com.cleversafe.og.guice.annotation.Read;
import com.cleversafe.og.guice.annotation.ReadHeaders;
import com.cleversafe.og.guice.annotation.ReadHost;
import com.cleversafe.og.guice.annotation.ReadObjectName;
import com.cleversafe.og.guice.annotation.ReadUri;
import com.cleversafe.og.guice.annotation.UriRoot;
import com.cleversafe.og.guice.annotation.Username;
import com.cleversafe.og.guice.annotation.Write;
import com.cleversafe.og.guice.annotation.WriteHeaders;
import com.cleversafe.og.guice.annotation.WriteHost;
import com.cleversafe.og.guice.annotation.WriteObjectName;
import com.cleversafe.og.guice.annotation.WriteUri;
import com.cleversafe.og.http.Api;
import com.cleversafe.og.http.Bodies;
import com.cleversafe.og.http.Headers;
import com.cleversafe.og.http.ResponseBodyConsumer;
import com.cleversafe.og.http.Scheme;
import com.cleversafe.og.soh.SOHWriteResponseBodyConsumer;
import com.cleversafe.og.supplier.CachingSupplier;
import com.cleversafe.og.supplier.RequestSupplier;
import com.cleversafe.og.supplier.Suppliers;
import com.cleversafe.og.supplier.UriSupplier;
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
         final Api api,
         @WriteUri final Supplier<URI> uri,
         @WriteObjectName final CachingSupplier<String> object,
         @WriteHeaders final Map<Supplier<String>, Supplier<String>> headers,
         final Supplier<Body> body,
         @Username final Supplier<String> username,
         @Password final Supplier<String> password)
   {
      checkNotNull(api);
      // SOH needs to use a special response consumer to extract the returned object id
      if (Api.SOH == api)
         headers.put(Suppliers.of(Headers.X_OG_RESPONSE_BODY_CONSUMER),
               Suppliers.of(SOH_PUT_OBJECT));

      return createRequestSupplier(Method.PUT, uri, object, headers, body, username, password);
   }

   @Provides
   @Singleton
   @Read
   public Supplier<Request> provideRead(
         @ReadUri final Supplier<URI> uri,
         @ReadObjectName final CachingSupplier<String> object,
         @ReadHeaders final Map<Supplier<String>, Supplier<String>> headers,
         @Username final Supplier<String> username,
         @Password final Supplier<String> password)
   {
      return createRequestSupplier(Method.GET, uri, object, headers, Suppliers.of(Bodies.none()),
            username, password);
   }

   @Provides
   @Singleton
   @Delete
   public Supplier<Request> provideDelete(
         @DeleteUri final Supplier<URI> uri,
         @DeleteObjectName final CachingSupplier<String> object,
         @DeleteHeaders final Map<Supplier<String>, Supplier<String>> headers,
         @Username final Supplier<String> username,
         @Password final Supplier<String> password)
   {
      return createRequestSupplier(Method.DELETE, uri, object, headers,
            Suppliers.of(Bodies.none()), username, password);
   }

   private Supplier<Request> createRequestSupplier(
         final Method method,
         final Supplier<URI> uri,
         final CachingSupplier<String> object,
         final Map<Supplier<String>, Supplier<String>> headers,
         final Supplier<Body> body,
         final Supplier<String> username,
         final Supplier<String> password)
   {
      checkNotNull(method);
      checkNotNull(uri);
      checkNotNull(headers);
      checkNotNull(body);

      final RequestSupplier.Builder b = new RequestSupplier.Builder(Suppliers.of(method), uri);

      if (object != null)
         b.withHeader(Suppliers.of(Headers.X_OG_OBJECT_NAME), new Supplier<String>()
         {
            @Override
            public String get()
            {
               return object.getCachedValue();
            }
         });

      for (final Entry<Supplier<String>, Supplier<String>> header : headers.entrySet())
      {
         b.withHeader(header.getKey(), header.getValue());
      }

      b.withBody(body);

      if (username != null && password != null)
      {
         b.withHeader(Suppliers.of(Headers.X_OG_USERNAME), username);
         b.withHeader(Suppliers.of(Headers.X_OG_PASSWORD), password);
      }

      return b.build();
   }

   @Provides
   @Singleton
   @WriteUri
   public Supplier<URI> providWriteUri(
         final Supplier<Scheme> scheme,
         @WriteHost final Supplier<String> host,
         final Supplier<Integer> port,
         @UriRoot final Supplier<String> uriRoot,
         @Container final Supplier<String> container,
         @WriteObjectName final CachingSupplier<String> object)
   {
      return createUri(scheme, host, port, uriRoot, container, object);
   }

   @Provides
   @Singleton
   @ReadUri
   public Supplier<URI> providReadUri(
         final Supplier<Scheme> scheme,
         @ReadHost final Supplier<String> host,
         final Supplier<Integer> port,
         @UriRoot final Supplier<String> uriRoot,
         @Container final Supplier<String> container,
         @ReadObjectName final CachingSupplier<String> object)
   {
      return createUri(scheme, host, port, uriRoot, container, object);
   }

   @Provides
   @Singleton
   @DeleteUri
   public Supplier<URI> providDeleteUri(
         final Supplier<Scheme> scheme,
         @DeleteHost final Supplier<String> host,
         final Supplier<Integer> port,
         @UriRoot final Supplier<String> uriRoot,
         @Container final Supplier<String> container,
         @DeleteObjectName final CachingSupplier<String> object)
   {
      return createUri(scheme, host, port, uriRoot, container, object);
   }

   private Supplier<URI> createUri(
         final Supplier<Scheme> scheme,
         final Supplier<String> host,
         final Supplier<Integer> port,
         final Supplier<String> uriRoot,
         final Supplier<String> container,
         final CachingSupplier<String> object)
   {
      final List<Supplier<String>> path = Lists.newArrayList();
      if (uriRoot != null)
         path.add(uriRoot);
      path.add(container);
      if (object != null)
         path.add(object);

      final UriSupplier.Builder b = new UriSupplier.Builder(host, path).withScheme(scheme);

      if (port != null)
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
