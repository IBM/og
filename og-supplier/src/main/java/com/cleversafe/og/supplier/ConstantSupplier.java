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

package com.cleversafe.og.supplier;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Supplier;

/**
 * A supplier which always supplies the same value
 * 
 * @param <T>
 *           the type of value to supply
 * @since 1.0
 */
public class ConstantSupplier<T> implements Supplier<T>
{
   private final T value;

   /**
    * Constructs a supplier using the provided value
    * 
    * @param value
    *           the value this supplier should always supply
    */
   public ConstantSupplier(final T value)
   {
      this.value = checkNotNull(value);
   }

   @Override
   public T get()
   {
      return this.value;
   }

   @Override
   public String toString()
   {
      return "ConstantSupplier [" + this.value + "]";
   }
}
