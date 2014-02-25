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
// Date: Feb 25, 2014
// ---------------------

package com.cleversafe.oom.api;

public class OperationManagerException extends Exception
{
   private static final long serialVersionUID = 1L;

   public OperationManagerException()
   {}

   public OperationManagerException(final String message)
   {
      super(message);
   }

   public OperationManagerException(final Throwable cause)
   {
      super(cause);
   }

   public OperationManagerException(final String message, final Throwable cause)
   {
      super(message, cause);
   }

   public OperationManagerException(
         final String message,
         final Throwable cause,
         final boolean enableSuppression,
         final boolean writableStackTrace)
   {
      super(message, cause, enableSuppression, writableStackTrace);
   }
}
