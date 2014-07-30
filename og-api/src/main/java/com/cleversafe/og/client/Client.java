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
// Date: Jan 21, 2014
// ---------------------

package com.cleversafe.og.client;

import com.cleversafe.og.operation.Request;
import com.cleversafe.og.operation.Response;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * An executor of requests.
 */
public interface Client
{
   /**
    * Executes a request asynchronously.
    * 
    * @param request
    *           the request to execute
    * @return A future representing the eventual completion of this request
    */
   ListenableFuture<Response> execute(Request request);

   /**
    * Shuts down this client.
    * 
    * @param immediate
    *           if true, shuts down this client immediately, else shuts down this client gracefully
    * @return a future representing the eventual shutdown of this client. When the future has
    *         completed, a value of true indicates a successful shutdown, while a value of false
    *         indicates some error in the shutdown process
    */
   ListenableFuture<Boolean> shutdown(boolean immediate);
}
