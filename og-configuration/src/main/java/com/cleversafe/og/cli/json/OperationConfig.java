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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cleversafe.og.cli.json.enums.CollectionAlgorithmType;

public class OperationConfig
{
   CollectionAlgorithmType hostAlgorithm;
   List<String> hosts;
   Map<String, String> headers;

   public OperationConfig()
   {
      this.hostAlgorithm = CollectionAlgorithmType.ROUNDROBIN;
      this.headers = new LinkedHashMap<String, String>();
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
