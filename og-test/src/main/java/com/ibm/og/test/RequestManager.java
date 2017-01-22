/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.test;

import com.ibm.og.api.Request;
import com.google.common.base.Supplier;

/**
 * A manager and supplier of requests. Simple implementations may provide no additional
 * functionality over a basic {@code Supplier&lt;Request&gt;}, while more sophisticated
 * implementations may vary requests over time, received responses, etc.
 * 
 * @since 1.0
 */
public interface RequestManager extends Supplier<Request> {
}
