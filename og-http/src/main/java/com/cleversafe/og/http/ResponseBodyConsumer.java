/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
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
