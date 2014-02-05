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

import java.nio.ByteBuffer;

import com.cleversafe.oom.object.ObjectName;
import com.cleversafe.oom.operation.entity.Entity;

/**
 * A basic <code>Operation</code> implementation.
 */
public class BaseOperation implements Operation
{
   private OperationType operationType;
   private OperationState operationState;
   private ObjectName objectName;
   private Entity requestEntity;
   private long ttfb;
   private long bytesSent;
   private long bytesReceived;
   private long duration;

   /**
    * Constructs a <code>BaseOperation</code> instance.
    * 
    * @param operationType
    *           the type of operation
    * @throws NullPointerException
    *            if operationType is null
    * @throws IllegalArgumentException
    *            if operationType is ALL
    */
   public BaseOperation(final OperationType operationType)
   {
      this.operationType = checkNotNull(operationType, "operationType must not be null");
      checkArgument(operationType != OperationType.ALL, "operationType must not be ALL");
      this.operationState = OperationState.NEW;
   }

   @Override
   public OperationType getOperationType()
   {
      return this.operationType;
   }

   @Override
   public void setOperationType(final OperationType operationType)
   {
      this.operationType = checkNotNull(operationType, "operationType must not be null");
      checkArgument(operationType != OperationType.ALL, "operationType must not be ALL");
   }

   @Override
   public OperationState getOperationState()
   {
      return this.operationState;
   }

   @Override
   public void setOperationState(final OperationState operationState)
   {
      this.operationState = checkNotNull(operationState, "operationState must not be null");
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
   public Entity getRequestEntity()
   {
      return this.requestEntity;
   }

   @Override
   public void setRequestEntity(final Entity entity)
   {
      this.requestEntity = checkNotNull(entity, "entity must not be null");
   }

   @Override
   public long getTTFB()
   {
      return this.ttfb;
   }

   @Override
   public void setTTFB(final long ttfb)
   {
      checkArgument(ttfb >= 0, "ttfb must be >= 0 [%s]", ttfb);
      this.ttfb = ttfb;
   }

   @Override
   public long getBytesSent()
   {
      return this.bytesSent;
   }

   @Override
   public void setBytesSent(final long bytes)
   {
      checkArgument(bytes >= 0, "bytes must be >= 0 [%s]", bytes);
      this.bytesSent = bytes;
   }

   @Override
   public long getBytesReceived()
   {
      return this.bytesReceived;
   }

   @Override
   public void onReceivedContent(final ByteBuffer bytes)
   {
      checkNotNull(bytes, "bytes must not be null");
      this.bytesReceived += bytes.remaining();
   }

   @Override
   public long getDuration()
   {
      return this.duration;
   }

   @Override
   public void setDuration(final long duration)
   {
      checkArgument(duration >= 0, "duration must be >= 0 [%s]", duration);
      this.duration = duration;
   }
}
