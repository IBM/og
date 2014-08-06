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

import org.junit.Assert;
import org.junit.Test;

import com.cleversafe.og.supplier.ConstantSupplier;
import com.google.common.base.Supplier;

public class ConstantSupplierTest
{
   @Test(expected = NullPointerException.class)
   public void testNull()
   {
      new ConstantSupplier<Integer>(null);
   }

   @Test
   public void testConstantSupplier()
   {
      final Supplier<Integer> p = new ConstantSupplier<Integer>(1);
      for (int i = 0; i < 10; i++)
      {
         Assert.assertEquals(Integer.valueOf(1), p.get());
      }
   }
}
