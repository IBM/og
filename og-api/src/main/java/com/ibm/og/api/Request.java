/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.api;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * An object that describes an http request
 * 
 * @since 1.0
 */
public interface Request extends Message {
  /**
   * Gets the http method for this request
   * 
   * @return the http method for this request
   * @see Method
   */
  Method getMethod();

  /**
   * Gets the OG operation for this request
   *
   * @return the operation that corresponds to this request
   * @see Operation
   */
  Operation getOperation();

  /**
   * Gets the uri for this request
   * 
   * @return the uri for this request
   */
  URI getUri();

  /**
   * Gets the query parameters for this request
   * 
   * @return the query parameters for this request
   */
  Map<String, List<String>> getQueryParameters();
}
