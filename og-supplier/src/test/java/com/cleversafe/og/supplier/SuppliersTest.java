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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.Test;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class SuppliersTest
{
   @Test(expected = NullPointerException.class)
   public void nullOf()
   {
      Suppliers.of(null);
   }

   @Test
   public void of()
   {
      final Supplier<Integer> s = Suppliers.of(1);
      for (int i = 0; i < 10; i++)
      {
         assertThat(s.get(), is(1));
      }
   }

   @Test(expected = NullPointerException.class)
   public void nullCycle()
   {
      Suppliers.cycle(null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void emptyCycle()
   {
      Suppliers.cycle(ImmutableList.of());
   }

   @Test(expected = NullPointerException.class)
   public void nullCycleElement()
   {
      final List<Integer> list = Lists.newArrayList();
      list.add(null);
      Suppliers.cycle(list);
   }

   @Test
   public void cycleOneElement()
   {
      final Supplier<Integer> p = Suppliers.cycle(ImmutableList.of(1));
      for (int i = 0; i < 10; i++)
      {
         assertThat(p.get(), is(1));
      }
   }

   @Test
   public void cycleMultipleElements()
   {
      final Supplier<Integer> p = Suppliers.cycle(ImmutableList.of(1, 2, 3));
      for (int i = 0; i < 10; i++)
      {
         assertThat(p.get(), is(1));
         assertThat(p.get(), is(2));
         assertThat(p.get(), is(3));
      }
   }

   @Test
   public void cycleModification()
   {
      final List<Integer> list = Lists.newArrayList();
      list.add(1);
      list.add(2);
      final Supplier<Integer> p = Suppliers.cycle(list);
      list.add(3);
      for (int i = 0; i < 10; i++)
      {
         assertThat(p.get(), is(1));
         assertThat(p.get(), is(2));
      }
   }
}
