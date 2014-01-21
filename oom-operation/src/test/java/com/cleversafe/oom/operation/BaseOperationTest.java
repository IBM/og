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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cleversafe.oom.object.LegacyObjectName;
import com.cleversafe.oom.statistic.Statistics;
import com.cleversafe.oom.statistic.StatisticsImpl;

public class BaseOperationTest
{
   private BaseOperation o;
   private Statistics stats;

   @Before
   public void setBefore()
   {
      this.stats = new StatisticsImpl(0, 5000);
      this.o = new BaseOperation(OperationType.WRITE, this.stats);
   }

   @Test(expected = NullPointerException.class)
   public void testNullOperationType()
   {
      new BaseOperation(null, this.stats);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testALLOperationType()
   {
      new BaseOperation(OperationType.ALL, this.stats);
   }

   @Test(expected = NullPointerException.class)
   public void testNullStats()
   {
      new BaseOperation(OperationType.WRITE, null);
   }

   @Test
   public void testBaseOperation()
   {
      final BaseOperation o = new BaseOperation(OperationType.WRITE, this.stats);
      Assert.assertEquals(OperationType.WRITE, o.getOperationType());
   }

   @Test(expected = IllegalStateException.class)
   public void testBeginOperationTwice()
   {
      this.o.beginOperation();
      this.o.beginOperation();
   }

   @Test
   public void testBeginOperation()
   {
      Assert.assertEquals(OperationState.NEW, this.o.getOperationState());
      final long beginTimestamp = this.o.beginOperation();
      Assert.assertEquals(OperationState.ACTIVE, this.o.getOperationState());
      Assert.assertTrue(beginTimestamp > 0);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeTTFB()
   {
      this.o.beginOperation();
      this.o.ttfb(-1000);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeTTFB2()
   {
      this.o.beginOperation();
      this.o.ttfb(-1);
   }

   @Test
   public void testZeroTTFB()
   {
      this.o.beginOperation();
      this.o.ttfb(0);
   }

   @Test
   public void testPositiveTTFB()
   {
      this.o.beginOperation();
      this.o.ttfb(1);
   }

   @Test
   public void testPositiveTTFB2()
   {
      this.o.beginOperation();
      this.o.ttfb(1000);
   }

   @Test(expected = IllegalStateException.class)
   public void testTTFBBeforeBeginOperation()
   {
      this.o.ttfb(1);
   }

   @Test(expected = IllegalStateException.class)
   public void testTTFBTwice()
   {
      this.o.beginOperation();
      this.o.ttfb(1);
      this.o.ttfb(1);
   }

   @Test(expected = IllegalStateException.class)
   public void testTTFBAfterCompleteOperation()
   {
      this.o.beginOperation();
      this.o.completeOperation();
      this.o.ttfb(1);
   }

   @Test(expected = IllegalStateException.class)
   public void testTTFBAfterFailOperation()
   {
      this.o.beginOperation();
      this.o.failOperation();
      this.o.ttfb(1);
   }

   @Test(expected = IllegalStateException.class)
   public void testTTFBAfterAbortOperation()
   {
      this.o.beginOperation();
      this.o.abortOperation();
      this.o.ttfb(1);
   }

   @Test
   public void testTTFB()
   {
      this.o.beginOperation();
      Assert.assertEquals(OperationState.ACTIVE, this.o.getOperationState());
      Assert.assertEquals(-1, this.o.getTTFB());
      this.o.ttfb(100);
      Assert.assertEquals(OperationState.ACTIVE, this.o.getOperationState());
      Assert.assertEquals(100, this.o.getTTFB());
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeBytes()
   {
      this.o.beginOperation();
      this.o.bytes(-1000);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeBytes2()
   {
      this.o.beginOperation();
      this.o.bytes(-1);
   }

   @Test
   public void testZeroBytes()
   {
      this.o.beginOperation();
      this.o.bytes(0);
   }

   @Test
   public void testPositiveBytes()
   {
      this.o.beginOperation();
      this.o.bytes(1);
   }

   @Test
   public void testPositiveBytes2()
   {
      this.o.beginOperation();
      this.o.bytes(1000);
   }

   @Test(expected = IllegalStateException.class)
   public void testBytesBeforeBeginOperation()
   {
      this.o.bytes(1);
   }

   @Test(expected = IllegalStateException.class)
   public void testBytesAfterCompleteOperation()
   {
      this.o.beginOperation();
      this.o.completeOperation();
      this.o.bytes(1);
   }

   @Test(expected = IllegalStateException.class)
   public void testBytesAfterFailOperation()
   {
      this.o.beginOperation();
      this.o.failOperation();
      this.o.bytes(1);
   }

   @Test(expected = IllegalStateException.class)
   public void testBytesAfterAbortOperation()
   {
      this.o.beginOperation();
      this.o.abortOperation();
      this.o.bytes(1);
   }

   @Test
   public void testBytes()
   {
      this.o.beginOperation();
      Assert.assertEquals(OperationState.ACTIVE, this.o.getOperationState());
      Assert.assertEquals(0, this.o.getBytes());
      this.o.bytes(0);
      Assert.assertEquals(OperationState.ACTIVE, this.o.getOperationState());
      Assert.assertEquals(0, this.o.getBytes());
      this.o.bytes(1024);
      Assert.assertEquals(OperationState.ACTIVE, this.o.getOperationState());
      Assert.assertEquals(1024, this.o.getBytes());
   }

   @Test(expected = IllegalStateException.class)
   public void testCompleteOperationBeforeBeginOperation()
   {
      this.o.completeOperation();
   }

   @Test(expected = IllegalStateException.class)
   public void testCompleteOperationTwice()
   {
      this.o.beginOperation();
      this.o.completeOperation();
      this.o.completeOperation();
   }

   @Test
   public void testCompleteOperation()
   {
      final long beginTimestamp = this.o.beginOperation();
      Assert.assertEquals(OperationState.ACTIVE, this.o.getOperationState());
      final long endTimestamp = this.o.completeOperation();
      Assert.assertEquals(OperationState.COMPLETED, this.o.getOperationState());
      Assert.assertEquals(endTimestamp - beginTimestamp, this.o.getDuration());
   }

   @Test(expected = IllegalStateException.class)
   public void testFailOperationBeforeBeginOperation()
   {
      this.o.failOperation();
   }

   @Test(expected = IllegalStateException.class)
   public void testFailOperationTwice()
   {
      this.o.beginOperation();
      this.o.failOperation();
      this.o.failOperation();
   }

   @Test
   public void testFailOperation()
   {
      final long beginTimestamp = this.o.beginOperation();
      Assert.assertEquals(OperationState.ACTIVE, this.o.getOperationState());
      final long endTimestamp = this.o.failOperation();
      Assert.assertEquals(OperationState.FAILED, this.o.getOperationState());
      Assert.assertEquals(endTimestamp - beginTimestamp, this.o.getDuration());
   }

   @Test(expected = IllegalStateException.class)
   public void testAbortOperationBeforeBeginOperation()
   {
      this.o.abortOperation();
   }

   @Test(expected = IllegalStateException.class)
   public void testAbortOperationTwice()
   {
      this.o.beginOperation();
      this.o.abortOperation();
      this.o.abortOperation();
   }

   @Test
   public void testAbortOperation()
   {
      final long beginTimestamp = this.o.beginOperation();
      Assert.assertEquals(OperationState.ACTIVE, this.o.getOperationState());
      final long endTimestamp = this.o.abortOperation();
      Assert.assertEquals(OperationState.ABORTED, this.o.getOperationState());
      Assert.assertEquals(endTimestamp - beginTimestamp, this.o.getDuration());
   }

   @Test
   public void testGetSetObjectName()
   {
      Assert.assertEquals(null, this.o.getObjectName());
      final LegacyObjectName objectName = LegacyObjectName.forBytes(new byte[18]);
      this.o.setObjectName(objectName);
      Assert.assertEquals(objectName, this.o.getObjectName());
   }
}
