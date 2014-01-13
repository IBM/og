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
// Date: Nov 5, 2013
// ---------------------

package com.cleversafe.oom.statistic;

import org.apache.commons.lang3.Validate;

/**
 * A counters implementation for storing simple tool measurements.
 */
public class Counters
{
   private final long[] ctrs;

   /**
    * Constructs a new <code>Counters</code> instance with the same counter values as the specified
    * <code>Counters</code> instance.
    * 
    * @param counters
    *           the counters instance whose counters are to be copied to this counters instance
    * @throws NullPointerException
    *            if counters is null
    */
   public Counters(final Counters counters)
   {
      this();
      Validate.notNull(counters, "counters must not be null");
      System.arraycopy(counters.ctrs, 0, this.ctrs, 0, counters.ctrs.length);
   }

   /**
    * Constructs a <code>Counters</code> instance with default counter values (0).
    */
   public Counters()
   {
      this.ctrs = new long[Counter.values().length];
   }

   /**
    * Gets the value of the specified counter
    * 
    * @param counter
    *           the counter type to get the value of
    * @return the value of the given counter
    * @throws NullPointerException
    *            if counter is null
    */
   public long getCounter(final Counter counter)
   {
      Validate.notNull(counter, "counter must not be null");
      return this.ctrs[counter.ordinal()];
   }

   /**
    * Sets the value of the specified counter
    * 
    * @param counter
    *           the counter type to set the value of
    * @param value
    *           the new value of the counter
    * @throws NullPointerException
    *            if counter is null
    * @throws IllegalArgumentException
    *            if value is negative
    */
   public void setCounter(final Counter counter, final long value)
   {
      Validate.notNull(counter, "counter must not be null");
      Validate.isTrue(value >= 0, "value must be >= 0 [%s]", value);
      this.ctrs[counter.ordinal()] = value;
   }
}
