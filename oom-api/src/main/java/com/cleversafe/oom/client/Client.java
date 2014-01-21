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

/**
 * A client for execution of <code>Operations</code> against a target host.
 * 
 * @param <T>
 *           the type of operations this client supports
 */
public interface Client<T extends Operation>
{
   /**
    * Executes an operation asynchronously, invoking the specified callback when the operation has
    * been fully executed.
    * 
    * @param operation
    *           the operation to execute
    * @param callback
    *           the callback to invoke upon completion of operation execution
    * @throws NullPointerExecption
    *            if operation is null
    * @throws NullPointerException
    *            if callback is null
    */
   void execute(T operation, OperationCallback<T> callback);
}
