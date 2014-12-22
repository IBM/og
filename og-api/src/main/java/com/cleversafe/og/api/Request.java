//
// Copyright (C) 2005-2011 Cleversafe, Inc. All rights reserved.
//
// Contact Information:
// Cleversafe, Inc.
// 222 South Riverside Plaza
// Suite 1700
// Chicago, IL 60606, USA
//
// licensing@cleversafe.com
//
// END-OF-HEADER
//
// -----------------------
// @author: rveitch
//
// Date: Feb 21, 2014
// ---------------------

package com.cleversafe.og.api;

import java.net.URI;

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
   * Gets the uri for this request
   * 
   * @return the uri for this request
   */
  URI getUri();
}
