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
// Date: Mar 24, 2014
// ---------------------

package com.cleversafe.og.json;

import java.util.concurrent.TimeUnit;

public class ConcurrencyConfig
{
   ConcurrencyType type;
   DistributionType distribution;
   double count;
   TimeUnit unit;
   double ramp;
   TimeUnit rampUnit;

   public ConcurrencyConfig(final double count)
   {
      this();
      this.count = count;
   }

   public ConcurrencyConfig()
   {
      this.type = ConcurrencyType.THREADS;
      this.distribution = DistributionType.UNIFORM;
      this.count = 1.0;
      this.unit = TimeUnit.SECONDS;
      this.ramp = 0.0;
      this.rampUnit = TimeUnit.SECONDS;
   }

   public ConcurrencyType getType()
   {
      return this.type;
   }

   public DistributionType getDistribution()
   {
      return this.distribution;
   }

   public double getCount()
   {
      return this.count;
   }

   public TimeUnit getUnit()
   {
      return this.unit;
   }

   public double getRamp()
   {
      return this.ramp;
   }

   public TimeUnit getRampUnit()
   {
      return this.rampUnit;
   }
}
