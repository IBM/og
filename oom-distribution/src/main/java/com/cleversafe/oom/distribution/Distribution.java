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

package com.cleversafe.oom.distribution;

/**
 * An object that emits values based on a configured statistical distribution.
 */
public interface Distribution
{
   /**
    * 
    * @return the configured mean
    */
   double getMean();

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
