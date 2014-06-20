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

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.cleversafe.oom.api.ByteBufferConsumer;
import com.cleversafe.oom.api.Consumer;
import com.cleversafe.oom.api.OperationManager;
import com.cleversafe.oom.api.Producer;
import com.cleversafe.oom.cli.json.ClientConfig;
import com.cleversafe.oom.cli.json.enums.API;
import com.cleversafe.oom.client.ApacheClient;
import com.cleversafe.oom.client.Client;
import com.cleversafe.oom.guice.annotation.DefaultContainer;
import com.cleversafe.oom.guice.annotation.DefaultObjectLocation;
import com.cleversafe.oom.guice.annotation.Delete;
import com.cleversafe.oom.guice.annotation.DeleteObjectName;
import com.cleversafe.oom.guice.annotation.DeleteWeight;
import com.cleversafe.oom.guice.annotation.Read;
import com.cleversafe.oom.guice.annotation.ReadObjectName;
import com.cleversafe.oom.guice.annotation.ReadWeight;
import com.cleversafe.oom.guice.annotation.Write;
import com.cleversafe.oom.guice.annotation.WriteObjectName;
import com.cleversafe.oom.guice.annotation.WriteWeight;
import com.cleversafe.oom.http.auth.HttpAuth;
import com.cleversafe.oom.http.operation.manager.SimpleOperationManager;
import com.cleversafe.oom.object.manager.DeleteObjectNameProducer;
import com.cleversafe.oom.object.manager.ObjectManager;
import com.cleversafe.oom.object.manager.ObjectNameConsumer;
import com.cleversafe.oom.object.manager.RandomObjectPopulator;
import com.cleversafe.oom.object.manager.ReadObjectNameProducer;
import com.cleversafe.oom.object.manager.UUIDObjectNameProducer;
import com.cleversafe.oom.operation.Request;
import com.cleversafe.oom.operation.Response;
import com.cleversafe.oom.scheduling.Scheduler;
import com.cleversafe.oom.soh.SOHWriteObjectNameConsumer;
import com.cleversafe.oom.util.ByteBufferConsumers;
import com.cleversafe.oom.util.WeightedRandomChoice;
import com.cleversafe.oom.util.producer.Producers;
import com.google.common.base.Function;
import com.google.common.math.DoubleMath;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class OOMModule extends AbstractModule
{
   private final static double err = Math.pow(0.1, 6);

   public OOMModule()
   {}

   @Override
   protected void configure()
   {}

   @Provides
   @Singleton
   public OperationManager provideOperationManager(
         @Write final Producer<Request> write,
         @Read final Producer<Request> read,
         @Delete final Producer<Request> delete,
         @WriteWeight final double writeWeight,
         @ReadWeight final double readWeight,
         @DeleteWeight final double deleteWeight,
         final ObjectManager objectManager,
         final Scheduler scheduler)
   {
      final double sum = readWeight + writeWeight + deleteWeight;
      checkArgument(DoubleMath.fuzzyEquals(sum, 100.0, err),
            "Sum of percentages must be 100.0 [%s]", sum);

      final WeightedRandomChoice<Producer<Request>> wrc =
            new WeightedRandomChoice<Producer<Request>>();
      if (writeWeight > 0.0)
         wrc.addChoice(write, writeWeight);
      if (readWeight > 0.0)
         wrc.addChoice(read, readWeight);
      if (deleteWeight > 0.0)
         wrc.addChoice(delete, deleteWeight);

      final Map<Long, Request> pendingRequests = new ConcurrentHashMap<Long, Request>();
      final List<Consumer<Response>> consumers = new ArrayList<Consumer<Response>>();
      final Consumer<Response> objectConsumer =
            new ObjectNameConsumer(objectManager, pendingRequests);
      consumers.add(objectConsumer);

      // TODO account for threaded vs iops
      return new SimpleOperationManager(Producers.of(wrc), consumers, scheduler, pendingRequests);
   }

   @Provides
   @Singleton
   public Client provideClient(final ClientConfig clientConfig, final HttpAuth auth)
   {
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
      return ApacheClient.custom()
            .withAuth(auth)
            .withConnectTimeout(clientConfig.getConnectTimeout())
            .withSoTimeout(clientConfig.getSoTimeout())
            .usingSoReuseAddress(clientConfig.isSoReuseAddress())
            .withSoLinger(clientConfig.getSoLinger())
            .usingSoKeepAlive(clientConfig.isSoKeepAlive())
            .usingTcpNoDelay(clientConfig.isTcpNoDelay())
            .usingChunkedEncoding(clientConfig.isChunkedEncoding())
            .usingExpectContinue(clientConfig.isExpectContinue())
            .withWaitForContinue(clientConfig.getWaitForContinue())
            .withByteBufferConsumers(byteBufferConsumers)
            .build();
   }

   @Provides
   @Singleton
   public ObjectManager provideObjectManager(
         @DefaultObjectLocation final String objectLocation,
         @DefaultContainer final Producer<String> container,
         final API api)
   {
      // FIXME this naming scheme will break unless @DefaultContainer is a constant producer
      final String aContainer = container.produce();
      return new RandomObjectPopulator(UUID.randomUUID(), objectLocation, aContainer + "-"
            + api.toString().toLowerCase());
   }

   @Provides
   @Singleton
   @WriteObjectName
   public Producer<String> provideWriteObjectName()
   {
      return new UUIDObjectNameProducer();
   }

   @Provides
   @Singleton
   @ReadObjectName
   public Producer<String> provideReadObjectName(final ObjectManager objectManager)
   {
      return new ReadObjectNameProducer(objectManager);
   }

   @Provides
   @Singleton
   @DeleteObjectName
   public Producer<String> provideDeleteObjectName(final ObjectManager objectManager)
   {
      return new DeleteObjectNameProducer(objectManager);
   }
}
