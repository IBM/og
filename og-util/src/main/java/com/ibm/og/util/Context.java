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
  public static final String X_OG_MPU_PARTIAL_LAST_PART = "x-og-mpu-partial-last-part";

  // Put Copy Keys
  public static final String X_OG_SSE_SOURCE_OBJECT_NAME = "x_og_sse_source_object_name";
  public static final String X_OG_SSE_SOURCE_OBJECT_CONTAINER_SUFFIX =
      "x_og_sse_source_object_container_suffix";
  public static final String X_OG_SOURCE_CONTAINER_NAME = "x_og_sse_source_object_container_name";
  public static final String X_OG_SOURCE_CONTAINER_PREFIX = "x_og_sse_source_object_container_prefix";
  public static final String X_OG_SSE_SOURCE_OBJECT_SIZE = "x_og_sse_source_object_size";
  public static final String X_OG_SSE_SOURCE_URI = "x_og_sse_source_uri";


  // worm feature
  // todo: should we use the prefix given in the config?
  public static final String X_OG_LEGAL_HOLD_PREFIX = "x-og-legalhold-prefix";
  public static final String X_OG_LEGAL_HOLD_SUFFIX = "x-og-legalhold-suffix";
  public static final String X_OG_LEGAL_HOLD = "Retention-Legal-Hold-ID";
  public static final String X_OG_OBJECT_RETENTION = "Retention-Period";
  public static final String X_OG_OBJECT_RETENTION_EXT = "Additional-Retention-Period";
  public static final String X_OG_CONTENT_MD5 = "Content-MD5";

  public static final String X_OG_OBJECT_RESTORE_PERIOD = "x-og-object-restore-period";
  public static final String X_OG_ARCHIVE_TRANSITION_PERIOD = "x-og-archive-transition-period";

  // container protection
  public static final String X_OG_CONTAINER_MINIMUM_RETENTION_PERIOD = "x-og-container-minimum-retention-period";
  public static final String X_OG_CONTAINER_MAXIMUM_RETENTION_PERIOD = "x-og-container-maximum-retention-period";
  public static final String X_OG_CONTAINER_DEFAULT_RETENTION_PERIOD = "x-og-container-default-retention-period";


  public static final String X_OG_LIST_MAX_KEYS = "x-og-list-max-keys";
  public static final String X_OG_LIST_SESSION_ID= "x-og-list-session-id";
  public static final String X_OG_LIST_SESSION_TYPE= "x-og-list-session-type";
  public static final String X_OG_LIST_START_AFTER = "x-og-list-start-after";
  public static final String X_OG_LIST_NEXT_MARKER = "x-og-list-next-marker";
  public static final String X_OG_LIST_NEXT_CONTINUATION_TOKEN = "x-og-list-next-continuation-token";
  public static final String X_OG_LIST_PREFIX = "x-og-list-prefix";
  public static final String X_OG_LIST_DELIMITER = "x-og-list-delimiter";
  public static final String X_OG_LIST_OBJECT_VERSIONS_KEY_MARKER = "x-og-list-object-versions-key-marker";
  public static final String X_OG_LIST_OBJECT_VERSIONS_VERSION_ID = "x-og-list-object-versions-version-id";
  public static final String X_OG_LIST_OBJECT_VERSIONS_NEXT_KEY_MARKER = "x-og-list-object-versions-next-key-marker";
  public static final String X_OG_LIST_OBJECT_VERSIONS_NEXT_VERSION_ID_MARKER = "x-og-list-object-versions-next-version-id-marker";

  public static final String X_OG_LIST_REQ_NUM = "x-og-list-req-num";
  public static final String X_OG_LIST_MAX_REQS = "x-og-list-max-reqs";


  public static final String X_OG_LIST_IS_TRUNCATED = "x-og-list-is-truncated";
  public static final String X_OG_NUM_LIST_CONTENTS = "x-og-list-num-contents";
  public static final String X_OG_NUM_LIST_COMMON_PREFIXES = "x-og-list-num-common-prefixes";

  public static final String X_OG_MULTI_DELETE_REQUEST_OBJECTS_COUNT = "x-og-multi-delete-request-objects-count";
  public static final String X_OG_MULTI_DELETE_REQUST_FAILED = "x-og-multi-delete-request-failed";
  public static final String X_OG_MULTI_DELETE_SUCCESS_OBJECTS_COUNT = "x-og-multi-delete-success-objects-count";
  public static final String X_OG_MULTI_DELETE_FAILED_OBJECTS_COUNT = "x-og-multi-delete-failed-objects-count";
  public static final String X_OG_STATIC_WEBSITE_VIRTUAL_HOST_SUFFIX = "x-og-website-read";

  public static final String X_OG_OBJECT_VERSION = "x-og-object-version";
  public static final String X_OG_OBJECT_VERSION_SELECTION = "x-og-object-version-selection";


  private Context() {}
}
