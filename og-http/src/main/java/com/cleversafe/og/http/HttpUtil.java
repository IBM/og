//
// Copyright (C) 2005-2011 Cleversafe, Inc. All rights reserved.
//
// Contact Information:
// Cleversafe, Inc.
// 222 South Riverside Plaza
// Suite 1700
// Chicago, IL 60606, USA
//
// licensing@cleversafe.com
//
// END-OF-HEADER
//
// -----------------------
// @author: rveitch
//
// Date: Jun 26, 2014
// ---------------------

package com.cleversafe.og.http;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;
import java.util.List;
import java.util.Locale;

import com.cleversafe.og.api.Method;
import com.cleversafe.og.util.Operation;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;

/**
 * A utility class for working with http components
 * 
 * @since 1.0
 */
public class HttpUtil {
  private static final Splitter URI_SPLITTER = Splitter.on("/").omitEmptyStrings();
  public static final Range<Integer> VALID_STATUS_CODES = Range.closed(100, 599);
  public static final List<Integer> SUCCESS_STATUS_CODES = ImmutableList.of(200, 201, 204);

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
   * Extracts an object name from the provided uri, if it exists
   * 
   * @param uri the uri to extract an object name from
   * @return an object name, if it exists
   */
  public static String getObjectName(final URI uri) {
    checkNotNull(uri);
    checkNotNull(uri.getScheme());
    // make sure this uri uses a known scheme
    Scheme.valueOf(uri.getScheme().toUpperCase(Locale.US));
    final List<String> parts = URI_SPLITTER.splitToList(uri.getPath());

    if (parts.size() == 3)
      return parts.get(2);

    if (parts.size() == 2) {
      try {
        // if 2 parts and first part is an api, must be soh write
        Api.valueOf(parts.get(0).toUpperCase(Locale.US));
        return null;
      } catch (final IllegalArgumentException e) {
        return parts.get(1);
      }
    }
    return null;
  }
}
