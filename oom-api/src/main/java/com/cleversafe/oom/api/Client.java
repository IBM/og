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

package com.cleversafe.oom.api;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * An executor of requests.
 * 
 */
public interface Client
{
   /**
    * Executes a request asynchronously.
    * 
    * @param request
    *           the request to execute
    * @throws NullPointerExecption
    *            if request is null
    * @return A future representing the eventual completion of this request
    */
   ListenableFuture<Response> execute(Request request);

   /**
    * Terminates all in progress requests and returns immediately.
    * 
    */
   void shutdownNow();
}
