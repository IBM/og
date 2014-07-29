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
// Date: Mar 19, 2014
// ---------------------

package com.cleversafe.og.util.producer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A producer which always produces the same value
 * 
 * @param <T>
 *           the type of value to produce
 * @since 1.0
 */
public class ConstantProducer<T> implements Producer<T>
{
   private final T value;

   /**
    * Constructs a producer using the provided value
    * 
    * @param value
    *           the value this producer should always produce
    */
   public ConstantProducer(final T value)
   {
      this.value = checkNotNull(value);
   }

   @Override
   public T produce()
   {
      return this.value;
   }
}
