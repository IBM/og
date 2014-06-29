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

import org.junit.Assert;
import org.junit.Test;

public class ConstantProducerTest
{
   @Test(expected = NullPointerException.class)
   public void testNull()
   {
      new ConstantProducer<Integer>(null);
   }

   @Test
   public void testConstantProducer()
   {
      final Producer<Integer> p = new ConstantProducer<Integer>(1);
      for (int i = 0; i < 10; i++)
      {
         Assert.assertEquals(Integer.valueOf(1), p.produce());
      }
   }
}
