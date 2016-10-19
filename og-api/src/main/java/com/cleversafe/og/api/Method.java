/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.cleversafe.og.api;

import java.security.InvalidParameterException;

/**
 * An enumeration of supported http methods
 * 
 * @since 1.0
 */
public enum Method {
  GET, HEAD, POST, PUT, DELETE;

  public static Method fromString(final String method) {
    if (method.equals("GET"))
      return GET;
    else if (method.equals("HEAD"))
      return HEAD;
    else if (method.equals("POST"))
      return POST;
    else if (method.equals("PUT"))
      return PUT;
    else if (method.equals("DELETE"))
      return DELETE;
    else
      throw new InvalidParameterException("Attempted to use unsupported method " + method);
  }
}
