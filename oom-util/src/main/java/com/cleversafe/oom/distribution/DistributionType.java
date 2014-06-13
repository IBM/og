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

public enum DistributionType
{
   UNIFORM, NORMAL, LOGNORMAL;

   public static DistributionType parseDistribution(final String distribution)
   {
      return DistributionType.valueOf(distribution.toUpperCase());
   }
}
