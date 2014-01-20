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
// Date: Oct 28, 2013
// ---------------------

package com.cleversafe.oom.statistic;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cleversafe.oom.operation.OperationType;

public class StatisticsImplTest
{
   private Statistics s;
   private OperationType w;
   private OperationType r;
   private OperationType a;

   @Before
   public void setBefore()
   {
      this.s = new StatisticsImpl(0, 5000);
      this.w = OperationType.WRITE;
      this.r = OperationType.READ;
      this.a = OperationType.ALL;
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeInitialObjectCount()
   {
      new StatisticsImpl(-10000, 5000);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeInitialObjectCount2()
   {
      new StatisticsImpl(-1, 5000);
   }

   @Test
   public void testZeroInitialObjectCount()
   {
      final Statistics s = new StatisticsImpl(0, 5000);
      Assert.assertEquals(0, s.getVaultFill());
   }

   @Test
   public void testPositiveInitialObjectCount()
   {
      final Statistics s = new StatisticsImpl(100, 5000);
      Assert.assertEquals(500000, s.getVaultFill());
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeAverageObjectSize()
   {
      new StatisticsImpl(0, -10000);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeAverageObjectSize2()
   {
      new StatisticsImpl(0, -1);
   }

   @Test
   public void testZeroAverageObjectSize()
   {
      final Statistics s = new StatisticsImpl(0, 0);
      Assert.assertEquals(0, s.getVaultFill());
   }

   @Test
   public void testPositiveAverageObjectSize()
   {
      final Statistics s = new StatisticsImpl(1, 10);
      Assert.assertEquals(10, s.getVaultFill());
   }

   @Test
   public void testInitialStats()
   {
      for (final OperationType operationType : OperationType.values())
      {
         allCountersEqual(this.s, operationType, 0);
         allStatsEqual(this.s, operationType, 0.0);
      }
      Assert.assertEquals(0, this.s.getVaultFill());
   }

   @Test
   public void testSnapshotInitialStats()
   {
      final Statistics snap = this.s.snapshot();
      for (final OperationType o : OperationType.values())
      {
         allCountersEqual(this.s, o, 0);
         allCountersEqual(snap, o, 0);
         allStatsEqual(this.s, o, 0.0);
         allStatsEqual(snap, o, 0.0);
      }
      Assert.assertEquals(0, this.s.getVaultFill());
      Assert.assertEquals(0, snap.getVaultFill());
   }

   @Test(expected = NullPointerException.class)
   public void testBeginOperationNullType()
   {
      this.s.beginOperation(null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testBeginOperationAllType()
   {
      this.s.beginOperation(this.a);
   }

   @Test
   public void testBeginOperation()
   {
      this.s.beginOperation(this.w);
      allCounterEquals(this.s, this.w, Counter.COUNT, 1);
      allCounterEquals(this.s, this.w, Counter.ACTIVE_COUNT, 1);
      allCounterEquals(this.s, this.w, Counter.ACTIVE_COUNT_MIN, 0);
      allCounterEquals(this.s, this.w, Counter.ACTIVE_COUNT_MAX, 1);
   }

   @Test
   public void testBeginOperationMultiple()
   {
      this.s.beginOperation(this.w);
      this.s.beginOperation(this.w);
      this.s.beginOperation(this.w);
      allCounterEquals(this.s, this.w, Counter.COUNT, 3);
      allCounterEquals(this.s, this.w, Counter.ACTIVE_COUNT, 3);
      allCounterEquals(this.s, this.w, Counter.ACTIVE_COUNT_MIN, 0);
      allCounterEquals(this.s, this.w, Counter.ACTIVE_COUNT_MAX, 3);
   }

   @Test
   public void testBeginOperationMultipleSnapshot()
   {
      this.s.beginOperation(this.w);
      this.s.beginOperation(this.r);
      final Statistics snap = this.s.snapshot();
      this.s.beginOperation(this.w);

      bothCounterEquals(snap, this.w, Counter.COUNT, 1);
      bothCounterEquals(snap, this.r, Counter.COUNT, 1);
      bothCounterEquals(snap, this.a, Counter.COUNT, 2);
      counterEquals(this.s, this.w, Counter.COUNT, 2);
      counterEquals(this.s, this.r, Counter.COUNT, 1);
      counterEquals(this.s, this.a, Counter.COUNT, 3);
      iCounterEquals(this.s, this.w, Counter.COUNT, 1);
      iCounterEquals(this.s, this.r, Counter.COUNT, 0);
      iCounterEquals(this.s, this.a, Counter.COUNT, 1);

      bothCounterEquals(snap, this.w, Counter.ACTIVE_COUNT, 1);
      bothCounterEquals(snap, this.r, Counter.ACTIVE_COUNT, 1);
      bothCounterEquals(snap, this.a, Counter.ACTIVE_COUNT, 2);
      counterEquals(this.s, this.w, Counter.ACTIVE_COUNT, 2);
      counterEquals(this.s, this.r, Counter.ACTIVE_COUNT, 1);
      counterEquals(this.s, this.a, Counter.ACTIVE_COUNT, 3);
      iCounterEquals(this.s, this.w, Counter.ACTIVE_COUNT, 2);
      iCounterEquals(this.s, this.r, Counter.ACTIVE_COUNT, 1);
      iCounterEquals(this.s, this.a, Counter.ACTIVE_COUNT, 3);

      allCounterEquals(snap, this.w, Counter.ACTIVE_COUNT_MIN, 0);
      bothCounterEquals(snap, this.r, Counter.ACTIVE_COUNT_MIN, 0);
      counterEquals(this.s, this.w, Counter.ACTIVE_COUNT_MIN, 0);
      counterEquals(this.s, this.r, Counter.ACTIVE_COUNT_MIN, 0);
      counterEquals(this.s, this.a, Counter.ACTIVE_COUNT_MIN, 0);
      iCounterEquals(this.s, this.w, Counter.ACTIVE_COUNT_MIN, 1);
      iCounterEquals(this.s, this.r, Counter.ACTIVE_COUNT_MIN, 1);
      iCounterEquals(this.s, this.a, Counter.ACTIVE_COUNT_MIN, 2);

      bothCounterEquals(snap, this.w, Counter.ACTIVE_COUNT_MAX, 1);
      bothCounterEquals(snap, this.r, Counter.ACTIVE_COUNT_MAX, 1);
      bothCounterEquals(snap, this.a, Counter.ACTIVE_COUNT_MAX, 2);
      counterEquals(this.s, this.w, Counter.ACTIVE_COUNT_MAX, 2);
      counterEquals(this.s, this.r, Counter.ACTIVE_COUNT_MAX, 1);
      counterEquals(this.s, this.a, Counter.ACTIVE_COUNT_MAX, 3);
      iCounterEquals(this.s, this.w, Counter.ACTIVE_COUNT_MAX, 2);
      iCounterEquals(this.s, this.r, Counter.ACTIVE_COUNT_MAX, 1);
      iCounterEquals(this.s, this.a, Counter.ACTIVE_COUNT_MAX, 3);
   }

   @Test(expected = NullPointerException.class)
   public void testTTFBNullType()
   {
      this.s.ttfb(null, 1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testTTFBAllType()
   {
      this.s.ttfb(this.a, 1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testTTFBNegative()
   {
      // large negative
      this.s.ttfb(this.w, -100);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testTTFBNegative2()
   {
      // negative at boundary
      this.s.ttfb(this.w, -1);
   }

   @Test
   public void testTTFBZero()
   {
      // border
      this.s.ttfb(this.w, 0);
   }

   @Test
   public void testTTFBPositive()
   {
      // positive at border
      this.s.ttfb(this.w, 1);
   }

   @Test
   public void testTTFBPositive2()
   {
      // large positive
      this.s.ttfb(this.w, 100);
   }

   @Test
   public void testTTFB()
   {
      this.s.beginOperation(this.w);
      this.s.ttfb(this.w, 100);
      allCounterEquals(this.s, this.w, Counter.TTFB, 100);
      allStatEquals(this.s, this.w, Stat.TTFB_AVG, 100.0);
   }

   @Test
   public void testTTFBMultiple()
   {
      this.s.beginOperation(this.w);
      this.s.beginOperation(this.w);
      this.s.beginOperation(this.w);
      this.s.ttfb(this.w, 100);
      this.s.ttfb(this.w, 225);
      this.s.ttfb(this.w, 350);
      allCounterEquals(this.s, this.w, Counter.TTFB, 675);
      allStatEquals(this.s, this.w, Stat.TTFB_AVG, (double) (675) / 3);
   }

   @Test(expected = NullPointerException.class)
   public void testBytesNullType()
   {
      this.s.bytes(null, 1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testBytesAllType()
   {
      this.s.bytes(this.a, 1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testBytesNegative()
   {
      // large negative
      this.s.bytes(this.w, -100);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testBytesNegative2()
   {
      // negative at boundary
      this.s.bytes(this.w, -1);
   }

   @Test
   public void testBytesZero()
   {
      // border
      this.s.bytes(this.w, 0);
   }

   @Test
   public void testBytesPositive()
   {
      // positive at border
      this.s.bytes(this.w, 1);
   }

   @Test
   public void testBytesPositive2()
   {
      // large positive
      this.s.bytes(this.w, 100);
   }

   @Test
   public void testBytes()
   {
      this.s.beginOperation(this.w);
      this.s.bytes(this.w, 1024);
      allCounterEquals(this.s, this.w, Counter.BYTES, 1024);
      allStatEquals(this.s, this.w, Stat.BYTES_AVG, 1024.0);

   }

   @Test
   public void testBytesMultiple()
   {
      this.s.beginOperation(this.w);
      this.s.beginOperation(this.w);
      this.s.beginOperation(this.w);
      this.s.bytes(this.w, 128);
      this.s.bytes(this.w, 256);
      this.s.bytes(this.w, 512);
      allCounterEquals(this.s, this.w, Counter.BYTES, 896);
      allStatEquals(this.s, this.w, Stat.BYTES_AVG, (double) (896) / 3);
      Assert.assertEquals(896, this.s.getVaultFill());
   }

   @Test(expected = NullPointerException.class)
   public void testCompleteOperationNullType()
   {
      this.s.completeOperation(null, System.nanoTime());
   }

   @Test(expected = IllegalArgumentException.class)
   public void testCompleteOperationAllType()
   {
      this.s.completeOperation(this.a, System.nanoTime());
   }

   @Test(expected = IllegalArgumentException.class)
   public void testCompleteOperationNegative()
   {
      // large negative
      this.s.completeOperation(this.w, -100);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testCompleteOperationNegative2()
   {
      // negative at boundary
      this.s.completeOperation(this.w, -1);
   }

   @Test
   public void testCompleteOperationZero()
   {
      // must call beginOperation here or Counter class
      // will complain about negative Counter.COUNT value
      this.s.beginOperation(this.w);
      // border
      this.s.completeOperation(this.w, 0);
   }

   @Test
   public void testCompleteOperationPositive()
   {
      // must call beginOperation here or Counter class
      // will complain about negative Counter.COUNT value
      this.s.beginOperation(this.w);
      // positive at border
      this.s.completeOperation(this.w, 1);
   }

   @Test
   public void testCompleteOperationPositive2()
   {
      // large positive
      this.s.bytes(this.w, 100);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testCompleteOperationLargeBeginTimestamp()
   {
      this.s.completeOperation(this.w, System.nanoTime() * 2);
   }

   @Test
   public void testCompleteOperation()
   {
      final long begin = this.s.beginOperation(this.w);
      final long end = this.s.completeOperation(this.w, begin);

      allCounterEquals(this.s, this.w, Counter.COUNT, 1);
      allCounterEquals(this.s, this.w, Counter.ACTIVE_COUNT, 0);
      allCounterEquals(this.s, this.w, Counter.ACTIVE_COUNT_MIN, 0);
      allCounterEquals(this.s, this.w, Counter.ACTIVE_COUNT_MAX, 1);
      allCounterEquals(this.s, this.w, Counter.COMPLETE_COUNT, 1);
      allCounterEquals(this.s, this.w, Counter.FAILURE_COUNT, 0);
      allCounterEquals(this.s, this.w, Counter.ABORT_COUNT, 0);
      allCounterEquals(this.s, this.w, Counter.DURATION, end - begin);
      allCounterEquals(this.s, this.w, Counter.ACTIVE_DURATION, end - begin);
   }

   @Test
   public void testCompleteOperationMultiple()
   {
      final long begin1 = this.s.beginOperation(this.w);
      final long end1 = this.s.completeOperation(this.w, begin1);
      final long begin2 = this.s.beginOperation(this.w);
      final long end2 = this.s.completeOperation(this.w, begin2);
      final long duration = (end1 - begin1) + (end2 - begin2);

      allCounterEquals(this.s, this.w, Counter.COUNT, 2);
      allCounterEquals(this.s, this.w, Counter.ACTIVE_COUNT, 0);
      allCounterEquals(this.s, this.w, Counter.ACTIVE_COUNT_MIN, 0);
      allCounterEquals(this.s, this.w, Counter.ACTIVE_COUNT_MAX, 1);
      allCounterEquals(this.s, this.w, Counter.COMPLETE_COUNT, 2);
      allCounterEquals(this.s, this.w, Counter.FAILURE_COUNT, 0);
      allCounterEquals(this.s, this.w, Counter.ABORT_COUNT, 0);
      allCounterEquals(this.s, this.w, Counter.DURATION, duration);
      allCounterEquals(this.s, this.w, Counter.ACTIVE_DURATION, duration);
   }

   @Test
   public void testCompleteOperationMultipleOverlap()
   {
      final long begin1 = this.s.beginOperation(this.w);
      final long begin2 = this.s.beginOperation(this.w);
      final long end1 = this.s.completeOperation(this.w, begin1);
      final long end2 = this.s.completeOperation(this.w, begin2);
      final long duration = (end1 - begin1) + (end2 - begin2);

      allCounterEquals(this.s, this.w, Counter.COUNT, 2);
      allCounterEquals(this.s, this.w, Counter.ACTIVE_COUNT, 0);
      allCounterEquals(this.s, this.w, Counter.ACTIVE_COUNT_MIN, 0);
      allCounterEquals(this.s, this.w, Counter.ACTIVE_COUNT_MAX, 2);
      allCounterEquals(this.s, this.w, Counter.COMPLETE_COUNT, 2);
      allCounterEquals(this.s, this.w, Counter.FAILURE_COUNT, 0);
      allCounterEquals(this.s, this.w, Counter.ABORT_COUNT, 0);
      allCounterEquals(this.s, this.w, Counter.DURATION, duration);
      allCounterEquals(this.s, this.w, Counter.ACTIVE_DURATION, end2 - begin1);
   }

   // failOperation is the same as completeOperation save one counter
   @Test
   public void testFailOperation()
   {
      final long begin = this.s.beginOperation(this.w);
      final long end = this.s.failOperation(this.w, begin);

      allCounterEquals(this.s, this.w, Counter.COUNT, 1);
      allCounterEquals(this.s, this.w, Counter.ACTIVE_COUNT, 0);
      allCounterEquals(this.s, this.w, Counter.ACTIVE_COUNT_MIN, 0);
      allCounterEquals(this.s, this.w, Counter.ACTIVE_COUNT_MAX, 1);
      allCounterEquals(this.s, this.w, Counter.COMPLETE_COUNT, 0);
      allCounterEquals(this.s, this.w, Counter.FAILURE_COUNT, 1);
      allCounterEquals(this.s, this.w, Counter.ABORT_COUNT, 0);
      allCounterEquals(this.s, this.w, Counter.DURATION, end - begin);
      allCounterEquals(this.s, this.w, Counter.ACTIVE_DURATION, end - begin);
   }

   // abortOperation is the same as completeOperation save one counter
   @Test
   public void testAbortOperation()
   {
      final long begin = this.s.beginOperation(this.w);
      final long end = this.s.abortOperation(this.w, begin);

      allCounterEquals(this.s, this.w, Counter.COUNT, 1);
      allCounterEquals(this.s, this.w, Counter.ACTIVE_COUNT, 0);
      allCounterEquals(this.s, this.w, Counter.ACTIVE_COUNT_MIN, 0);
      allCounterEquals(this.s, this.w, Counter.ACTIVE_COUNT_MAX, 1);
      allCounterEquals(this.s, this.w, Counter.COMPLETE_COUNT, 0);
      allCounterEquals(this.s, this.w, Counter.FAILURE_COUNT, 0);
      allCounterEquals(this.s, this.w, Counter.ABORT_COUNT, 1);
      allCounterEquals(this.s, this.w, Counter.DURATION, end - begin);
      allCounterEquals(this.s, this.w, Counter.ACTIVE_DURATION, end - begin);
   }

   @Test
   public void testCompleteOperationMultipleSnapshot()
   {
      final long begin1 = this.s.beginOperation(this.w);
      final long begin2 = this.s.beginOperation(this.r);
      this.s.completeOperation(this.w, begin1);
      this.s.completeOperation(this.r, begin2);
      final Statistics snap = this.s.snapshot();
      final long begin3 = this.s.beginOperation(this.w);
      this.s.completeOperation(this.w, begin3);

      bothCounterEquals(snap, this.w, Counter.COMPLETE_COUNT, 1);
      bothCounterEquals(snap, this.r, Counter.COMPLETE_COUNT, 1);
      bothCounterEquals(snap, this.a, Counter.COMPLETE_COUNT, 2);
      counterEquals(this.s, this.w, Counter.COMPLETE_COUNT, 2);
      counterEquals(this.s, this.r, Counter.COMPLETE_COUNT, 1);
      counterEquals(this.s, this.a, Counter.COMPLETE_COUNT, 3);
      iCounterEquals(this.s, this.w, Counter.COMPLETE_COUNT, 1);
      iCounterEquals(this.s, this.r, Counter.COMPLETE_COUNT, 0);
      iCounterEquals(this.s, this.a, Counter.COMPLETE_COUNT, 1);
   }

   private void allCountersEqual(final Statistics s, final OperationType o, final long value)
   {
      for (final Counter c : Counter.values())
      {
         Assert.assertEquals(value, s.getCounter(o, c, false));
         Assert.assertEquals(value, s.getCounter(o, c, true));
      }
   }

   private void counterEquals(
         final Statistics s,
         final OperationType o,
         final Counter c,
         final long value)
   {
      Assert.assertEquals(value, s.getCounter(o, c, false));
   }

   private void iCounterEquals(
         final Statistics s,
         final OperationType o,
         final Counter c,
         final long value)
   {
      Assert.assertEquals(value, s.getCounter(o, c, true));
   }

   private void bothCounterEquals(
         final Statistics s,
         final OperationType o,
         final Counter c,
         final long value)
   {
      counterEquals(s, o, c, value);
      iCounterEquals(s, o, c, value);
   }

   private void allCounterEquals(
         final Statistics s,
         final OperationType o,
         final Counter c,
         final long value)
   {
      bothCounterEquals(s, o, c, value);
      bothCounterEquals(s, OperationType.ALL, c, value);
   }

   private void allStatsEqual(final Statistics s, final OperationType o, final double value)
   {
      for (final Stat stat : Stat.values())
      {
         Assert.assertEquals(value, s.getStat(o, stat, false), 0.001);
         Assert.assertEquals(value, s.getStat(o, stat, true), 0.001);
      }
   }

   private void statEquals(
         final Statistics s,
         final OperationType o,
         final Stat stat,
         final double value)
   {
      Assert.assertEquals(value, s.getStat(o, stat, false), 0.001);
   }

   private void iStatEquals(
         final Statistics s,
         final OperationType o,
         final Stat stat,
         final double value)
   {
      Assert.assertEquals(value, s.getStat(o, stat, true), 0.001);
   }

   private void bothStatEquals(
         final Statistics s,
         final OperationType o,
         final Stat stat,
         final double value)
   {
      statEquals(s, o, stat, value);
      iStatEquals(s, o, stat, value);
   }

   private void allStatEquals(
         final Statistics s,
         final OperationType o,
         final Stat stat,
         final double value)
   {
      bothStatEquals(s, o, stat, value);
      bothStatEquals(s, OperationType.ALL, stat, value);
   }
}
