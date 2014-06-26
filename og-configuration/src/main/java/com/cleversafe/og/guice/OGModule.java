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
         final StoppingConditionsConfig config)
   {
      final LoadTest test = new LoadTest(operationManager, client);
      final List<TestCondition> conditions = new ArrayList<TestCondition>();

      if (config.getOperations() > 0)
         conditions.add(new CounterCondition(Operation.ALL, Counter.OPERATIONS,
               config.getOperations()));

      if (config.getAborts() > 0)
         conditions.add(new CounterCondition(Operation.ALL, Counter.ABORTS, config.getAborts()));

      if (config.getRuntime() > 0)
         new RuntimeCondition(Thread.currentThread(), test, config.getRuntime(),
               config.getRuntimeUnit());

      final Map<Integer, Integer> scMap = config.getStatusCodes();
      for (final Entry<Integer, Integer> sc : scMap.entrySet())
      {
         if (sc.getValue() > 0)
            conditions.add(new StatusCodeCondition(Operation.ALL, sc.getKey(), sc.getValue()));
      }

      return test;
   }

   @Provides
   @Singleton
   public Statistics provideStatistics()
   {
      return new Statistics();
   }
}
