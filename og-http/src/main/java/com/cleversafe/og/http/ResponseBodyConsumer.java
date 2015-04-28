/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * A response body consumer takes a response body and consumes it, processing the body for any
 * relevant metadata
 * 
 * @since 1.0
 */
public interface ResponseBodyConsumer {
  /**
   * Consumes a response body
   * 
   * @param statusCode the associated status code for the response body
   * @param response the response body
   * @return a map containing elements that were processed based on the content of the response body
   * @throws IllegalArgumentException if statusCode is not a valid status code
   * @throws IOException if an exception occurs while processing the response body
   */
  Map<String, String> consume(int statusCode, InputStream response) throws IOException;
}
