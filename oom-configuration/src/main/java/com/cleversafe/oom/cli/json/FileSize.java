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

public class FileSize
{
   String distribution;
   double average;
   String averageUnit;
   double spread;
   String spreadUnit;
   long weight;

   public FileSize()
   {
      this.distribution = "normal";
      this.average = 5.0;
      this.averageUnit = "mb";
      this.spread = 1.25;
      this.spreadUnit = "mb";
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
   public double getAverage()
   {
      return this.average;
   }

   /**
    * @return the averageUnit
    */
   public String getAverageUnit()
   {
      return this.averageUnit;
   }

   /**
    * @return the spread
    */
   public double getSpread()
   {
      return this.spread;
   }

   /**
    * @return the spreadUnit
    */
   public String getSpreadUnit()
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
