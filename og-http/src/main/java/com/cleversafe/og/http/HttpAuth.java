/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
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
