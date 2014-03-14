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
// Date: Mar 12, 2014
// ---------------------

package com.cleversafe.oom.util;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.oom.api.ByteBufferConsumer;

public class ByteBufferConsumers
{
   private static Logger _logger = LoggerFactory.getLogger(ByteBufferConsumers.class);
   private static final ByteBufferConsumer NO_OP_CONSUMER = new ByteBufferConsumer()
   {
      private final Map<String, String> emptyMap = Collections.emptyMap();

      @Override
      public void consume(final ByteBuffer item)
      {
         // do nothing
      }

      @Override
      public Iterator<Entry<String, String>> metaData()
      {
         return this.emptyMap.entrySet().iterator();
      }
   };

   private ByteBufferConsumers()
   {}

   public static ByteBufferConsumer noOp()
   {
      return ByteBufferConsumers.NO_OP_CONSUMER;
   }
}
