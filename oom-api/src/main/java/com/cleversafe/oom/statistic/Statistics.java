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
// Date: Jan 15, 2014
// ---------------------

package com.cleversafe.oom.statistic;

import com.cleversafe.oom.operation.OperationType;

/**
 * A statistics interface representing a collection of counters and statistics calculations.
 */
public interface Statistics
{
   /**
    * Sets counters for a new operation.
    * 
    * @param operationType
    *           the type of operation
    * @return the timestamp at the time this method was called, in nanoseconds
    * @throws NullPointerException
    *            if operationType is null
    */
   long beginOperation(final OperationType operationType);

   /**
    * Sets ttfb counters. This method is optional and does not need to be called for operations that
    * do not support a notion of ttfb.
    * 
    * @param operationType
    *           the type of operation
    * @param ttfb
    *           time to first byte duration, in nanoseconds
    * @throws NullPointerException
    *            if operationType is null
    * @throws IllegalArgumentException
    *            if ttfb is negative
    * @throws IllegalStateException
    *            if no operation of operationType is currently active
    */
   void ttfb(final OperationType operationType, final long ttfb);

   /**
    * Sets bytes counters. This method is optional and does not need to be called for operations
    * that do not support a notion of bytes. This method may be called multiple times by the caller.
    * 
    * @param operationType
    *           the type of operation
    * @param bytes
    *           bytes processed by this operation
    * @throws NullPointerException
    *            if operationType is null
    * @throws IllegalArgumentException
    *            if bytes is less than one
    * @throws IllegalStateException
    *            if no operation of operationType is currently active
    */
   void bytes(final OperationType operationType, final long bytes);

   /**
    * Sets counters for a completed operation.
    * 
    * @param operationType
    *           the type of operation
    * @param beginTimestamp
    *           the timestamp returned by <code>beginOperation</code
    * @return the timestamp at the time this method was called, in nanoseconds
    * @throws NullPointerException
    *            if operationType is null
    * @throws IllegalArgumentException
    *            if beginTimestamp is negative
    * @throws IllegalStateException
    *            if no operation of operationType is currently active
    */
   long completeOperation(final OperationType operationType, final long beginTimestamp);

   /**
    * Sets counters for a failed operation.
    * 
    * @param operationType
    *           the type of operation
    * @param beginTimestamp
    *           the timestamp returned by <code>beginOperation</code
    * @return the timestamp at the time this method was called, in nanoseconds
    * @throws NullPointerException
    *            if operationType is null
    * @throws IllegalArgumentException
    *            if beginTimestamp is negative
    * @throws IllegalStateException
    *            if no operation of operationType is currently active
    */
   long failOperation(final OperationType operationType, final long beginTimestamp);

   /**
    * Sets counters for an aborted operation.
    * 
    * @param operationType
    *           the type of operation
    * @param beginTimestamp
    *           the timestamp returned by <code>beginOperation</code
    * @return the timestamp at the time this method was called, in nanoseconds
    * @throws NullPointerException
    *            if operationType is null
    * @throws IllegalArgumentException
    *            if beginTimestamp is negative
    * @throws IllegalStateException
    *            if no operation of operationType is currently active
    */
   long abortOperation(final OperationType operationType, final long beginTimestamp);

   /**
    * Retrieves the value of a counter.
    * 
    * @param operationType
    *           the operation type of the counter
    * @param counter
    *           the counter type
    * @param interval
    *           if true, retrieve the counter value for the current interval, otherwise retrieve the
    *           overall counter value
    * @return the counter value
    * @throws NullPointerException
    *            if operationType is null
    * @throws NullPointerException
    *            if counter is null
    */
   long getCounter(OperationType operationType, Counter counter, boolean interval);

   /**
    * Calculates the value of a stat.
    * 
    * @param operationType
    *           the operation type of the stat
    * @param stat
    *           the stat type
    * @param interval
    *           if true, calculates the stat value for the current interval, otherwise calculates
    *           the overall stat value
    * @return the calculated value of the stat
    * @throws NullPointerException
    *            if operationType is null
    * @throws NullPointerException
    *            if stat is null
    */
   double getStat(OperationType operationType, Stat stat, boolean interval);

   /**
    * Calculates duration.
    * 
    * @param interval
    *           if true, calculates duration for the current interval, otherwise calculates the
    *           overall duration
    * @return duration, in nanoseconds
    */
   long getDuration(boolean interval);

   /**
    * Calculates a rough approximation of vault fill. Lack of knowledge of object size for deleted
    * objects as well as vault activity by other clients limits the accuracy of this approximation.
    * 
    * @return vault fill, in bytes
    */
   long getVaultFill();

   /**
    * Configures a <code>Stats</code> instance containing copies of all counters and timers stored
    * by this instance. All counters and timers of the returned instance are independent copies from
    * those found in this instance.
    * 
    * @return a <code>Stats</code> instance containing duplicated counters
    */
   Statistics snapshot();
}
