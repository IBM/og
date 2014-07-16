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

import com.cleversafe.og.json.enums.ConcurrencyType;

public class ConcurrencyConfig
{
   ConcurrencyType type;
   double count;
   TimeUnit unit;
   double rampup;
   TimeUnit rampupUnit;

   public ConcurrencyConfig(final double count)
   {
      this();
      this.count = count;
   }

   public ConcurrencyConfig()
   {
      this.type = ConcurrencyType.THREADS;
      this.count = 1.0;
      this.unit = TimeUnit.SECONDS;
      this.rampup = 0.0;
      this.rampupUnit = TimeUnit.SECONDS;
   }

   public ConcurrencyType getType()
   {
      return this.type;
   }

   public double getCount()
   {
      return this.count;
   }

   public TimeUnit getUnit()
   {
      return this.unit;
   }

   public double getRampup()
   {
      return this.rampup;
   }

   public TimeUnit getRampupUnit()
   {
      return this.rampupUnit;
   }
}
