/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
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
