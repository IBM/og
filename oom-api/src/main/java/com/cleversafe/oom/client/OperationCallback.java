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
 * A callback to be invoked by a <code>Client</code> instance.
 * 
 * @param <T>
 *           the type of operation this callback should be used for
 */
public interface OperationCallback<T extends Operation>
{
   /**
    * Invokes this callback with a fully executed operation
    * 
    * @param operation
    *           the operation to process
    * @throws NullPointerException
    *            if operation is null
    */
   void callback(T operation);
}
