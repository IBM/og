/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.http;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;

import com.ibm.og.api.Method;
import com.ibm.og.api.Operation;

/**
 * A utility class for working with http components
 * 
 * @since 1.0
 */
public class HttpUtil {
  public static final Set<Integer> VALID_STATUS_CODES =
      ContiguousSet.create(Range.closed(100, 599), DiscreteDomain.integers());
  public static final Set<Integer> SUCCESS_STATUS_CODES =
      ContiguousSet.create(Range.closed(200, 299), DiscreteDomain.integers());
  public static final Set<Integer> DELETE_HANDLING_STATUS_CODES =
          ContiguousSet.create(Range.closed(100, 599), DiscreteDomain.integers());

  private HttpUtil() {}

  /**
   * Translates the provided operation into the corresponding method
   * 
   * @param operation the operation to convert
   * @return the translated operation instance
   */
  public static Method toMethod(final Operation operation) {
    checkNotNull(operation);
    switch (operation) {
      case WRITE:
      case OVERWRITE:
      case MULTIPART_WRITE_PART:
      case WRITE_COPY:
      case PUT_TAGS:
      case PUT_OBJECT_LOCK_RETENTION:
      case PUT_OBJECT_LOCK_LEGAL_HOLD:
        return Method.PUT;
      case READ:
      case LIST:
      case READ_LEGAL_HOLD:
      case GET_TAGS:
      case GET_OBJECT_LOCK_RETENTION:
      case GET_OBJECT_LOCK_LEGAL_HOLD:
        return Method.GET;
      case METADATA:
        return Method.HEAD;
      case DELETE:
      case MULTIPART_WRITE_ABORT:
      case DELETE_TAGS:
        return Method.DELETE;
      case MULTIPART_WRITE_INITIATE:
      case MULTIPART_WRITE_COMPLETE:
      case WRITE_LEGAL_HOLD:
      case EXTEND_RETENTION:
      case DELETE_LEGAL_HOLD:
      case MULTI_DELETE:
        return Method.POST;
      default:
        throw new IllegalArgumentException(String.format("Unrecognized operation [%s]", operation));
    }
  }
}
