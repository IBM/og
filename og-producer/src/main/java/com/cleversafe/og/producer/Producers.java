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
// Date: Mar 21, 2014
// ---------------------

package com.cleversafe.og.producer;

import java.util.List;

import com.google.common.base.Supplier;

/**
 * A utility class for creating supplier instances
 * 
 * @since 1.0
 */
public class Producers
{
   private Producers()
   {}

   /**
    * Creates a supplier that always returns the same value
    * 
    * @param value
    *           the value to supply
    * @return a supply which always returns the same value
    */
   public static <T> Supplier<T> of(final T value)
   {
      return new ConstantProducer<T>(value);
   }

   /**
    * Creates a supplier that returns values in a cycle
    * 
    * @param values
    *           the values to supply
    * @return a supplier which supplies values in a cycle
    */
   public static <T> Supplier<T> cycle(final List<T> values)
   {
      return new CycleProducer<T>(values);
   }
}
