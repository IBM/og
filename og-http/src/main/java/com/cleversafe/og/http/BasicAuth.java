/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.http;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import com.cleversafe.og.api.Request;
import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import com.google.common.net.HttpHeaders;

/**
 * An http auth implementation that creates authorization header values using the basic auth
 * algorithm
 * 
 * @since 1.0
 */
public class BasicAuth implements HttpAuth {
  public BasicAuth() {}

  @Override
  public Map<String, String> getAuthorizationHeaders(Request request) {
    final String username = checkNotNull(request.headers().get(Headers.X_OG_USERNAME));
    final String password = checkNotNull(request.headers().get(Headers.X_OG_PASSWORD));
    final String credentials = username + ":" + password;

    return Collections.singletonMap(HttpHeaders.AUTHORIZATION,
        "Basic " + BaseEncoding.base64().encode(credentials.getBytes(Charsets.UTF_8)));
  }

  @Override
  public InputStream wrapStream(Request request, InputStream stream) {
    return stream;
  }

  @Override
  public long getContentLength(final Request request) {
    return request.getBody().getSize();
  }

  @Override
  public String toString() {
    return "BasicAuth []";
  }
}
