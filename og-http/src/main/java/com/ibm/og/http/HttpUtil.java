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
        return Method.PUT;
      case READ:
        return Method.GET;
      case METADATA:
        return Method.HEAD;
      case DELETE:
      case MULTIPART_WRITE_ABORT:
        return Method.DELETE;
      case LIST:
        return Method.GET;
      case MULTIPART_WRITE_INITIATE:
      case MULTIPART_WRITE_COMPLETE:
      case WRITE_LEGAL_HOLD:
      case EXTEND_RETENTION:
        return Method.POST;
      case READ_LEGAL_HOLD:
        return Method.GET;
      case DELETE_LEGAL_HOLD:
        return Method.POST;
      case WRITE_COPY:
        return Method.PUT;
      case MULTI_DELETE:
        return Method.POST;
      case PUT_TAGS:
        return Method.PUT;
      case DELETE_TAGS:
        return Method.DELETE;
      case GET_TAGS:
        return Method.GET;
      default:
        throw new IllegalArgumentException(String.format("Unrecognized operation [%s]", operation));
    }
  }
}
