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

import com.cleversafe.og.client.ApacheClient;
import com.cleversafe.og.client.Client;
import com.cleversafe.og.consumer.ObjectNameConsumer;
import com.cleversafe.og.consumer.ReadObjectNameConsumer;
import com.cleversafe.og.consumer.WriteObjectNameConsumer;
import com.cleversafe.og.guice.annotation.Delete;
import com.cleversafe.og.guice.annotation.DeleteObjectName;
import com.cleversafe.og.guice.annotation.DeleteWeight;
import com.cleversafe.og.guice.annotation.Read;
import com.cleversafe.og.guice.annotation.ReadObjectName;
import com.cleversafe.og.guice.annotation.ReadWeight;
import com.cleversafe.og.guice.annotation.TestObjectFileLocation;
import com.cleversafe.og.guice.annotation.TestObjectFileName;
import com.cleversafe.og.guice.annotation.Write;
import com.cleversafe.og.guice.annotation.WriteObjectName;
import com.cleversafe.og.guice.annotation.WriteWeight;
import com.cleversafe.og.http.auth.HttpAuth;
import com.cleversafe.og.http.util.Api;
import com.cleversafe.og.http.util.HttpUtil;
import com.cleversafe.og.json.ClientConfig;
import com.cleversafe.og.json.StoppingConditionsConfig;
import com.cleversafe.og.object.manager.ObjectManager;
import com.cleversafe.og.object.manager.RandomObjectPopulator;
import com.cleversafe.og.object.producer.DeleteObjectNameProducer;
import com.cleversafe.og.object.producer.ReadObjectNameProducer;
import com.cleversafe.og.object.producer.UUIDObjectNameProducer;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.operation.manager.OperationManager;
import com.cleversafe.og.scheduling.Scheduler;
import com.cleversafe.og.statistic.Counter;
import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.test.LoadTest;
import com.cleversafe.og.test.condition.CounterCondition;
import com.cleversafe.og.test.condition.RuntimeCondition;
import com.cleversafe.og.test.condition.StatusCodeCondition;
import com.cleversafe.og.test.condition.TestCondition;
import com.cleversafe.og.test.operation.manager.SimpleOperationManager;
import com.cleversafe.og.util.Operation;
import com.cleversafe.og.util.ResponseBodyConsumer;
import com.cleversafe.og.util.Version;
import com.cleversafe.og.util.producer.CachingProducer;
import com.cleversafe.og.util.producer.Producer;
import com.cleversafe.og.util.producer.RandomChoiceProducer;
import com.google.common.eventbus.EventBus;
import com.google.common.math.DoubleMath;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class OGModule extends AbstractModule
{
   private static final double ERR = Math.pow(0.1, 6);

   @Override
   protected void configure()
   {
      bind(OperationManager.class).to(SimpleOperationManager.class).in(Singleton.class);
      bind(EventBus.class).in(Singleton.class);
   }

   @Provides
   @Singleton
   public Statistics provideStatistics(final EventBus eventBus)
   {
      final Statistics stats = new Statistics();
      eventBus.register(stats);
      return stats;
   }

   @Provides
   @Singleton
   public Producer<Producer<Request>> provideRequestProducer(
         @Write final Producer<Request> write,
         @Read final Producer<Request> read,
         @Delete final Producer<Request> delete,
         @WriteWeight final double writeWeight,
         @ReadWeight final double readWeight,
         @DeleteWeight final double deleteWeight)
   {
      final double sum = readWeight + writeWeight + deleteWeight;
      checkArgument(DoubleMath.fuzzyEquals(sum, 100.0, ERR),
            "Sum of percentages must be 100.0 [%s]", sum);

      final RandomChoiceProducer.Builder<Producer<Request>> wrc =
            new RandomChoiceProducer.Builder<Producer<Request>>();
      if (writeWeight > 0.0)
         wrc.withChoice(write, writeWeight);
      if (readWeight > 0.0)
         wrc.withChoice(read, readWeight);
      if (deleteWeight > 0.0)
         wrc.withChoice(delete, deleteWeight);

      return wrc.build();
   }

   @Provides
   @Singleton
   LoadTest provideLoadTest(
         final EventBus eventBus,
         final OperationManager operationManager,
         final Client client,
         final Scheduler scheduler,
         final Statistics stats,
         final StoppingConditionsConfig config)
   {
      final List<TestCondition> conditions = new ArrayList<TestCondition>();

      if (config.getOperations() > 0)
         conditions.add(new CounterCondition(Operation.ALL, Counter.OPERATIONS,
               config.getOperations()));

      if (config.getAborts() > 0)
         conditions.add(new CounterCondition(Operation.ALL, Counter.ABORTS, config.getAborts()));

      final Map<Integer, Integer> scMap = config.getStatusCodes();
      for (final Entry<Integer, Integer> sc : scMap.entrySet())
      {
         if (sc.getValue() > 0)
            conditions.add(new StatusCodeCondition(Operation.ALL, sc.getKey(), sc.getValue()));
      }

      final LoadTest test =
            new LoadTest(eventBus, operationManager, client, scheduler, stats, conditions);

      // have to create this condition after LoadTest because it triggers LoadTest.stopTest()
      if (config.getRuntime() > 0)
         new RuntimeCondition(test, config.getRuntime(), config.getRuntimeUnit());

      return test;
   }

   @Provides
   @Singleton
   public Client provideClient(
         final ClientConfig clientConfig,
         final HttpAuth authentication,
         final Map<String, ResponseBodyConsumer> responseBodyConsumers)
   {
      return new ApacheClient.Builder(responseBodyConsumers)
            .withConnectTimeout(clientConfig.getConnectTimeout())
            .withSoTimeout(clientConfig.getSoTimeout())
            .usingSoReuseAddress(clientConfig.isSoReuseAddress())
            .withSoLinger(clientConfig.getSoLinger())
            .usingSoKeepAlive(clientConfig.isSoKeepAlive())
            .usingTcpNoDelay(clientConfig.isTcpNoDelay())
            .usingChunkedEncoding(clientConfig.isChunkedEncoding())
            .usingExpectContinue(clientConfig.isExpectContinue())
            .withWaitForContinue(clientConfig.getWaitForContinue())
            .withAuthentication(authentication)
            .withUserAgent(Version.displayVersion())
            .withWriteThroughput(clientConfig.getWriteThroughput())
            .withReadThroughput(clientConfig.getReadThroughput())
            .build();
   }

   @Provides
   @Singleton
   public ObjectManager provideObjectManager(
         @TestObjectFileLocation final String objectFileLocation,
         @TestObjectFileName final String objectFileName)
   {
      return new RandomObjectPopulator(UUID.randomUUID(), objectFileLocation, objectFileName);
   }

   @Provides
   @Singleton
   @WriteObjectName
   public CachingProducer<String> provideWriteObjectName(final Api api)
   {
      if (Api.SOH == api)
         return null;
      return new CachingProducer<String>(new UUIDObjectNameProducer());
   }

   @Provides
   @Singleton
   @ReadObjectName
   public CachingProducer<String> provideReadObjectName(final ObjectManager objectManager)
   {
      return new CachingProducer<String>(new ReadObjectNameProducer(objectManager));
   }

   @Provides
   @Singleton
   @DeleteObjectName
   public CachingProducer<String> provideDeleteObjectName(final ObjectManager objectManager)
   {
      return new CachingProducer<String>(new DeleteObjectNameProducer(objectManager));
   }

   @Provides
   @Singleton
   public List<ObjectNameConsumer> provideObjectNameConsumers(
         final ObjectManager objectManager,
         final EventBus eventBus)
   {
      final List<Integer> sc = HttpUtil.SUCCESS_STATUS_CODES;
      final List<ObjectNameConsumer> consumers = new ArrayList<ObjectNameConsumer>();
      consumers.add(new WriteObjectNameConsumer(objectManager, sc));
      consumers.add(new ReadObjectNameConsumer(objectManager, sc));

      for (final ObjectNameConsumer consumer : consumers)
      {
         eventBus.register(consumer);
      }
      return consumers;
   }
}
