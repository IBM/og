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

public class ReadObjectNameProducerTest
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
      new ReadObjectNameProducer(null);
   }

   @Test
   public void testReadObjectNameProducer()
   {
      final String objectString = "objectName";
      final ObjectName mockObjectName = mock(ObjectName.class);
      when(mockObjectName.toString()).thenReturn(objectString);
      when(this.mockObjectManager.acquireNameForRead()).thenReturn(mockObjectName);

      final Producer<String> p = new ReadObjectNameProducer(this.mockObjectManager);
      Assert.assertEquals(objectString, p.produce());
   }

   @Test(expected = ObjectManagerException.class)
   public void testProducerException()
   {
      when(this.mockObjectManager.acquireNameForRead()).thenThrow(new ObjectManagerException());
      final Producer<String> p = new ReadObjectNameProducer(this.mockObjectManager);
      p.produce();
   }
}
