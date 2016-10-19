/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.cleversafe.og.object;

/**
 * This exception signals that an unexpected event occurred in an object manager instance while
 * attempting to service a request.
 * 
 * @since 1.0
 */
@SuppressWarnings("serial")
public class ObjectManagerException extends RuntimeException {
  public ObjectManagerException() {}

  public ObjectManagerException(final String message) {
    super(message);
  }

  public ObjectManagerException(final Throwable cause) {
    super(cause);
  }

  public ObjectManagerException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
