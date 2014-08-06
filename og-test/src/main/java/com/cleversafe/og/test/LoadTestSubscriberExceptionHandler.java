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
// Date: Aug 6, 2014
// ---------------------

package com.cleversafe.og.test;

import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;

public class LoadTestSubscriberExceptionHandler implements SubscriberExceptionHandler
{
   private static final Logger _logger =
         LoggerFactory.getLogger(LoadTestSubscriberExceptionHandler.class);
   private LoadTest test;

   public LoadTestSubscriberExceptionHandler()
   {}

   @Override
   public void handleException(final Throwable exception, final SubscriberExceptionContext context)
   {
      _logger.error("Exception while processing subscriber", exception);
      this.test.abortTest();
   }

   public void setLoadTest(final LoadTest test)
   {
      this.test = checkNotNull(test);
   }
}
