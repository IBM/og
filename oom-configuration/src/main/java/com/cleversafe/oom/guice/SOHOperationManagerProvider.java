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
// Date: Mar 27, 2014
// ---------------------

package com.cleversafe.oom.guice;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.cleversafe.oom.api.Consumer;
import com.cleversafe.oom.api.Producer;
import com.cleversafe.oom.guice.annotation.DefaultContainer;
import com.cleversafe.oom.guice.annotation.DefaultEntity;
import com.cleversafe.oom.guice.annotation.DefaultHeaders;
import com.cleversafe.oom.guice.annotation.DefaultHost;
import com.cleversafe.oom.guice.annotation.DefaultId;
import com.cleversafe.oom.guice.annotation.DefaultMetaData;
import com.cleversafe.oom.guice.annotation.DefaultPort;
import com.cleversafe.oom.guice.annotation.DefaultQueryParams;
import com.cleversafe.oom.guice.annotation.DefaultScheme;
import com.cleversafe.oom.http.Scheme;
import com.cleversafe.oom.http.producer.RequestProducer;
import com.cleversafe.oom.http.producer.URLProducer;
import com.cleversafe.oom.object.manager.ObjectManager;
import com.cleversafe.oom.object.manager.ObjectNameProcessor;
import com.cleversafe.oom.operation.Entity;
import com.cleversafe.oom.operation.EntityType;
import com.cleversafe.oom.operation.Method;
import com.cleversafe.oom.operation.OperationType;
import com.cleversafe.oom.operation.OperationTypeMix;
import com.cleversafe.oom.operation.Request;
import com.cleversafe.oom.operation.Response;
import com.cleversafe.oom.scheduling.Scheduler;
import com.cleversafe.oom.soh.SOHOperationManager;
import com.cleversafe.oom.util.Entities;
import com.cleversafe.oom.util.Pair;
import com.cleversafe.oom.util.producer.Producers;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class SOHOperationManagerProvider implements Provider<SOHOperationManager>
{
   private final OperationTypeMix mix;
   private final Producer<Long> id;
   private final Producer<Scheme> scheme;
   private final Producer<String> host;
   private final Producer<Integer> port;
   private final Producer<String> container;
   private final Producer<Map<String, String>> queryParams;
   private final List<Producer<Pair<String, String>>> headers;
   private final Producer<Entity> entity;
   private final Producer<Map<String, String>> metadata;
   private final Scheduler scheduler;
   private final ObjectManager objectManager;
   private final Map<Long, Request> pendingRequests;
   private final Producer<String> object;

   @Inject
   public SOHOperationManagerProvider(
         final OperationTypeMix mix,
         @DefaultId
         final Producer<Long> id,
         @DefaultScheme
         final Producer<Scheme> scheme,
         @DefaultHost
         final Producer<String> host,
         @DefaultPort
         final Producer<Integer> port,
         @DefaultContainer
         final Producer<String> container,
         @DefaultQueryParams
         final Producer<Map<String, String>> queryParams,
         @DefaultHeaders
         final List<Producer<Pair<String, String>>> headers,
         @DefaultEntity
         final Producer<Entity> entity,
         @DefaultMetaData
         final Producer<Map<String, String>> metadata,
         final Scheduler scheduler,
         final ObjectManager objectManager)
   {
      this.mix = mix;
      this.id = id;
      this.scheme = scheme;
      this.host = host;
      this.port = port;
      this.container = container;
      this.queryParams = queryParams;
      this.headers = headers;
      this.entity = entity;
      this.metadata = metadata;
      this.scheduler = scheduler;
      this.objectManager = objectManager;
      this.pendingRequests = new ConcurrentHashMap<Long, Request>();
      this.object = new ObjectNameProcessor(this.objectManager, this.pendingRequests);
   }

   @Override
   public SOHOperationManager get()
   {
      final Map<OperationType, Producer<Request>> producers =
            new HashMap<OperationType, Producer<Request>>();
      producers.put(OperationType.WRITE, createSOHWriteProducer());
      producers.put(OperationType.READ, createSOHReadProducer());
      producers.put(OperationType.DELETE, createSOHDeleteProducer());

      final List<Consumer<Response>> consumers = new ArrayList<Consumer<Response>>();
      // TODO remove cast, possible need for Processor interface (Producer + Consumer)
      consumers.add((Consumer<Response>) this.object);

      // TODO account for threaded vs iops
      return new SOHOperationManager(this.mix, producers, consumers, this.scheduler,
            this.pendingRequests);
   }

   private Producer<Request> createSOHWriteProducer()
   {
      final List<Producer<String>> parts = new ArrayList<Producer<String>>();
      parts.add(this.container);
      final Producer<URL> writeURL =
            new URLProducer(this.scheme, this.host, this.port, parts, this.queryParams);

      return new RequestProducer(this.id,
            Producers.of("soh.put_object"),
            Producers.of(Method.PUT),
            writeURL,
            this.headers,
            this.entity,
            this.metadata);
   }

   private Producer<Request> createSOHReadProducer()
   {
      final List<Producer<String>> parts = new ArrayList<Producer<String>>();
      parts.add(this.container);
      parts.add(this.object);
      final Producer<URL> readURL =
            new URLProducer(this.scheme, this.host, this.port, parts, this.queryParams);

      return new RequestProducer(this.id,
            Producers.of("soh.get_object"),
            Producers.of(Method.GET),
            readURL,
            this.headers,
            Producers.of(Entities.of(EntityType.NONE, 0)),
            this.metadata);
   }

   private Producer<Request> createSOHDeleteProducer()
   {
      final List<Producer<String>> parts = new ArrayList<Producer<String>>();
      parts.add(this.container);
      parts.add(this.object);
      final Producer<URL> deleteURL =
            new URLProducer(this.scheme, this.host, this.port, parts, this.queryParams);

      return new RequestProducer(this.id,
            Producers.of("soh.delete_object"),
            Producers.of(Method.DELETE),
            deleteURL,
            this.headers,
            Producers.of(Entities.of(EntityType.NONE, 0)),
            this.metadata);
   }
}
