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

package com.cleversafe.og.util;

import org.junit.Assert;
import org.junit.Test;

public class PairTest
{
   @Test(expected = NullPointerException.class)
   public void testNullKey()
   {
      Pair.of(null, "value");
   }

   @Test(expected = NullPointerException.class)
   public void testNullValue()
   {
      Pair.of("key", null);
   }

   @Test
   public void testPair()
   {
      final Pair<String, String> p = Pair.of("key", "value");
      Assert.assertEquals("key", p.getKey());
      Assert.assertEquals("value", p.getValue());
   }
}
