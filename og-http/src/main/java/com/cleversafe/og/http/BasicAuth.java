/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.http;

import static com.google.common.base.Preconditions.checkNotNull;

import com.cleversafe.og.api.AuthenticatedRequest;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.util.Context;
import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import com.google.common.net.HttpHeaders;

/**
 * An http auth implementation which applies header values using the basic authentication algorithm
 * 
 * @since 1.0
 */
public class BasicAuth implements HttpAuth {
  public BasicAuth() {}

  @Override
  public AuthenticatedRequest authenticate(final Request request) {
    final String username = checkNotNull(request.getContext().get(Context.X_OG_USERNAME));
    final String password = checkNotNull(request.getContext().get(Context.X_OG_PASSWORD));
    final String credentials = username + ":" + password;

    final AuthenticatedHttpRequest authenticatedRequest = new AuthenticatedHttpRequest(request);
    authenticatedRequest.addHeader(HttpHeaders.AUTHORIZATION,
        "Basic " + BaseEncoding.base64().encode(credentials.getBytes(Charsets.UTF_8)));

    return authenticatedRequest;
  }

  @Override
  public String toString() {
    return "BasicAuth []";
  }
}
