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
// Date: Jun 28, 2014
// ---------------------

package com.cleversafe.og.producer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.cleversafe.og.producer.Producers;

public class ProducersTest
{
   @Test(expected = NullPointerException.class)
   public void testNullOf()
   {
      Producers.of(null);
   }

   @Test
   public void testOf()
   {
      Producers.of(1);
   }

   @Test(expected = NullPointerException.class)
   public void testNullCycle()
   {
      Producers.cycle(null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testZeroCycle()
   {
      Producers.cycle(Collections.emptyList());
   }

   @Test(expected = NullPointerException.class)
   public void testNullCycleElement()
   {
      final List<Integer> list = new ArrayList<Integer>();
      list.add(null);
      Producers.cycle(list);
   }

   @Test
   public void testCycle()
   {
      final List<Integer> list = new ArrayList<Integer>();
      list.add(1);
      Producers.cycle(list);
   }
}
