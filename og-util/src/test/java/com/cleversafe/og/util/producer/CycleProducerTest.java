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

package com.cleversafe.og.util.producer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class CycleProducerTest
{
   @Test(expected = NullPointerException.class)
   public void testNull()
   {
      new CycleProducer<Integer>(null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testEmptyList()
   {
      new CycleProducer<Integer>(Collections.<Integer> emptyList());
   }

   @Test(expected = NullPointerException.class)
   public void testNullElement()
   {
      final List<Integer> list = new ArrayList<Integer>();
      list.add(null);
      new CycleProducer<Integer>(list);
   }

   @Test
   public void testOneElement()
   {
      final List<Integer> list = new ArrayList<Integer>();
      list.add(1);
      final Producer<Integer> p = new CycleProducer<Integer>(list);
      for (int i = 0; i < 10; i++)
      {
         Assert.assertEquals(Integer.valueOf(1), p.produce());
      }
   }

   @Test
   public void testNElements()
   {
      final List<Integer> list = new ArrayList<Integer>();
      list.add(1);
      list.add(2);
      list.add(3);
      final Producer<Integer> p = new CycleProducer<Integer>(list);
      for (int i = 0; i < 10; i++)
      {
         Assert.assertEquals(Integer.valueOf(1), p.produce());
         Assert.assertEquals(Integer.valueOf(2), p.produce());
         Assert.assertEquals(Integer.valueOf(3), p.produce());
      }
   }
}
