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
// Date: Apr 8, 2014
// ---------------------

package com.cleversafe.og.producer;

@SuppressWarnings("serial")
public class ProducerException extends RuntimeException
{
   public ProducerException()
   {}

   public ProducerException(final String message)
   {
      super(message);
   }

   public ProducerException(final Throwable cause)
   {
      super(cause);
   }

   public ProducerException(final String message, final Throwable cause)
   {
      super(message, cause);
   }
}
