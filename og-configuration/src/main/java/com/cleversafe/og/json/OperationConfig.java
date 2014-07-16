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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cleversafe.og.json.enums.CollectionAlgorithmType;

public class OperationConfig
{
   double weight;
   CollectionAlgorithmType hostAlgorithm;
   List<HostConfig> host;
   Map<String, String> headers;

   public OperationConfig(final double weight)
   {
      this();
      this.weight = weight;
   }

   public OperationConfig()
   {
      this.weight = 0.0;
      this.hostAlgorithm = CollectionAlgorithmType.ROUNDROBIN;
      this.host = new ArrayList<HostConfig>();
      this.headers = new LinkedHashMap<String, String>();
   }

   public double getWeight()
   {
      return this.weight;
   }

   public CollectionAlgorithmType getHostAlgorithm()
   {
      return this.hostAlgorithm;
   }

   public List<HostConfig> getHost()
   {
      return this.host;
   }

   public Map<String, String> getHeaders()
   {
      return this.headers;
   }
}
