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

package com.cleversafe.oom.client;

import com.cleversafe.oom.operation.Operation;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * A client for execution of <code>Operations</code> against a target host.
 * 
 * @param <T>
 *           the type of operations this client supports
 */
public interface Client<T extends Operation>
{
   /**
    * Executes an operation asynchronously.
    * 
    * @param operation
    *           the operation to execute
    * @throws NullPointerExecption
    *            if operation is null
    * @return A future representing the eventual completion of this operation
    */
   ListenableFuture<T> execute(T operation);

   /**
    * Terminates all in progress requests and returns immediately.
    * 
    */
   void shutdownNow();
}
