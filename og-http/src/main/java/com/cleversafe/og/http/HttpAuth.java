/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.http;

import java.io.InputStream;
import java.util.Map;

import com.cleversafe.og.api.Request;

/**
 * A creator of http authorization headers
 * 
 * @since 1.0
 */
public interface HttpAuth {
  /**
   * Creates the necessary headers to be added for authorization
   * 
   * @param request the request to create the authorization header value for
   * @return A map of required authorization headers for the provided request
   */
  Map<String, String> getAuthorizationHeaders(final Request request);

  /**
   * Decorates the provided input stream to add the necessary authorization info to the stream. For
   * example, AWS sig4 has a chunked mode where hashes of the chunks of the data stream must be
   * included in between each chunk.
   * 
   * @param request The {@code Request} used for this stream. Contains header information that needs
   *        to be used to generate the write signatures for chunked encoding streams.
   * @param InputStream containing the request's data.
   * 
   * @return Decorated stream containing the data along with any necessary authorization
   *         information.
   */
  InputStream wrapStream(Request request, final InputStream stream);

  /**
   * @return value to use in the Content-Length header field. May be different from the actual
   *         data's length if this {@code HttpAuth} is decorating the input stream with chunk
   *         signatures.
   */
  long getContentLength(final Request request);
}
