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
import com.cleversafe.og.operation.Response;

/**
 * A producer of requests and consumer of responses.
 */
public interface OperationManager
{
   /**
    * Creates and returns the next request to be executed. This method will block until it is time
    * for the next request.
    * 
    * @return the next request, to be immediately executed
    * @throws OperationManagerException
    *            if this call cannot be satisfied
    */
   Request next() throws OperationManagerException;

   /**
    * Consumes an operation. Implementations of this interface may optionally use information
    * gathered from request/response pairs to inform the creation of future requests.
    * 
    * @param request
    *           the request for a completed operation
    * @param response
    *           the response for a completed operation
    * @throws NullPointerException
    *            if request is null
    * @throws NullPointerException
    *            if response is null
    */
   void complete(Request request, Response response);
}
