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
// Date: Jul 11, 2014
// ---------------------

package com.cleversafe.og.scheduling;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.Mockito.mock;

import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.cleversafe.og.api.Request;
import com.cleversafe.og.api.Response;
import com.cleversafe.og.util.Pair;
import com.google.common.util.concurrent.Uninterruptibles;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class ConcurrentRequestSchedulerTest
{
   @Rule
   public ExpectedException thrown = ExpectedException.none();

   @DataProvider
   public static Object[][] provideInvalidConcurrentRequestScheduler()
   {
      final TimeUnit unit = TimeUnit.SECONDS;
      return new Object[][]{
            {-1, 0.0, unit, IllegalArgumentException.class},
            {0, 0.0, unit, IllegalArgumentException.class},
            {1, -1.0, unit, IllegalArgumentException.class},
            {1, 0.0, null, NullPointerException.class}
      };
   }

   @Test
   @UseDataProvider("provideInvalidConcurrentRequestScheduler")
   public void invalidConcurrentRequestScheduler(
         final int concurrentRequests,
         final double rampup,
         final TimeUnit rampupUnit,
         final Class<Exception> expectedException)
   {
      this.thrown.expect(expectedException);
      new ConcurrentRequestScheduler(concurrentRequests, rampup, rampupUnit);
   }

   @Test
   public void oneConcurrentRequest()
   {
      concurrentRequestScheduler(1);
   }

   @Test
   public void multipleConcurrentRequests()
   {
      concurrentRequestScheduler(10);
   }

   private void concurrentRequestScheduler(final int concurrentRequests)
   {
      final ConcurrentRequestScheduler scheduler =
            new ConcurrentRequestScheduler(concurrentRequests, 0.0, TimeUnit.SECONDS);
      int count = 0;
      final int threadWait = 50;
      new Thread(new Runnable()
      {
         @Override
         public void run()
         {
            Uninterruptibles.sleepUninterruptibly(threadWait, TimeUnit.MILLISECONDS);
            scheduler.complete(Pair.of(mock(Request.class), mock(Response.class)));
         }
      }).start();

      final long timestampStart = System.nanoTime();
      while (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timestampStart) < threadWait / 2)
      {
         count++;
         scheduler.waitForNext();
      }
      assertThat(count, is(concurrentRequests));
   }

   @Test
   public void interruptedSchedulerThread()
   {
      final ConcurrentRequestScheduler s = new ConcurrentRequestScheduler(1, 0.0, TimeUnit.SECONDS);
      final Thread t = new Thread(new Runnable()
      {
         @Override
         public void run()
         {
            s.waitForNext();
         }
      });
      t.start();
      t.interrupt();
      final long timestampStart = System.nanoTime();
      Uninterruptibles.joinUninterruptibly(t);
      final long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timestampStart);
      assertThat(duration, lessThan(1000L));
   }
}
