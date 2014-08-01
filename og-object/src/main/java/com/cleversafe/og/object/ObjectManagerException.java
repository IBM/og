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
// Date: Jan 15, 2014
// ---------------------

package com.cleversafe.og.object;

/**
 * This exception signals that an unexpected event occurred in an object manager instance while
 * attempting to service a request.
 * 
 * @since 1.0
 */
public class ObjectManagerException extends RuntimeException
{
   private static final long serialVersionUID = 1L;

   public ObjectManagerException()
   {}

   public ObjectManagerException(final String message)
   {
      super(message);
   }

   public ObjectManagerException(final Throwable cause)
   {
      super(cause);
   }
}
