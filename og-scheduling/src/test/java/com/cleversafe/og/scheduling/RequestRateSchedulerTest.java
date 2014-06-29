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
// Date: Feb 11, 2014
// ---------------------

package com.cleversafe.og.scheduling;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.scheduling.RequestRateScheduler;
import com.cleversafe.og.util.distribution.Distribution;

public class RequestRateSchedulerTest
{
   private Distribution mockDistribution;

   @Before
   public void setBefore()
   {
      this.mockDistribution = mock(Distribution.class);
      when(this.mockDistribution.nextSample()).thenReturn(1.0);
   }

   @Test(expected = NullPointerException.class)
   public void testNullDistribution()
   {
      new RequestRateScheduler(null, TimeUnit.MILLISECONDS);
   }

   @Test(expected = NullPointerException.class)
   public void testNullTimeUnit()
   {
      new RequestRateScheduler(this.mockDistribution, null);
   }

   @Test
   public void testRequestRateScheduler()
   {
      new RequestRateScheduler(this.mockDistribution, TimeUnit.MILLISECONDS);
   }
}
