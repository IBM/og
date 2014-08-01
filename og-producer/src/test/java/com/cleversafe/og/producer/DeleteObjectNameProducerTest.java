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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.object.ObjectManager;
import com.cleversafe.og.object.ObjectManagerException;
import com.cleversafe.og.object.ObjectName;
import com.cleversafe.og.producer.DeleteObjectNameProducer;
import com.cleversafe.og.producer.Producer;
import com.cleversafe.og.producer.ProducerException;

public class DeleteObjectNameProducerTest
{
   private ObjectManager mockObjectManager;

   @Before
   public void before()
   {
      this.mockObjectManager = mock(ObjectManager.class);
   }

   @Test(expected = NullPointerException.class)
   public void testNullObjectManager()
   {
      new DeleteObjectNameProducer(null);
   }

   @Test
   public void testDeleteObjectNameProducer()
   {
      final String objectString = "objectName";
      final ObjectName mockObjectName = mock(ObjectName.class);
      when(mockObjectName.toString()).thenReturn(objectString);
      when(this.mockObjectManager.getNameForDelete()).thenReturn(mockObjectName);

      final Producer<String> p = new DeleteObjectNameProducer(this.mockObjectManager);
      Assert.assertEquals(objectString, p.produce());
   }

   @Test(expected = ProducerException.class)
   public void testProducerException()
   {
      when(this.mockObjectManager.getNameForDelete()).thenThrow(new ObjectManagerException());
      final Producer<String> p = new DeleteObjectNameProducer(this.mockObjectManager);
      p.produce();
   }
}
