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

package com.cleversafe.og.supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.object.ObjectManager;
import com.cleversafe.og.object.ObjectManagerException;
import com.cleversafe.og.object.ObjectName;

public class ReadObjectNameSupplierTest
{
   private ObjectManager objectManager;

   @Before
   public void before()
   {
      this.objectManager = mock(ObjectManager.class);
   }

   @Test(expected = NullPointerException.class)
   public void nullObjectManager()
   {
      new ReadObjectNameSupplier(null);
   }

   @Test
   public void readObjectNameSupplier()
   {
      final String object = "objectName";
      final ObjectName objectName = mock(ObjectName.class);
      when(objectName.toString()).thenReturn(object);
      when(this.objectManager.acquireNameForRead()).thenReturn(objectName);

      assertThat(new ReadObjectNameSupplier(this.objectManager).get(), is(object));
   }

   @Test(expected = ObjectManagerException.class)
   public void supplierException()
   {
      when(this.objectManager.acquireNameForRead()).thenThrow(new ObjectManagerException());
      new ReadObjectNameSupplier(this.objectManager).get();
   }
}
