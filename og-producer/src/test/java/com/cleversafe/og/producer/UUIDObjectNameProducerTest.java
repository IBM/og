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
// Date: Jun 29, 2014
// ---------------------

package com.cleversafe.og.producer;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Supplier;

public class UUIDObjectNameProducerTest
{
   @Test
   public void testUUIDObjectNameProducer()
   {
      final Supplier<String> p = new UUIDObjectNameProducer();
      final String u1 = p.get();
      final String u2 = p.get();
      final String u3 = p.get();
      Assert.assertNotEquals(u1, u2);
      Assert.assertNotEquals(u1, u3);
      Assert.assertNotEquals(u2, u3);
   }
}
