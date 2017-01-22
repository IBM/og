/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.http;

import static com.google.common.base.Preconditions.checkNotNull;

import com.ibm.og.api.AuthenticatedRequest;
import com.ibm.og.api.Request;
import com.ibm.og.util.Context;
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
