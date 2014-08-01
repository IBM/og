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
// Date: Aug 1, 2014
// ---------------------

package com.cleversafe.og.consumer;

@SuppressWarnings("serial")
public class ConsumerException extends RuntimeException
{
   public ConsumerException()
   {}

   public ConsumerException(final String message)
   {
      super(message);
   }

   public ConsumerException(final Throwable cause)
   {
      super(cause);
   }

   public ConsumerException(final String message, final Throwable cause)
   {
      super(message, cause);
   }

   public ConsumerException(
         final String message,
         final Throwable cause,
         final boolean enableSuppression,
         final boolean writableStackTrace)
   {
      super(message, cause, enableSuppression, writableStackTrace);
   }
}
