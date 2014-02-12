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

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import com.cleversafe.oom.object.ObjectName;
import com.cleversafe.oom.object.manager.ObjectManagerException;
import com.cleversafe.oom.operation.HTTPMethod;
import com.cleversafe.oom.operation.HTTPOperation;
import com.cleversafe.oom.operation.OperationState;
import com.cleversafe.oom.operation.OperationType;
import com.cleversafe.oom.operation.entity.Entities;
import com.cleversafe.oom.operation.manager.OperationManager;
import com.cleversafe.oom.soh.operation.SOHPutObjectOperation;

public class SOHOperationManager implements OperationManager<HTTPOperation>
{
   private final SOHOperationManagerConfiguration config;

   public SOHOperationManager(final SOHOperationManagerConfiguration config)

   {
      this.config = checkNotNull(config, "config must not be null");
   }

   @Override
   public HTTPOperation next()
   {
      final OperationType operationType =
            this.config.getOperationTypeMix().getNextOperationType(
                  this.config.getStats().getVaultFill());

      // TODO fix try/catch logic, should never return null from next()
      try
      {
         switch (operationType)
         {
            case READ :
               return createReadOperation();
            case DELETE :
               return createDeleteOperation();
            default :
               return createWriteOperation();
         }
      }
      catch (final ObjectManagerException e)
      {
         return null;
      }
   }

   public HTTPOperation createWriteOperation()
   {
      final URL url = createURL(null);
      final HTTPOperation operation =
            new SOHPutObjectOperation(OperationType.WRITE, url, HTTPMethod.PUT);
      operation.setOperationState(OperationState.ACTIVE);
      final long nextSize = (long) this.config.getSizeDistribution().nextChoice().nextSample();
      operation.setRequestEntity(Entities.random(nextSize));

      return operation;
   }

   public HTTPOperation createReadOperation() throws ObjectManagerException
   {
      final ObjectName objectName = this.config.getObjectManager().acquireNameForRead();
      final URL url = createURL(objectName);
      final HTTPOperation operation = new HTTPOperation(OperationType.READ, url, HTTPMethod.GET);
      operation.setObjectName(objectName);
      operation.setOperationState(OperationState.ACTIVE);
      operation.setRequestHeader("Accept-Encoding", "Identity");
      operation.setRequestEntity(Entities.empty());
      return operation;
   }

   public HTTPOperation createDeleteOperation() throws ObjectManagerException
   {
      final ObjectName objectName = this.config.getObjectManager().getNameForDelete();
      final URL url = createURL(objectName);
      final HTTPOperation operation =
            new HTTPOperation(OperationType.DELETE, url, HTTPMethod.DELETE);
      operation.setObjectName(objectName);
      operation.setOperationState(OperationState.ACTIVE);
      operation.setRequestEntity(Entities.empty());
      return operation;
   }

   private URL createURL(final ObjectName objectName)
   {
      final String host = getHost();
      final String vault = this.config.getVault();
      String url = "http://" + host + "/" + vault;
      if (objectName != null)
         url += "/" + objectName;
      try
      {
         return new URL(url);
      }
      catch (final MalformedURLException e)
      {
         return null;
      }
   }

   private String getHost()
   {
      final List<String> hosts = this.config.getHosts();
      Collections.shuffle(hosts);
      return hosts.get(0);
   }

   @Override
   public void complete(final HTTPOperation operation)
   {
      // do nothing, this implementation is not responsive based on the state of completed
      // operations, nor does it recycle completed operation instances for later use
   }
}
