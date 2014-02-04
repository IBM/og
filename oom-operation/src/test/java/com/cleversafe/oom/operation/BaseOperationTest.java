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
// Date: Jan 21, 2014
// ---------------------

package com.cleversafe.oom.operation;

import java.io.InputStream;
import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cleversafe.oom.object.ObjectName;
import com.cleversafe.oom.operation.entity.Entity;

public class BaseOperationTest
{
   private BaseOperation operation;

   @Before
   public void setBefore()
   {
      this.operation = new BaseOperation(OperationType.WRITE);
   }

   @Test(expected = NullPointerException.class)
   public void testNullOperationType()
   {
      new BaseOperation(null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testALLOperationType()
   {
      new BaseOperation(OperationType.ALL);
   }

   @Test
   public void testBaseOperation()
   {
      final BaseOperation o = new BaseOperation(OperationType.WRITE);
      Assert.assertEquals(OperationType.WRITE, o.getOperationType());
      Assert.assertEquals(OperationState.NEW, o.getOperationState());
   }

   @Test(expected = NullPointerException.class)
   public void testSetNullOperationType()
   {
      this.operation.setOperationType(null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testSetALLOperationType()
   {
      this.operation.setOperationType(OperationType.ALL);
   }

   @Test
   public void testSetOperationType()
   {
      this.operation.setOperationType(OperationType.READ);
      Assert.assertEquals(OperationType.READ, this.operation.getOperationType());
   }

   @Test(expected = NullPointerException.class)
   public void testSetNullOperationState()
   {
      this.operation.setOperationState(null);
   }

   @Test
   public void testSetOperationState()
   {
      this.operation.setOperationState(OperationState.COMPLETED);
      Assert.assertEquals(OperationState.COMPLETED, this.operation.getOperationState());
   }

   @Test
   public void testGetSetObjectName()
   {
      final ObjectName objectName = new ObjectName()
      {
         @Override
         public void setName(final byte[] objectName)
         {}

         @Override
         public byte[] toBytes()
         {
            return null;
         }
      };
      Assert.assertEquals(null, this.operation.getObjectName());
      this.operation.setObjectName(objectName);
      Assert.assertEquals(objectName, this.operation.getObjectName());
   }

   @Test(expected = NullPointerException.class)
   public void testSetNullRequestEntity()
   {
      this.operation.setRequestEntity(null);
   }

   @Test
   public void testSetRequestEntity()
   {
      final Entity e = new Entity()
      {
         @Override
         public InputStream getInputStream()
         {
            return null;
         }

         @Override
         public long getSize()
         {
            return 0;
         }
      };
      this.operation.setRequestEntity(e);
      Assert.assertEquals(e, this.operation.getRequestEntity());
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeTTFB()
   {
      this.operation.setTTFB(-1000);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeTTFB2()
   {
      this.operation.setTTFB(-1);
   }

   @Test
   public void testZeroTTFB()
   {
      this.operation.setTTFB(0);
   }

   @Test
   public void testPositiveTTFB()
   {
      this.operation.setTTFB(1);
   }

   @Test
   public void testPositiveTTFB2()
   {
      this.operation.setTTFB(1000);
   }

   @Test
   public void testTTFB()
   {
      Assert.assertEquals(0, this.operation.getTTFB());
      this.operation.setTTFB(100);
      Assert.assertEquals(100, this.operation.getTTFB());
   }

   @Test(expected = NullPointerException.class)
   public void testNullOnReceivedContent()
   {
      this.operation.onReceivedContent(null);
   }

   @Test
   public void testOnReceivedContent()
   {
      Assert.assertEquals(0, this.operation.getBytes());
      this.operation.onReceivedContent(ByteBuffer.allocate(0));
      Assert.assertEquals(0, this.operation.getBytes());
      final ByteBuffer buf = ByteBuffer.allocate(1024);
      buf.put(new byte[1024]);
      buf.flip();
      this.operation.onReceivedContent(buf);
      Assert.assertEquals(1024, this.operation.getBytes());
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeDuration()
   {
      this.operation.setDuration(-1000);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeDuration2()
   {
      this.operation.setDuration(-1);
   }

   @Test
   public void testZeroDuration()
   {
      this.operation.setDuration(0);
   }

   @Test
   public void testPositiveDuration()
   {
      this.operation.setDuration(1);
   }

   @Test
   public void testPositiveDuration2()
   {
      this.operation.setDuration(1000);
   }

   @Test
   public void testDuration()
   {
      Assert.assertEquals(0, this.operation.getDuration());
      this.operation.setDuration(100);
      Assert.assertEquals(100, this.operation.getDuration());
   }
}
