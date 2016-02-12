/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
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
