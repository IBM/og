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
// Date: Jul 16, 2014
// ---------------------

package com.cleversafe.og.json;

public class HostConfig
{
   String host;
   double weight;

   public HostConfig(final String host)
   {
      this();
      this.host = host;
   }

   public HostConfig()
   {
      this.host = null;
      this.weight = 1.0;
   }

   public String getHost()
   {
      return this.host;
   }

   public double getWeight()
   {
      return this.weight;
   }
}
