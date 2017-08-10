/*
 * Copyright (c) IBM Corporation 2016. All Rights Reserved. Project name: Object Generator This
 * project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.util;

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
  public static final String X_OG_CONTAINER_PREFIX = "x-og-container-prefix";
  public static final String X_OG_CONTAINER_SUFFIX = "x-og-container-suffix";
  public static final String X_OG_CONTAINER_NAME = "x-og-container-name";
  public static final String X_OG_USERNAME = "x-og-username";
  public static final String X_OG_PASSWORD = "x-og-password";
  public static final String X_OG_STORAGE_ACCOUNT_NAME = "x-og-storage-account-name";
  // FIXME refactor this into separate openstack guice module
  public static final String X_OG_KEYSTONE_TOKEN = "x-og-keystone-token";
  public static final String X_OG_IAM_TOKEN = "x-og-iam-token";
  public static final String X_OG_RESPONSE_BODY_CONSUMER = "x-og-response-body-consumer";
  // Multipart Context Keys
  public static final String X_OG_MULTIPART_REQUEST = "x-og-multipart-request";
  public static final String X_OG_MULTIPART_UPLOAD_ID = "x-og-multipart-upload-id";
  public static final String X_OG_MULTIPART_PART_NUMBER = "x-og-multipart-part-number";
  public static final String X_OG_MULTIPART_PART_SIZE = "x-og-multipart-part-size";
  public static final String X_OG_MULTIPART_CONTAINER = "x-og-multipart-container";
  public static final String X_OG_MULTIPART_BODY_DATA_TYPE = "x-og-multipart-body-data-type";
  public static final String X_OG_MULTIPART_MAX_PARTS = "x-og-multipart-max-parts";
  // Put Copy Keys
  public static final String X_OG_SSE_SOURCE_OBJECT_NAME = "x_og_sse_source_object_name";
  public static final String X_OG_SSE_SOURCE_OBJECT_CONTAINER_SUFFIX =
      "x_og_sse_source_object_container_suffix";
  public static final String X_OG_SSE_SOURCE_OBJECT_SIZE = "x_og_sse_source_object_size";
  public static final String X_OG_SSE_SOURCE_URI = "x_og_sse_source_uri";


  // worm feature
  // todo: should we use the prefix given in the config?
  public static final String X_OG_LEGAL_HOLD_PREFIX = "x-og-legalhold-prefix";
  public static final String X_OG_LEGAL_HOLD_SUFFIX = "x-og-legalhold-suffix";
  public static final String X_OG_LEGAL_HOLD = "Retention-Legal-Hold-ID";
  public static final String X_OG_NUM_LEGAL_HOLDS = "x-og-num-legal-holds";
  public static final String X_OG_OBJECT_RETENTION = "Retention-Period";
  public static final String X_OG_CONTENT_MD5 = "Content-MD5";

  private Context() {}
}
