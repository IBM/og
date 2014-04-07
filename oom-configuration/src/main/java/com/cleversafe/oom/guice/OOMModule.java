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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.cleversafe.oom.api.ByteBufferConsumer;
import com.cleversafe.oom.api.Consumer;
import com.cleversafe.oom.api.OperationManager;
import com.cleversafe.oom.api.Producer;
import com.cleversafe.oom.client.Client;
import com.cleversafe.oom.client.JavaClient;
import com.cleversafe.oom.client.JavaClientConfiguration;
import com.cleversafe.oom.guice.annotation.DefaultObjectName;
import com.cleversafe.oom.guice.annotation.Delete;
import com.cleversafe.oom.guice.annotation.Read;
import com.cleversafe.oom.guice.annotation.Write;
import com.cleversafe.oom.http.operation.manager.SimpleOperationManager;
import com.cleversafe.oom.object.manager.ObjectManager;
import com.cleversafe.oom.object.manager.ObjectNameConsumer;
import com.cleversafe.oom.object.manager.ObjectNameProducer;
import com.cleversafe.oom.object.manager.RandomObjectPopulator;
import com.cleversafe.oom.operation.OperationType;
import com.cleversafe.oom.operation.OperationTypeMix;
import com.cleversafe.oom.operation.Request;
import com.cleversafe.oom.operation.Response;
import com.cleversafe.oom.scheduling.Scheduler;
import com.cleversafe.oom.soh.SOHWriteObjectNameConsumer;
import com.cleversafe.oom.util.ByteBufferConsumers;
import com.google.common.base.Function;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class OOMModule extends AbstractModule
{
   public OOMModule()
   {}

   @Override
   protected void configure()
   {}

   @Provides
   @Singleton
   OperationManager provideOperationManager(
         @Write final Producer<Request> write,
         @Read final Producer<Request> read,
         @Delete final Producer<Request> delete,
         final OperationTypeMix mix,
         final ObjectManager objectManager,
         final Scheduler scheduler)
   {
      final Map<OperationType, Producer<Request>> producers =
            new HashMap<OperationType, Producer<Request>>();
      final Map<Long, Request> pendingRequests = new ConcurrentHashMap<Long, Request>();
      producers.put(OperationType.WRITE, write);
      producers.put(OperationType.READ, read);
      producers.put(OperationType.DELETE, delete);

      final List<Consumer<Response>> consumers = new ArrayList<Consumer<Response>>();
      final Consumer<Response> objectConsumer =
            new ObjectNameConsumer(objectManager, pendingRequests);
      consumers.add(objectConsumer);

      // TODO account for threaded vs iops
      return new SimpleOperationManager(mix, producers, consumers, scheduler, pendingRequests);
   }

   @Provides
   @Singleton
   Client provideClient()
   {
      final JavaClientConfiguration clientConfig = new JavaClientConfiguration();
      final Function<String, ByteBufferConsumer> byteBufferConsumers =
            new Function<String, ByteBufferConsumer>()
            {

               @Override
               public ByteBufferConsumer apply(final String input)
               {
                  // TODO rework how ByteBufferConsumers are injected into a client
                  if ("soh.put_object".equals(input))
                  {
                     return new SOHWriteObjectNameConsumer();
                  }
                  return ByteBufferConsumers.noOp();
               }

            };
      return new JavaClient(clientConfig, byteBufferConsumers);
   }

   @Provides
   @Singleton
   ObjectManager provideObjectManager()
   {
      // TODO configure via test.json
      return new RandomObjectPopulator(UUID.randomUUID());
   }

   @Provides
   @Singleton
   @DefaultObjectName
   Producer<String> provideDefaultObjectName(final ObjectManager objectManager)
   {
      return new ObjectNameProducer(objectManager);
   }
}
