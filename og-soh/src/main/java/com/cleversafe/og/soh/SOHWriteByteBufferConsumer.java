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
// Date: Mar 29, 2014
// ---------------------

package com.cleversafe.og.soh;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.cleversafe.og.util.consumer.ByteBufferConsumer;

public class SOHWriteByteBufferConsumer implements ByteBufferConsumer
{
   private final byte[] objectNameBytes;
   private int idx;
   private final Map<String, String> metadata;

   public SOHWriteByteBufferConsumer()
   {
      // TODO byte[] reuse? So we don't have to allocate a new byte[] for each request
      this.objectNameBytes = new byte[36];
      this.idx = 0;
      this.metadata = new HashMap<String, String>();
   }

   @Override
   public void consume(final ByteBuffer src)
   {
      final int srcRemaining = src.remaining();
      final int dstRemaining = remaining();
      final int transferSize = Math.min(srcRemaining, dstRemaining);
      if (transferSize > 0)
      {
         src.get(this.objectNameBytes, this.idx, transferSize);
         this.idx += transferSize;
         if (remaining() == 0)
         {
            this.metadata.put("object_name", new String(this.objectNameBytes));
         }
      }
   }

   private int remaining()
   {
      return this.objectNameBytes.length - this.idx;
   }

   @Override
   public Iterator<Entry<String, String>> metaData()
   {
      // TODO map is mutable so iterator can modify, is this a problem?
      return this.metadata.entrySet().iterator();
   }

}
