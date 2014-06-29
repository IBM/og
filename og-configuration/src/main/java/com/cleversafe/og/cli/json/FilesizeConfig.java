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

package com.cleversafe.og.cli.json;

import com.cleversafe.og.cli.json.enums.DistributionType;
import com.cleversafe.og.util.SizeUnit;

public class FilesizeConfig
{
   private final DistributionType distribution;
   private final double average;
   private final SizeUnit averageUnit;
   private final double spread;
   private final SizeUnit spreadUnit;
   private final double weight;

   public FilesizeConfig()
   {
      this.distribution = DistributionType.UNIFORM;
      this.average = 5.0;
      this.averageUnit = SizeUnit.MEBIBYTES;
      this.spread = 0.0;
      this.spreadUnit = SizeUnit.MEBIBYTES;
      this.weight = 1.0;
   }

   /**
    * @return the distribution
    */
   public DistributionType getDistribution()
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
   public SizeUnit getAverageUnit()
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
   public SizeUnit getSpreadUnit()
   {
      return this.spreadUnit;
   }

   /**
    * @return the weight
    */
   public double getWeight()
   {
      return this.weight;
   }
}
