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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.cleversafe.og.cli.json.StoppingConditionsConfig;
import com.cleversafe.og.client.Client;
import com.cleversafe.og.operation.manager.OperationManager;
import com.cleversafe.og.statistic.Counter;
import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.test.LoadTest;
import com.cleversafe.og.test.RuntimeListener;
import com.cleversafe.og.test.StatisticsListener;
import com.cleversafe.og.test.StatusCodeListener;
import com.cleversafe.og.util.Operation;
import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class OGModule extends AbstractModule
{

   public OGModule()
   {}

   @Override
   protected void configure()
   {}

   // TODO refactor stopping condition creation
   @Provides
   @Singleton
   LoadTest provideLoadTest(
         final OperationManager operationManager,
         final Client client,
         final StoppingConditionsConfig stoppingConditions,
         final EventBus eventBus)
   {
      final LoadTest test = new LoadTest(operationManager, client);
      final List<StatisticsListener> statsListeners = new ArrayList<StatisticsListener>();
      final List<StatusCodeListener> statusCodeListeners = new ArrayList<StatusCodeListener>();

      if (stoppingConditions.getOperations() > 0)
         statsListeners.add(new StatisticsListener(test, Operation.ALL, Counter.OPERATIONS,
               stoppingConditions.getOperations()));

      if (stoppingConditions.getAborts() > 0)
         statsListeners.add(new StatisticsListener(test, Operation.ALL, Counter.ABORTS,
               stoppingConditions.getAborts()));

      // RuntimeListener does not need to be registered with the event bus
      if (stoppingConditions.getRuntime() > 0)
         new RuntimeListener(Thread.currentThread(), test, stoppingConditions.getRuntime(),
               stoppingConditions.getRuntimeUnit());

      final Map<Integer, Integer> scMap = stoppingConditions.getStatusCodes();
      for (final Entry<Integer, Integer> sc : scMap.entrySet())
      {
         if (sc.getValue() > 0)
            statusCodeListeners.add(new StatusCodeListener(test, Operation.ALL, sc.getKey(),
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
