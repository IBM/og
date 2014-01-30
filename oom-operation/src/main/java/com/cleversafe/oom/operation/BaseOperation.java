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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.cleversafe.oom.object.ObjectName;
import com.cleversafe.oom.operation.entity.Entity;
import com.cleversafe.oom.statistic.Statistics;

/**
 * A basic <code>Operation</code> implementation.
 */
public class BaseOperation implements Operation
{
   private final Statistics stats;
   private final OperationType operationType;
   private OperationState operationState;
   private ObjectName objectName;
   private Entity requestEntity;
   private Entity responseEntity;
   private long beginTimestamp;
   private long ttfb;
   private long bytes;
   private long endTimestamp;

   /**
    * Constructs a <code>BaseOperation</code> instance. operation calls will delegate statistics
    * updates to the specified statistics instance.
    * 
    * @param operationType
    *           the type of operation
    * @param statistics
    *           the statistics instance to delegate statistics updates to
    * @throws NullPointerException
    *            if operationType is null
    * @throws IllegalArgumentException
    *            if operationType is ALL
    * @throws NullPointerException
    *            if statistics is null
    */
   public BaseOperation(final OperationType operationType, final Statistics statistics)
   {
      this.operationType = checkNotNull(operationType, "operationType must not be null");
      checkArgument(operationType != OperationType.ALL, "operationType must not be ALL");
      this.stats = checkNotNull(statistics, "statistics must not be null");
      this.operationState = OperationState.NEW;
      this.ttfb = -1;
   }

   @Override
   public long beginOperation()
   {
      checkState(this.operationState == OperationState.NEW, "operationState must be NEW [%s]",
            this.operationState);
      this.operationState = OperationState.ACTIVE;
      this.beginTimestamp = this.stats.beginOperation(this.operationType);
      return this.beginTimestamp;
   }

   @Override
   public void ttfb(final long ttfb)
   {
      checkState(this.operationState == OperationState.ACTIVE,
            "operationState must be ACTIVE [%s]", this.operationState);
      checkState(this.ttfb == -1, "ttfb already called for this operation");
      checkArgument(ttfb >= 0, "ttfb must be >= 0 [%s]", ttfb);
      this.ttfb = ttfb;
      this.stats.ttfb(this.operationType, ttfb);
   }

   @Override
   public void bytes(final long bytes)
   {
      checkState(this.operationState == OperationState.ACTIVE,
            "operationState must be ACTIVE [%s]", this.operationState);
      checkArgument(bytes >= 0, "bytes must be >= 0 [%s]", bytes);
      this.bytes += bytes;
      this.stats.bytes(this.operationType, bytes);
   }

   @Override
   public long completeOperation()
   {
      checkState(this.operationState == OperationState.ACTIVE,
            "operationState must be ACTIVE [%s]", this.operationState);
      this.operationState = OperationState.COMPLETED;
      this.endTimestamp = this.stats.completeOperation(this.operationType, this.beginTimestamp);
      return this.endTimestamp;
   }

   @Override
   public long failOperation()
   {
      checkState(this.operationState == OperationState.ACTIVE,
            "operationState must be ACTIVE [%s]", this.operationState);
      this.operationState = OperationState.FAILED;
      this.endTimestamp = this.stats.failOperation(this.operationType, this.beginTimestamp);
      return this.endTimestamp;
   }

   @Override
   public long abortOperation()
   {
      checkState(this.operationState == OperationState.ACTIVE,
            "operationState must be ACTIVE [%s]", this.operationState);
      this.operationState = OperationState.ABORTED;
      this.endTimestamp = this.stats.abortOperation(this.operationType, this.beginTimestamp);
      return this.endTimestamp;
   }

   @Override
   public OperationType getOperationType()
   {
      return this.operationType;
   }

   @Override
   public OperationState getOperationState()
   {
      return this.operationState;
   }

   @Override
   public ObjectName getObjectName()
   {
      return this.objectName;
   }

   @Override
   public void setObjectName(final ObjectName objectName)
   {
      this.objectName = objectName;
   }

   @Override
   public long getDuration()
   {
      if (this.operationState == OperationState.NEW || this.operationState == OperationState.ACTIVE)
         return -1;
      return this.endTimestamp - this.beginTimestamp;
   }

   @Override
   public long getTTFB()
   {
      return this.ttfb;
   }

   @Override
   public long getBytes()
   {
      return this.bytes;
   }

   @Override
   public Entity getRequestEntity()
   {
      return this.requestEntity;
   }

   @Override
   public void setRequestEntity(final Entity entity)
   {
      this.requestEntity = entity;
   }

   @Override
   public Entity getResponseEntity()
   {
      return this.responseEntity;
   }

   @Override
   public void setResponseEntity(final Entity entity)
   {
      this.responseEntity = entity;
   }
}
