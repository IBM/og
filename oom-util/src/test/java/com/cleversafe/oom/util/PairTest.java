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
// Date: Apr 2, 2014
// ---------------------

package com.cleversafe.oom.util;

import org.junit.Assert;
import org.junit.Test;

public class PairTest
{
   @Test(expected = NullPointerException.class)
   public void testNullKey()
   {
      new Pair<String, String>(null, "value");
   }

   @Test(expected = NullPointerException.class)
   public void testNullValue()
   {
      new Pair<String, String>("key", null);
   }

   @Test
   public void testPair()
   {
      final Pair<String, String> p = new Pair<String, String>("key", "value");
      Assert.assertEquals("key", p.getKey());
      Assert.assertEquals("value", p.getValue());
   }
}
