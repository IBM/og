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

package com.cleversafe.og.object.manager;

/**
 * Signals that an unexpected event occurred in an object manager instance while attempting to
 * service a request.
 */
public class ObjectManagerException extends RuntimeException
{
   private static final long serialVersionUID = 1L;

   /**
    * Constructs an <code>ObjectManagerException</code> with <code>null</code> as its error detail
    * message.
    */
   public ObjectManagerException()
   {}

   /**
    * Constructs an <code>ObjectManagerException</code> with the specified detail message.
    * 
    * @param message
    *           The detail message (which is saved for later retrieval by the {@link #getMessage()}
    *           method)
    */
   public ObjectManagerException(final String message)
   {
      super(message);
   }

   public ObjectManagerException(final Throwable cause)
   {
      super(cause);
   }
}
