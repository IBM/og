/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.cleversafe.og.api;

import java.io.InputStream;

/**
 * A request which has been authenticated via an {@code HttpAuth} implementation
 * 
 * Authenticated requests may contain additional request parameters or headers, and will also
 * contain an input stream which has been prepared for request execution.
 * 
 * @since 1.0
 */
public interface AuthenticatedRequest extends Request {
  /**
   * Gets the input stream for the content of this request
   * 
   * @return request content input stream, or null if this request has no content
   */
  InputStream getContent();

  /**
   * Gets the length of the content stream returned by {@code getContent}
   * 
   * @return length of the content stream
   */
  long getContentLength();
}
