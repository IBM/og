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
    * Gets the operation type of this operation
    * 
    * @return the type of this operation
    */
   OperationType getOperationType();

   /**
    * Sets the operation type of this operation
    * 
    * @param operationType
    *           the type of this operation
    * @throws NullPointerException
    *            if operationType is null
    */
   void setOperationType(OperationType operationType);

   /**
    * Gets the operation state of this operation
    * 
    * @return the state of this operation
    */
   OperationState getOperationState();

   /**
    * Sets the operation state of this operation
    * 
    * @param operationState
    *           the state of this operation
    * @throws NullPointerException
    *            if operationState is null
    */
   void setOperationState(OperationState operationState);

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
   public void setRequestEntity(Entity entity);

   /**
    * Gets the ttfb for this operation, if set.
    * 
    * @return the ttfb for this operation, in nanoseconds
    */
   long getTTFB();

   /**
    * Sets the ttfb for this operation. This method is optional and does not need to be called for
    * operations that do not support a notion of ttfb.
    * 
    * @param ttfb
    *           time to first byte duration, in nanoseconds
    * @throws IllegalArgumentException
    *            if ttfb is negative
    */
   void setTTFB(long ttfb);

   /**
    * Gets the amount of data processed for this operation, in bytes.
    * 
    * @return the amount of data processed for this operation, in bytes.
    */
   long getBytes();

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
   void onReceivedContent(ByteBuffer bytes);

   /**
    * Gets the time spent on this operation.
    * 
    * @return the time spent on this operation, in nanoseconds
    */
   long getDuration();

   /**
    * Sets the time spent on this operation
    * 
    * @param duration
    *           the duration of this operation, in nanoseconds
    * @throws IllegalArgumentException
    *            if duration is negative
    */
   void setDuration(long duration);
}
