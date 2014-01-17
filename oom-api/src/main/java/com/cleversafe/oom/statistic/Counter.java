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
// Date: Oct 27, 2013
// ---------------------

package com.cleversafe.oom.statistic;

public enum Counter
{

   /**
    * Count of started operations
    */
   COUNT,

   /**
    * Count of active operations
    */
   ACTIVE_COUNT,

   /**
    * Count of minimum active operations
    */
   ACTIVE_COUNT_MIN,

   /**
    * Count of maximum active operations
    */
   ACTIVE_COUNT_MAX,

   /**
    * Count of completed operations
    */
   COMPLETE_COUNT,

   /**
    * Count of failed operations
    */
   FAILURE_COUNT,

   /**
    * Count of aborted operations
    */
   ABORT_COUNT,

   /**
    * Sum of operation durations, in nanoseconds
    */
   DURATION,

   /**
    * Sum of durations when one or more operations were active, in nanoseconds
    */
   ACTIVE_DURATION,

   /**
    * Sum of operation bytes
    */
   BYTES,

   /**
    * Sum of operation ttfb, in nanoseconds
    */
   TTFB
}
