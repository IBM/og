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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cleversafe.og.cli.json.enums.CollectionAlgorithmType;

public class OperationConfig
{
   private final double weight;
   private final CollectionAlgorithmType hostAlgorithm;
   private final List<String> hosts;
   private final Map<String, String> headers;

   public OperationConfig()
   {
      this.weight = 0.0;
      this.hostAlgorithm = CollectionAlgorithmType.ROUNDROBIN;
      this.hosts = new ArrayList<String>();
      this.headers = new LinkedHashMap<String, String>();
   }

   /**
    * @return the write
    */
   public double getweight()
   {
      return this.weight;
   }

   /**
    * @return the hostAlgorithm
    */
   public CollectionAlgorithmType getHostAlgorithm()
   {
      return this.hostAlgorithm;
   }

   /**
    * @return the hosts
    */
   public List<String> getHosts()
   {
      return this.hosts;
   }

   /**
    * @return the headers
    */
   public Map<String, String> getHeaders()
   {
      return this.headers;
   }
}
