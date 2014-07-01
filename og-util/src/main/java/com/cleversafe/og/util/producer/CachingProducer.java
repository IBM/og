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
// Date: Jul 1, 2014
// ---------------------

package com.cleversafe.og.util.producer;

import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CachingProducer<T> implements Producer<T>
{
   private static final Logger _logger = LoggerFactory.getLogger(CachingProducer.class);
   private final Producer<T> producer;
   private T cachedValue;

   public CachingProducer(final Producer<T> producer)
   {
      this.producer = checkNotNull(producer);
   }

   @Override
   public T produce()
   {
      this.cachedValue = this.producer.produce();
      return this.cachedValue;
   }

   public T getCachedValue()
   {
      return this.cachedValue;
   }
}
