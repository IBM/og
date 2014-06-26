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

package com.cleversafe.og.test;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuntimeListener
{
   private static Logger _logger = LoggerFactory.getLogger(RuntimeListener.class);
   private final Thread mainThread;
   private final LoadTest test;
   private final double duration;
   private final TimeUnit unit;

   public RuntimeListener(
         final Thread mainThread,
         final LoadTest test,
         final double duration,
         final TimeUnit unit)
   {
      this.mainThread = checkNotNull(mainThread);
      this.test = checkNotNull(test);
      checkArgument(duration > 0.0, "duration must be > 0.0 [%s]", duration);
      this.duration = duration;
      this.unit = checkNotNull(unit);

      final Thread t = new Thread(new Runnable()
      {
         @Override
         public void run()
         {
            long timestampStart = System.nanoTime();
            long sleepRemaining = nextSleepDuration();

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
            RuntimeListener.this.test.stopTest();
            RuntimeListener.this.mainThread.interrupt();
         }

         private final long nextSleepDuration()
         {
            return (long) (RuntimeListener.this.duration * RuntimeListener.this.unit.toNanos(1));
         }

      });
      t.setName("runtimeListener");
      t.setDaemon(true);
      t.start();
   }
}
