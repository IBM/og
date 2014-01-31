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
// Date: Jan 20, 2014
// ---------------------

package com.cleversafe.oom.operation;

import java.nio.ByteBuffer;

import com.cleversafe.oom.object.ObjectName;
import com.cleversafe.oom.operation.entity.Entity;

public interface Operation
{
   /**
    * Transitions the <code>OperationState</code> of this operation from <code>NEW</code> to
    * <code>ACTIVE</code>.
    * 
    * @return the timestamp at the time this method was called, in nanoseconds
    * @throws IllegalStateException
    *            if operation state is not <code>NEW</code>
    */
   long beginOperation();

   /**
    * Sets ttfb. This method is optional and does not need to be called for operations that do not
    * support a notion of ttfb.
    * 
    * @param ttfb
    *           time to first byte duration, in nanoseconds
    * @throws IllegalArgumentException
    *            if ttfb is negative
    * @throws IllegalStateException
    *            if operation state is not <code>ACTIVE</code>
    * @throws IllegalStateException
    *            if this method has already been called
    */
   void ttfb(final long ttfb);

   /**
    * Transitions the <code>OperationState</code> of this operation from <code>ACTIVE</code> to
    * <code>COMPLETED</code>.
    * 
    * @return the timestamp at the time this method was called, in nanoseconds
    * @throws IllegalStateException
    *            if operation state is not <code>ACTIVE</code>
    */
   long completeOperation();

   /**
    * Transitions the <code>OperationState</code> of this operation from <code>ACTIVE</code> to
    * <code>FAILED</code>.
    * 
    * @return the timestamp at the time this method was called, in nanoseconds
    * @throws IllegalStateException
    *            if operation state is not <code>ACTIVE</code>
    */
   long failOperation();

   /**
    * Transitions the <code>OperationState</code> of this operation from <code>ACTIVE</code> to
    * <code>ABORTED</code>.
    * 
    * @return the timestamp at the time this method was called, in nanoseconds
    * @throws IllegalStateException
    *            if operation state is not <code>ACTIVE</code>
    */
   long abortOperation();

   /**
    * Gets the operation type of this operation
    * 
    * @return the type of this operation
    */
   OperationType getOperationType();

   /**
    * Gets the operation state of this operation
    * 
    * @return the state of this operation
    */
   OperationState getOperationState();

   /**
    * Gets the object name for this operation
    * 
    * @return the name of this operation, or null if no object name exists for this operation
    */
   ObjectName getObjectName();

   /**
    * Sets the object name for this operation
    * 
    * @param objectName
    *           the object name to use for this operation, or null if no object name is required
    */
   void setObjectName(ObjectName objectName);

   /**
    * Gets the time spetn on this operation, if finished.
    * 
    * @return the time spent on this operation, in nanoseconds, or -1 if this operation is active
    */
   long getDuration();

   /**
    * Gets the ttfb for this operation, if set.
    * 
    * @return the ttfb for this operation, in nanoseconds, or -1 if ttfb was not set
    */
   long getTTFB();

   /**
    * Gets the amount of data processed for this operation, in bytes. <code>0</code> is returned for
    * the case where <code>bytes</code> was called with <code>0</code>, and also if
    * <code>bytes</code> was never called.
    * 
    * @return the amount of data processed for this operation, in bytes.
    */
   long getBytes();

   /**
    * Gets the entity to use as source data for this operation.
    * 
    * @return the entity for use as source data for this operation, or null if not set
    */
   public Entity getRequestEntity();

   /**
    * Sets the entity for use as source data for this operation.
    * 
    * @param entity
    *           the source entity for this operation
    */
   public void setRequestEntity(final Entity entity);

   /**
    * Processes response content as it is ready. This method is optional and does not need to be
    * called for operations that do not support a notion of response content. This method may be
    * called multiple times by the caller.
    * 
    * @param bytes
    *           bytes processed for this operation
    * @throws NullPointerException
    *            if bytes is null
    * @throws IllegalStateException
    *            if operation state is not <code>ACTIVE</code>
    */
   void onReceivedContent(final ByteBuffer bytes);
}
