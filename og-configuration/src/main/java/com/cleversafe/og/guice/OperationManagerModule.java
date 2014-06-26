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
// Date: Jun 25, 2014
// ---------------------

package com.cleversafe.og.guice;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.guice.annotation.Delete;
import com.cleversafe.og.guice.annotation.DeleteWeight;
import com.cleversafe.og.guice.annotation.Read;
import com.cleversafe.og.guice.annotation.ReadWeight;
import com.cleversafe.og.guice.annotation.Write;
import com.cleversafe.og.guice.annotation.WriteWeight;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.operation.Response;
import com.cleversafe.og.operation.manager.OperationManager;
import com.cleversafe.og.scheduling.Scheduler;
import com.cleversafe.og.test.operation.manager.SimpleOperationManager;
import com.cleversafe.og.util.consumer.Consumer;
import com.cleversafe.og.util.producer.Producer;
import com.cleversafe.og.util.producer.RandomChoiceProducer;
import com.google.common.math.DoubleMath;
import com.google.common.reflect.TypeToken;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class OperationManagerModule extends AbstractModule
{
   private static Logger _logger = LoggerFactory.getLogger(OperationManagerModule.class);
   private final static double err = Math.pow(0.1, 6);

   public OperationManagerModule()
   {}

   @Override
   protected void configure()
   {}

   @Provides
   @Singleton
   public OperationManager provideOperationManager(
         final Producer<Producer<Request>> producer,
         final Scheduler scheduler,
         final List<Consumer<Response>> consumers)
   {
      return new SimpleOperationManager(producer, consumers, scheduler);
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
      checkArgument(DoubleMath.fuzzyEquals(sum, 100.0, err),
            "Sum of percentages must be 100.0 [%s]", sum);

      // Have to capture generic type with a type token
      @SuppressWarnings("serial")
      final TypeToken<Producer<Request>> t = new TypeToken<Producer<Request>>()
      {};
      final RandomChoiceProducer.Builder<Producer<Request>> wrc =
            RandomChoiceProducer.custom(t.getRawType());
      if (writeWeight > 0.0)
         wrc.withChoice(write, writeWeight);
      if (readWeight > 0.0)
         wrc.withChoice(read, readWeight);
      if (deleteWeight > 0.0)
         wrc.withChoice(delete, deleteWeight);

      return wrc.build();
   }
}
