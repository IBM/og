/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.api;

import java.util.Map;

/**
 * An object that describes an http message
 * 
 * @since 1.0
 */
public interface Message {
  Map<String, String> headers();

  /**
   * Gets the description of the body for this message
   * 
   * @return the description of the body for this message
   */
  Body getBody();
}
