/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.cleversafe.og.util;

/**
 * Custom Object Generator Context keys
 * 
 * @since 1.0
 */
public class Context {
  public static final String X_OG_REQUEST_ID = "x-og-request-id";
  public static final String X_OG_OBJECT_NAME = "x-og-object-name";
  // FIXME header to differentiate between objects which can be stored in the object manager
  public static final String X_OG_SEQUENTIAL_OBJECT_NAME = "x-og-sequential-object-name";
  public static final String X_OG_OBJECT_SIZE = "x-og-object-size";
  public static final String X_OG_CONTAINER_SUFFIX = "x-og-container-suffix";
  public static final String X_OG_USERNAME = "x-og-username";
  public static final String X_OG_PASSWORD = "x-og-password";
  // FIXME refactor this into separate openstack guice module
  public static final String X_OG_KEYSTONE_TOKEN = "x-og-keystone-token";
  public static final String X_OG_RESPONSE_BODY_CONSUMER = "x-og-response-body-consumer";
  // Multipart Context Keys
  public static final String X_OG_MULTIPART_REQUEST = "x-og-multipart-request";
  public static final String X_OG_MULTIPART_UPLOAD_ID = "x-og-multipart-upload-id";
  public static final String X_OG_MULTIPART_PART_NUMBER = "x-og-multipart-part-number";
  public static final String X_OG_MULTIPART_PART_SIZE = "x-og-multipart-part-size";
  public static final String X_OG_MULTIPART_CONTAINER = "x-og-multipart-container";
  public static final String X_OG_MULTIPART_BODY_DATA_TYPE = "x-og-multipart-body-data-type";

  private Context() {}
}
