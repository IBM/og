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

package com.cleversafe.og.producer;

/**
 * A producer of values
 * 
 * @param <T>
 *           the type of value to produce
 * @since 1.0
 */
public interface Producer<T>
{
   /**
    * Returns the next value from this producer
    * 
    * @return the next value. Consecutive calls may return previously returned values or new values.
    */
   T produce();
}
