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
// Date: Jul 1, 2014
// ---------------------

package com.cleversafe.og.producer;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;

public class CachingProducerTest
{
   @Test(expected = NullPointerException.class)
   public void testNullSupplier()
   {
      new CachingProducer<String>((Supplier<String>) null);
   }

   @Test
   public void testCachingSupplier()
   {
      final List<Integer> ints = Lists.newArrayList();
      ints.add(1);
      ints.add(2);
      ints.add(3);
      final Supplier<Integer> cycle = Producers.cycle(ints);
      final CachingProducer<Integer> p = new CachingProducer<Integer>(cycle);
      for (int i = 0; i < 10; i++)
      {
         Assert.assertEquals(p.get(), p.getCachedValue());
      }
   }
}
