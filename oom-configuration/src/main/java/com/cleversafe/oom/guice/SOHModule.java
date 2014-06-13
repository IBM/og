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

package com.cleversafe.oom.guice;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.cleversafe.oom.api.Producer;
import com.cleversafe.oom.guice.annotation.DefaultEntity;
import com.cleversafe.oom.guice.annotation.DefaultId;
import com.cleversafe.oom.guice.annotation.DefaultMetaData;
import com.cleversafe.oom.guice.annotation.DefaultObjectName;
import com.cleversafe.oom.guice.annotation.DefaultUriRoot;
import com.cleversafe.oom.guice.annotation.Delete;
import com.cleversafe.oom.guice.annotation.DeleteContainer;
import com.cleversafe.oom.guice.annotation.DeleteHeaders;
import com.cleversafe.oom.guice.annotation.DeleteHost;
import com.cleversafe.oom.guice.annotation.DeletePort;
import com.cleversafe.oom.guice.annotation.DeleteQueryParams;
import com.cleversafe.oom.guice.annotation.DeleteScheme;
import com.cleversafe.oom.guice.annotation.Read;
import com.cleversafe.oom.guice.annotation.ReadContainer;
import com.cleversafe.oom.guice.annotation.ReadHeaders;
import com.cleversafe.oom.guice.annotation.ReadHost;
import com.cleversafe.oom.guice.annotation.ReadPort;
import com.cleversafe.oom.guice.annotation.ReadQueryParams;
import com.cleversafe.oom.guice.annotation.ReadScheme;
import com.cleversafe.oom.guice.annotation.Write;
import com.cleversafe.oom.guice.annotation.WriteContainer;
import com.cleversafe.oom.guice.annotation.WriteHeaders;
import com.cleversafe.oom.guice.annotation.WriteHost;
import com.cleversafe.oom.guice.annotation.WritePort;
import com.cleversafe.oom.guice.annotation.WriteQueryParams;
import com.cleversafe.oom.guice.annotation.WriteScheme;
import com.cleversafe.oom.http.Scheme;
import com.cleversafe.oom.http.producer.RequestProducer;
import com.cleversafe.oom.http.producer.URIProducer;
import com.cleversafe.oom.operation.Entity;
import com.cleversafe.oom.operation.EntityType;
import com.cleversafe.oom.operation.Method;
import com.cleversafe.oom.operation.Request;
import com.cleversafe.oom.util.Entities;
import com.cleversafe.oom.util.Pair;
import com.cleversafe.oom.util.producer.Producers;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class SOHModule extends AbstractModule
{
   @Override
   protected void configure()
   {}

   @Provides
   @Singleton
   @Write
   private Producer<Request> provideWrite(
         @DefaultId final Producer<Long> id,
         @WriteScheme final Producer<Scheme> scheme,
         @WriteHost final Producer<String> host,
         @WritePort final Producer<Integer> port,
         @DefaultUriRoot final Producer<String> uriRoot,
         @WriteContainer final Producer<String> container,
         @WriteQueryParams final Producer<Map<String, String>> queryParams,
         @WriteHeaders final List<Producer<Pair<String, String>>> headers,
         @DefaultEntity final Producer<Entity> entity,
         @DefaultMetaData final Producer<Map<String, String>> metadata)
   {
      final List<Producer<String>> parts = new ArrayList<Producer<String>>();
      addUriRoot(parts, uriRoot);
      parts.add(container);
      final Producer<URI> writeURI = new URIProducer.Builder()
            .withScheme(scheme)
            .toHost(host)
            .onPort(port)
            .atPath(parts)
            .withQueryParams(queryParams)
            .build();

      return new RequestProducer(id,
            Producers.of("soh.put_object"),
            Producers.of(Method.PUT),
            writeURI,
            headers,
            entity,
            metadata);
   }

   @Provides
   @Singleton
   @Read
   private Producer<Request> provideRead(
         @DefaultId final Producer<Long> id,
         @ReadScheme final Producer<Scheme> scheme,
         @ReadHost final Producer<String> host,
         @ReadPort final Producer<Integer> port,
         @DefaultUriRoot final Producer<String> uriRoot,
         @ReadContainer final Producer<String> container,
         @DefaultObjectName final Producer<String> object,
         @ReadQueryParams final Producer<Map<String, String>> queryParams,
         @ReadHeaders final List<Producer<Pair<String, String>>> headers,
         @DefaultMetaData final Producer<Map<String, String>> metadata)
   {
      final List<Producer<String>> parts = new ArrayList<Producer<String>>();
      addUriRoot(parts, uriRoot);
      parts.add(container);
      parts.add(object);
      final Producer<URI> readURI = new URIProducer.Builder()
            .withScheme(scheme)
            .toHost(host)
            .onPort(port)
            .atPath(parts)
            .withQueryParams(queryParams)
            .build();

      return new RequestProducer(id,
            Producers.of("soh.get_object"),
            Producers.of(Method.GET),
            readURI,
            headers,
            Producers.of(Entities.of(EntityType.NONE, 0)),
            metadata);
   }

   @Provides
   @Singleton
   @Delete
   private Producer<Request> provideDelete(
         @DefaultId final Producer<Long> id,
         @DeleteScheme final Producer<Scheme> scheme,
         @DeleteHost final Producer<String> host,
         @DeletePort final Producer<Integer> port,
         @DefaultUriRoot final Producer<String> uriRoot,
         @DeleteContainer final Producer<String> container,
         @DefaultObjectName final Producer<String> object,
         @DeleteQueryParams final Producer<Map<String, String>> queryParams,
         @DeleteHeaders final List<Producer<Pair<String, String>>> headers,
         @DefaultMetaData final Producer<Map<String, String>> metadata)
   {
      final List<Producer<String>> parts = new ArrayList<Producer<String>>();
      addUriRoot(parts, uriRoot);
      parts.add(container);
      parts.add(object);
      final Producer<URI> deleteURI = new URIProducer.Builder()
            .withScheme(scheme)
            .toHost(host)
            .onPort(port)
            .atPath(parts)
            .withQueryParams(queryParams)
            .build();

      return new RequestProducer(id,
            Producers.of("soh.delete_object"),
            Producers.of(Method.DELETE),
            deleteURI,
            headers,
            Producers.of(Entities.of(EntityType.NONE, 0)),
            metadata);
   }

   // TODO better way to do this? Maybe uriRoot should never be null and/or should be propagated
   // all the way to URIProducer
   private void addUriRoot(final List<Producer<String>> parts, final Producer<String> uriRoot)
   {
      if (uriRoot != null)
         parts.add(uriRoot);
   }
}
