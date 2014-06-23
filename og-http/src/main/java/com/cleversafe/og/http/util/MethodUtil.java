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
// Date: Jun 23, 2014
// ---------------------

package com.cleversafe.og.http.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.operation.Method;
import com.cleversafe.og.util.OperationType;

public class MethodUtil
{
   private static Logger _logger = LoggerFactory.getLogger(MethodUtil.class);

   private MethodUtil()
   {}

   public static OperationType toOperationType(final Method method)
   {
      switch (method)
      {
         case GET :
         case HEAD :
            return OperationType.READ;
         case POST :
         case PUT :
            return OperationType.WRITE;
         case DELETE :
            return OperationType.DELETE;
         default :
            throw new RuntimeException(String.format("Unrecognized method [%s]", method));
      }
   }
}
