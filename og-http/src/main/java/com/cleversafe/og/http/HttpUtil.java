/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.http;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.cleversafe.og.api.Method;
import com.cleversafe.og.util.Operation;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;

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

  private HttpUtil() {}

  /**
   * Translates the provided method into the corresponding operation
   * 
   * @param method the method to convert
   * @return the translated operation instance
   */
  public static Operation toOperation(final Method method) {
    checkNotNull(method);
    switch (method) {
      case PUT:
      case POST:
        return Operation.WRITE;
      case GET:
      case HEAD:
        return Operation.READ;
      case DELETE:
        return Operation.DELETE;
      default:
        throw new IllegalArgumentException(String.format("Unrecognized method [%s]", method));
    }
  }

  /**
   * Creates a new, mutable, header map from the provided headers map with the x-og headers filtered
   * out.
   */
  public static Map<String, String> filterOutOgHeaders(final Map<String, String> headers) {
    final Map<String, String> filtered = Maps.newHashMap();
    for (final Entry<String, String> header : headers.entrySet()) {
      if (!header.getKey().startsWith("x-og")) {
        filtered.put(header.getKey(), header.getValue());
      }
    }
    return filtered;
  }
}
