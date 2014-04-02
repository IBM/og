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

package com.cleversafe.oom.cli.json;

import com.cleversafe.oom.util.SizeUnit;

// TODO decimal sizes?
public class FileSize
{
   String distribution;
   long average;
   SizeUnit averageUnit;
   long spread;
   SizeUnit spreadUnit;
   long weight;

   public FileSize()
   {
      this.distribution = "normal";
      this.average = 5;
      this.averageUnit = SizeUnit.MEBIBYTES;
      this.spread = 1;
      this.spreadUnit = SizeUnit.MEBIBYTES;
      this.weight = 1;
   }

   /**
    * @return the distribution
    */
   public String getDistribution()
   {
      return this.distribution;
   }

   /**
    * @return the average
    */
   public long getAverage()
   {
      return this.average;
   }

   /**
    * @return the averageUnit
    */
   public SizeUnit getAverageUnit()
   {
      return this.averageUnit;
   }

   /**
    * @return the spread
    */
   public long getSpread()
   {
      return this.spread;
   }

   /**
    * @return the spreadUnit
    */
   public SizeUnit getSpreadUnit()
   {
      return this.spreadUnit;
   }

   /**
    * @return the weight
    */
   public long getWeight()
   {
      return this.weight;
   }
}
