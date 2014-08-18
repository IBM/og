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

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class OperationConfig
{
   double weight;
   SelectionType hostSelection;
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
      this.hostSelection = SelectionType.ROUNDROBIN;
      this.host = Lists.newArrayList();
      this.headers = Maps.newLinkedHashMap();
   }

   public double getWeight()
   {
      return this.weight;
   }

   public SelectionType getHostSelection()
   {
      return this.hostSelection;
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
