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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cleversafe.oom.http.Scheme;

public class OperationConfig
{
   Scheme scheme;
   List<String> hosts;
   String container;
   Map<String, String> queryParams;
   Map<String, String> headers;
   Integer port;

   public OperationConfig()
   {
      this.queryParams = new HashMap<String, String>();
      this.headers = new HashMap<String, String>();
   }

   /**
    * @return the scheme
    */
   public Scheme getScheme()
   {
      return this.scheme;
   }

   /**
    * @return the hosts
    */
   public List<String> getHosts()
   {
      return this.hosts;
   }

   /**
    * @return the container
    */
   public String getContainer()
   {
      return this.container;
   }

   /**
    * @return the queryParams
    */
   public Map<String, String> getQueryParams()
   {
      return this.queryParams;
   }

   /**
    * @return the headers
    */
   public Map<String, String> getHeaders()
   {
      return this.headers;
   }

   /**
    * @return the port
    */
   public Integer getPort()
   {
      return this.port;
   }
}
