/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
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

  /**
   * Gets the context map for this message. Some messages may include additional context to further
   * describe the nature of the message.
   * 
   * @return context map for this message
   */
  Map<String, String> getContext();
}
