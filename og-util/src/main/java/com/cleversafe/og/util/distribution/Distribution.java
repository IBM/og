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
// Date: Oct 23, 2013
// ---------------------

package com.cleversafe.og.util.distribution;

/**
 * An object that emits values based on a configured statistical distribution.
 * 
 * @since 1.0
 */
public interface Distribution
{
   /**
    * 
    * @return the configured average
    */
   double getAverage();

   /**
    * 
    * @return the configured spread
    */
   double getSpread();

   /**
    * 
    * @return the next value as determined by the configured distribution
    */
   double nextSample();
}
