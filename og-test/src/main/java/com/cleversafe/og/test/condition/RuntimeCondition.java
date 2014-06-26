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
// Date: Jun 22, 2014
// ---------------------

package com.cleversafe.og.test.condition;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.test.LoadTest;

public class RuntimeCondition implements TestCondition
{
   private static Logger _logger = LoggerFactory.getLogger(RuntimeCondition.class);
   private final Thread mainThread;
   private final LoadTest test;
   private final long runtime;
   private final long timestampStart;

   public RuntimeCondition(
         final Thread mainThread,
         final LoadTest test,
         final double runtime,
         final TimeUnit unit)
   {
      this.mainThread = checkNotNull(mainThread);
      this.test = checkNotNull(test);
      checkArgument(runtime > 0.0, "duration must be > 0.0 [%s]", runtime);
      this.runtime = (long) (runtime * unit.toNanos(1));
      this.timestampStart = System.nanoTime();

      final Thread t = new Thread(new Runnable()
      {
         @Override
         public void run()
         {
            long timestampStart = RuntimeCondition.this.timestampStart;
            long sleepRemaining = RuntimeCondition.this.runtime;

            while (sleepRemaining > 0)
            {
               try
               {
                  TimeUnit.NANOSECONDS.sleep(sleepRemaining);
               }
               catch (final InterruptedException e)
               {
                  // TODO meaningful message
               }
               finally
               {
                  final long timestampEnd = System.nanoTime();
                  final long sleptTime = timestampEnd - timestampStart;
                  timestampStart = timestampEnd;
                  sleepRemaining -= sleptTime;
               }
            }
            RuntimeCondition.this.test.stopTest();
            RuntimeCondition.this.mainThread.interrupt();
         }
      });
      t.setName("runtimeCondition");
      t.setDaemon(true);
      t.start();
   }

   @Override
   public boolean isTriggered(final Statistics stats)
   {
      // this method ignores the stats, it is concerned with runtime only
      final long currentRuntime = System.nanoTime() - this.timestampStart;
      if (currentRuntime >= this.runtime)
         return true;
      return false;
   }
}
