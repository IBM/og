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

package com.cleversafe.og.guice;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.cleversafe.og.api.ByteBufferConsumer;
import com.cleversafe.og.api.Consumer;
import com.cleversafe.og.api.OperationManager;
import com.cleversafe.og.api.Producer;
import com.cleversafe.og.cli.json.ClientConfig;
import com.cleversafe.og.cli.json.StoppingConditionsConfig;
import com.cleversafe.og.client.ApacheClient;
import com.cleversafe.og.client.Client;
import com.cleversafe.og.guice.annotation.DefaultContainer;
import com.cleversafe.og.guice.annotation.DefaultObjectLocation;
import com.cleversafe.og.guice.annotation.Delete;
import com.cleversafe.og.guice.annotation.DeleteObjectName;
import com.cleversafe.og.guice.annotation.DeleteWeight;
import com.cleversafe.og.guice.annotation.Read;
import com.cleversafe.og.guice.annotation.ReadObjectName;
import com.cleversafe.og.guice.annotation.ReadWeight;
import com.cleversafe.og.guice.annotation.Write;
import com.cleversafe.og.guice.annotation.WriteObjectName;
import com.cleversafe.og.guice.annotation.WriteWeight;
import com.cleversafe.og.http.auth.HttpAuth;
import com.cleversafe.og.http.util.ApiType;
import com.cleversafe.og.object.manager.DeleteObjectNameProducer;
import com.cleversafe.og.object.manager.ObjectManager;
import com.cleversafe.og.object.manager.RandomObjectPopulator;
import com.cleversafe.og.object.manager.ReadObjectNameProducer;
import com.cleversafe.og.object.manager.UUIDObjectNameProducer;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.operation.Response;
import com.cleversafe.og.scheduling.Scheduler;
import com.cleversafe.og.soh.SOHWriteByteBufferConsumer;
import com.cleversafe.og.statistic.Counter;
import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.test.LoadTest;
import com.cleversafe.og.test.RuntimeListener;
import com.cleversafe.og.test.StatisticsListener;
import com.cleversafe.og.test.StatusCodeListener;
import com.cleversafe.og.test.operation.manager.SimpleOperationManager;
import com.cleversafe.og.util.ByteBufferConsumers;
import com.cleversafe.og.util.OperationType;
import com.cleversafe.og.util.WeightedRandomChoice;
import com.cleversafe.og.util.producer.Producers;
import com.google.common.base.Function;
import com.google.common.eventbus.EventBus;
import com.google.common.math.DoubleMath;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class OGModule extends AbstractModule
{
   private final static double err = Math.pow(0.1, 6);

   public OGModule()
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
         final Map<Long, Request> pendingRequests,
         final Scheduler scheduler,
         final List<Consumer<Response>> consumers,
         final Statistics stats)
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

      return new SimpleOperationManager(Producers.of(wrc), consumers, scheduler, pendingRequests,
            stats);
   }

   @Provides
   @Singleton
   public Map<Long, Request> providePendingRequests()
   {
      return new ConcurrentHashMap<Long, Request>();
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
                     return new SOHWriteByteBufferConsumer();
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
         final ApiType api)
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

   // TODO refactor stopping condition creation
   @Provides
   @Singleton
   LoadTest provideLoadTest(
         final OperationManager operationManager,
         final Client client,
         final StoppingConditionsConfig stoppingConditions,
         final EventBus eventBus)
   {
      final ExecutorService executorService = Executors.newCachedThreadPool();
      final LoadTest test = new LoadTest(operationManager, client, executorService);
      final List<StatisticsListener> statsListeners = new ArrayList<StatisticsListener>();
      final List<StatusCodeListener> statusCodeListeners = new ArrayList<StatusCodeListener>();

      if (stoppingConditions.getOperations() > 0)
         statsListeners.add(new StatisticsListener(test, OperationType.ALL, Counter.OPERATIONS,
               stoppingConditions.getOperations()));

      // RuntimeListener does not need to be registered with the event bus
      if (stoppingConditions.getRuntime() > 0)
         new RuntimeListener(test, stoppingConditions.getRuntime(),
               stoppingConditions.getRuntimeUnit());

      final Map<Integer, Integer> scMap = stoppingConditions.getStatusCodes();
      for (final Entry<Integer, Integer> sc : scMap.entrySet())
      {
         if (sc.getValue() > 0)
            statusCodeListeners.add(new StatusCodeListener(test, OperationType.ALL, sc.getKey(),
                  sc.getValue()));
      }

      for (final StatisticsListener listener : statsListeners)
      {
         eventBus.register(listener);
      }

      for (final StatusCodeListener listener : statusCodeListeners)
      {
         eventBus.register(listener);
      }

      return test;
   }

   @Provides
   @Singleton
   public EventBus provideEventBus()
   {
      return new EventBus();
   }

   @Provides
   @Singleton
   public Statistics provideStatistics(final EventBus eventBus)
   {
      return new Statistics(eventBus);
   }
}
