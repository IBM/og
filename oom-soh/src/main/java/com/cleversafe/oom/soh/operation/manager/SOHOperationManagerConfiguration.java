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
// Date: Feb 11, 2014
// ---------------------

package com.cleversafe.oom.soh.operation.manager;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import com.cleversafe.oom.distribution.Distribution;
import com.cleversafe.oom.object.manager.ObjectManager;
import com.cleversafe.oom.operation.OperationTypeMix;
import com.cleversafe.oom.statistic.Statistics;
import com.cleversafe.oom.util.WeightedRandomChoice;

public class SOHOperationManagerConfiguration
{
   private final String vault;
   private final List<String> hosts;
   private final OperationTypeMix operationTypeMix;
   private final WeightedRandomChoice<Distribution> sizeDistribution;
   private final ObjectManager objectManager;
   private final Statistics stats;

   public SOHOperationManagerConfiguration(
         final String vault,
         final List<String> hosts,
         final OperationTypeMix operationTypeMix,
         final WeightedRandomChoice<Distribution> sizeDistribution,
         final ObjectManager objectManager,
         final Statistics stats)
   {
      this.vault = checkNotNull(vault, "vault must not be null");
      this.hosts = checkNotNull(hosts, "hosts must not be null");
      checkArgument(hosts.size() > 0, "hosts must contain at least one host [%s]", hosts.size());
      this.operationTypeMix = checkNotNull(operationTypeMix, "operationTypeMix must not be null");
      this.sizeDistribution = checkNotNull(sizeDistribution, "sizeDistribution must not be null");
      this.objectManager = checkNotNull(objectManager, "objectManager must not be null");
      this.stats = checkNotNull(stats, "stats must not be null");
   }

   /**
    * @return the vault
    */
   public String getVault()
   {
      return this.vault;
   }

   /**
    * @return the hosts
    */
   public List<String> getHosts()
   {
      return this.hosts;
   }

   /**
    * @return the operationTypeMix
    */
   public OperationTypeMix getOperationTypeMix()
   {
      return this.operationTypeMix;
   }

   /**
    * @return the sizeDistribution
    */
   public WeightedRandomChoice<Distribution> getSizeDistribution()
   {
      return this.sizeDistribution;
   }

   /**
    * @return the objectManager
    */
   public ObjectManager getObjectManager()
   {
      return this.objectManager;
   }

   /**
    * @return the stats
    */
   public Statistics getStats()
   {
      return this.stats;
   }
}
