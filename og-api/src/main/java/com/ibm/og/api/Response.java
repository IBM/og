/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.api;

/**
 * An object that describes an http response
 * 
 * @since 1.0
 */
public interface Response extends Message {
  /**
   * Gets the status code for this response
   * 
   * @return the status code for this response
   */
  int getStatusCode();

  RequestTimestamps getRequestTimestamps();
}
