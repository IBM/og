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

package com.cleversafe.og.util.producer;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class CachingProducerTest
{
   @Test(expected = NullPointerException.class)
   public void testNullProducer()
   {
      new CachingProducer<String>((Producer<String>) null);
   }

   @Test
   public void testCachingProducer()
   {
      final List<Integer> ints = new ArrayList<Integer>();
      ints.add(1);
      ints.add(2);
      ints.add(3);
      final Producer<Integer> cycle = Producers.cycle(ints);
      final CachingProducer<Integer> p = new CachingProducer<Integer>(cycle);
      for (int i = 0; i < 10; i++)
      {
         Assert.assertEquals(p.produce(), p.getCachedValue());
      }
   }
}
