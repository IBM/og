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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.cleversafe.og.cli.json.StoppingConditionsConfig;
import com.cleversafe.og.client.Client;
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
import com.cleversafe.og.util.Operation;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class OGModule extends AbstractModule
{

   private final Map<Long, Request> pendingRequests;

   public OGModule()
   {
      // have to create this here so that LoadTest can access a modifiable
      // version of the map, all others get a read-only instance
      this.pendingRequests = new ConcurrentHashMap<Long, Request>();
   }

   @Override
   protected void configure()
   {}

   @Provides
   @Singleton
   LoadTest provideLoadTest(
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
            new LoadTest(operationManager, client, scheduler, stats, conditions,
                  this.pendingRequests);

      // have to create this condition after LoadTest because it triggers LoadTest.stopTest()
      if (config.getRuntime() > 0)
         new RuntimeCondition(test, config.getRuntime(), config.getRuntimeUnit());

      return test;
   }

   @Provides
   @Singleton
   public Map<Long, Request> providePendingRequests()
   {
      return Collections.unmodifiableMap(this.pendingRequests);
   }

   @Provides
   @Singleton
   public Statistics provideStatistics()
   {
      return new Statistics();
   }
}
