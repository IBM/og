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

package com.cleversafe.og.supplier;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.cleversafe.og.supplier.CycleSupplier;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;

public class CycleSupplierTest
{
   @Test(expected = NullPointerException.class)
   public void testNull()
   {
      new CycleSupplier<Integer>(null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testEmptyList()
   {
      new CycleSupplier<Integer>(Collections.<Integer> emptyList());
   }

   @Test(expected = NullPointerException.class)
   public void testNullElement()
   {
      final List<Integer> list = Lists.newArrayList();
      list.add(null);
      new CycleSupplier<Integer>(list);
   }

   @Test
   public void testOneElement()
   {
      final List<Integer> list = Lists.newArrayList();
      list.add(1);
      final Supplier<Integer> p = new CycleSupplier<Integer>(list);
      for (int i = 0; i < 10; i++)
      {
         Assert.assertEquals(Integer.valueOf(1), p.get());
      }
   }

   @Test
   public void testNElements()
   {
      final List<Integer> list = Lists.newArrayList();
      list.add(1);
      list.add(2);
      list.add(3);
      final Supplier<Integer> p = new CycleSupplier<Integer>(list);
      for (int i = 0; i < 10; i++)
      {
         Assert.assertEquals(Integer.valueOf(1), p.get());
         Assert.assertEquals(Integer.valueOf(2), p.get());
         Assert.assertEquals(Integer.valueOf(3), p.get());
      }
   }

   @Test
   public void testListModification()
   {
      final List<Integer> list = Lists.newArrayList();
      list.add(1);
      list.add(2);
      final Supplier<Integer> p = new CycleSupplier<Integer>(list);
      list.add(3);
      for (int i = 0; i < 10; i++)
      {
         Assert.assertEquals(Integer.valueOf(1), p.get());
         Assert.assertEquals(Integer.valueOf(2), p.get());
      }
   }
}
