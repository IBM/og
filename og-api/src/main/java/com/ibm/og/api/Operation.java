/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.api;


/**
 * An enum for describing an operation type
 * 
 * @since 1.0
 */
public enum Operation {
  ALL, WRITE, OVERWRITE, READ, METADATA, DELETE, LIST, CONTAINER_LIST, CONTAINER_CREATE,
  MULTIPART_WRITE, MULTIPART_WRITE_INITIATE, MULTIPART_WRITE_PART, MULTIPART_WRITE_COMPLETE, MULTIPART_WRITE_ABORT,
  WRITE_COPY, WRITE_LEGAL_HOLD, DELETE_LEGAL_HOLD, READ_LEGAL_HOLD, EXTEND_RETENTION, OBJECT_RESTORE,
  PUT_BUCKET_LIFECYCLE, GET_BUCKET_LIFECYCLE
}
