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
// Date: Apr 7, 2014
// ---------------------

package com.cleversafe.og.test.operation.manager;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Locale;

import javax.inject.Inject;

import com.cleversafe.og.operation.OperationManager;
import com.cleversafe.og.operation.OperationManagerException;
import com.cleversafe.og.operation.Request;
import com.google.common.base.Supplier;

public class SimpleOperationManager implements OperationManager
{
   private final Supplier<Supplier<Request>> requestMix;

   @Inject
   public SimpleOperationManager(final Supplier<Supplier<Request>> requestMix)
   {
      this.requestMix = checkNotNull(requestMix);
   }

   @Override
   public Request next() throws OperationManagerException
   {
      try
      {
         final Supplier<Request> producer = this.requestMix.get();
         return producer.get();
      }
      catch (final Exception e)
      {
         throw new OperationManagerException(e);
      }
   }

   @Override
   public String toString()
   {
      return String.format(Locale.US, "SimpleOperationManager [%nrequestMix=%s%n]", this.requestMix);
   }
}
