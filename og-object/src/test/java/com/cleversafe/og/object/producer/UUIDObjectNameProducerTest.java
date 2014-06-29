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

package com.cleversafe.og.object.producer;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.util.producer.Producer;

public class UUIDObjectNameProducerTest
{
   private static Logger _logger = LoggerFactory.getLogger(UUIDObjectNameProducerTest.class);

   @Test
   public void testUUIDObjectNameProducer()
   {
      final Producer<String> p = new UUIDObjectNameProducer();
      final String u1 = p.produce();
      final String u2 = p.produce();
      final String u3 = p.produce();
      Assert.assertNotEquals(u1, u2);
      Assert.assertNotEquals(u1, u3);
      Assert.assertNotEquals(u2, u3);
   }

}
