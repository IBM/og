/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.http;

import com.cleversafe.og.api.AuthenticatedRequest;
import com.cleversafe.og.api.Request;

/**
 * A {@code Request} authentication class
 * 
 * @since 1.0
 */
public interface HttpAuth {
  /**
   * Creates an {@code AuthenticatedRequest} which wraps an existing request. The returned request
   * may contain additional or modified request parameters and headers, and will contain a content
   * stream that is ready for request execution.
   * 
   * @param request the underlying request
   * @return a request object which contains all necessary authentication metadata
   */
  AuthenticatedRequest authenticate(Request request);
}
