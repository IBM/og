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
// Date: Feb 12, 2014
// ---------------------

package com.cleversafe.oom.soh.operation;

import java.net.URL;
import java.nio.ByteBuffer;

import com.cleversafe.oom.operation.HTTPMethod;
import com.cleversafe.oom.operation.HTTPOperation;
import com.cleversafe.oom.operation.OperationType;

public class SOHPutObjectOperation extends HTTPOperation
{
   private final ByteBuffer oid;

   public SOHPutObjectOperation(
         final OperationType operationType,
         final URL url,
         final HTTPMethod method)
   {
      super(operationType, url, method);
      this.oid = ByteBuffer.allocate(18);
   }

   @Override
   public void onReceivedContent(final ByteBuffer bytes)
   {
      super.onReceivedContent(bytes);
      if (this.oid.hasRemaining())
      {
         final int numBytes = Math.min(this.oid.remaining(), bytes.remaining());
         bytes.limit(bytes.position() + numBytes);
         this.oid.put(bytes);

         if (!this.oid.hasRemaining())
            getObjectName().setName(this.oid.array());
      }
   }
}
