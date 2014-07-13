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
// Date: Feb 6, 2014
// ---------------------

package com.cleversafe.og.operation.manager;

import com.cleversafe.og.operation.Request;

/**
 * A producer of requests.
 */
public interface OperationManager
{
   /**
    * Creates and returns the next request to be executed.
    * 
    * @return the next request, to be immediately executed
    * @throws OperationManagerException
    *            if this call cannot be satisfied
    */
   Request next() throws OperationManagerException;
}
