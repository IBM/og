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

import com.cleversafe.og.api.Producer;
import com.cleversafe.og.guice.annotation.DefaultContainer;
import com.cleversafe.og.guice.annotation.DefaultEntity;
import com.cleversafe.og.guice.annotation.DefaultId;
import com.cleversafe.og.guice.annotation.DefaultPort;
import com.cleversafe.og.guice.annotation.DefaultQueryParams;
import com.cleversafe.og.guice.annotation.DefaultScheme;
import com.cleversafe.og.guice.annotation.DefaultUriRoot;
import com.cleversafe.og.guice.annotation.Delete;
import com.cleversafe.og.guice.annotation.DeleteHeaders;
import com.cleversafe.og.guice.annotation.DeleteHost;
import com.cleversafe.og.guice.annotation.DeleteObjectName;
import com.cleversafe.og.guice.annotation.Read;
import com.cleversafe.og.guice.annotation.ReadHeaders;
import com.cleversafe.og.guice.annotation.ReadHost;
import com.cleversafe.og.guice.annotation.ReadObjectName;
import com.cleversafe.og.guice.annotation.Write;
import com.cleversafe.og.guice.annotation.WriteHeaders;
import com.cleversafe.og.guice.annotation.WriteHost;
import com.cleversafe.og.http.Scheme;
import com.cleversafe.og.http.producer.RequestProducer;
import com.cleversafe.og.http.producer.URIProducer;
import com.cleversafe.og.operation.Entity;
import com.cleversafe.og.operation.EntityType;
import com.cleversafe.og.operation.MetaDataConstants;
import com.cleversafe.og.operation.Method;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.util.Entities;
import com.cleversafe.og.util.Pair;
import com.cleversafe.og.util.producer.Producers;
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
   public Producer<Request> provideWrite(
         @DefaultId final Producer<Long> id,
         @DefaultScheme final Producer<Scheme> scheme,
         @WriteHost final Producer<String> host,
         @DefaultPort final Producer<Integer> port,
         @DefaultUriRoot final Producer<String> uriRoot,
         @DefaultContainer final Producer<String> container,
         @DefaultQueryParams final Producer<Map<String, String>> queryParams,
         @WriteHeaders final List<Producer<Pair<String, String>>> headers,
         @DefaultEntity final Producer<Entity> entity)
   {
      final List<Producer<String>> parts = new ArrayList<Producer<String>>();
      addUriRoot(parts, uriRoot);
      parts.add(container);
      final Producer<URI> writeURI = URIProducer.custom()
            .withScheme(scheme)
            .toHost(host)
            .onPort(port)
            .atPath(parts)
            .withQueryParams(queryParams)
            .build();
      final Map<String, String> metadata = new HashMap<String, String>();
      metadata.put(MetaDataConstants.RESPONSE_BODY_PROCESSOR.toString(), "soh.put_object");

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
         @DefaultId final Producer<Long> id,
         @DefaultScheme final Producer<Scheme> scheme,
         @ReadHost final Producer<String> host,
         @DefaultPort final Producer<Integer> port,
         @DefaultUriRoot final Producer<String> uriRoot,
         @DefaultContainer final Producer<String> container,
         @ReadObjectName final Producer<String> object,
         @DefaultQueryParams final Producer<Map<String, String>> queryParams,
         @ReadHeaders final List<Producer<Pair<String, String>>> headers)
   {
      final List<Producer<String>> parts = new ArrayList<Producer<String>>();
      addUriRoot(parts, uriRoot);
      parts.add(container);
      parts.add(object);
      final Producer<URI> readURI = URIProducer.custom()
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
         @DefaultId final Producer<Long> id,
         @DefaultScheme final Producer<Scheme> scheme,
         @DeleteHost final Producer<String> host,
         @DefaultPort final Producer<Integer> port,
         @DefaultUriRoot final Producer<String> uriRoot,
         @DefaultContainer final Producer<String> container,
         @DeleteObjectName final Producer<String> object,
         @DefaultQueryParams final Producer<Map<String, String>> queryParams,
         @DeleteHeaders final List<Producer<Pair<String, String>>> headers)
   {
      final List<Producer<String>> parts = new ArrayList<Producer<String>>();
      addUriRoot(parts, uriRoot);
      parts.add(container);
      parts.add(object);
      final Producer<URI> deleteURI = URIProducer.custom()
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

   // TODO better way to do this? Maybe uriRoot should never be null and/or should be propagated
   // all the way to URIProducer
   private void addUriRoot(final List<Producer<String>> parts, final Producer<String> uriRoot)
   {
      if (uriRoot != null)
         parts.add(uriRoot);
   }
}
