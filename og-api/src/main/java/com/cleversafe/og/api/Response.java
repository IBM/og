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
}
