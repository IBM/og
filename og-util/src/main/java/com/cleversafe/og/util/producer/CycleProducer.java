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
// Date: Jun 23, 2014
// ---------------------

package com.cleversafe.og.util.producer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.api.Producer;

public class CycleProducer<T> implements Producer<T>
{
   private static Logger _logger = LoggerFactory.getLogger(CycleProducer.class);
   private final List<T> items;
   private final AtomicLong counter;

   public CycleProducer(final List<T> items)
   {
      this.items = checkNotNull(items);
      checkArgument(items.size() > 0, "items size must be > 0");
      this.counter = new AtomicLong(0);
   }

   @Override
   public T produce()
   {
      final int idx = (int) (this.counter.getAndIncrement() % this.items.size());
      return this.items.get(idx);
   }

}
