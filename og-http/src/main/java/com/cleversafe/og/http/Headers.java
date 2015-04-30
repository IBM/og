/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.http;

/**
 * Custom Object Generator header keys
 * 
 * @since 1.0
 */
public class Headers {
  private Headers() {}

  public static final String X_OG_REQUEST_ID = "x-og-request-id";
  public static final String X_OG_OBJECT_NAME = "x-og-object-name";
  public static final String X_OG_USERNAME = "x-og-username";
  public static final String X_OG_PASSWORD = "x-og-password";
  public static final String X_OG_RESPONSE_BODY_CONSUMER = "x-og-response-body-consumer";
  public static final String X_OG_CONTAINER_SUFFIX = "x-og-container-suffix";
}
