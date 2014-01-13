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
// Date: Nov 7, 2013
// ---------------------

package com.cleversafe.oom.statistic;

public enum Stat
{
   /**
    * Counter.BYTES / Counter.DURATION
    */
   THROUGHPUT,

   /**
    * Counter.BYTES / Counter.ACTIVE_DURATION
    */
   ACTIVE_THROUGHPUT,

   /**
    * Counter.BYTES / elapsed duration
    */
   ELAPSED_THROUGHPUT,

   /**
    * Counter.DURATION / Counter.COUNT
    */
   DURATION_AVG,

   /**
    * Counter.BYTES / Counter.COUNT
    */
   BYTES_AVG,

   /**
    * Counter.TTFB / Counter.COUNT
    */
   TTFB_AVG,

   /**
    * Counter.COUNT / elapsed duration
    */
   RATE
}
