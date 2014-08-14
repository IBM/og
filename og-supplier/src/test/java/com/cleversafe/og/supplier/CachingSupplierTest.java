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

package com.cleversafe.og.supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;

public class CachingSupplierTest
{
   @Test(expected = NullPointerException.class)
   public void nullSupplier()
   {
      new CachingSupplier<String>((Supplier<String>) null);
   }

   @Test
   public void cachingSupplier()
   {
      final Supplier<Integer> cycle = Suppliers.cycle(ImmutableList.of(1, 2, 3));
      final CachingSupplier<Integer> cache = new CachingSupplier<Integer>(cycle);
      for (int i = 0; i < 10; i++)
      {
         assertThat(cache.get(), is(cache.getCachedValue()));
      }
   }
}
