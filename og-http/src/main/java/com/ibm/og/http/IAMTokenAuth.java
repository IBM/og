/* Copyright (c) IBM Corporation 2017. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.http;

import static com.google.common.base.Preconditions.checkNotNull;

import com.ibm.og.api.AuthenticatedRequest;
import com.ibm.og.api.Request;
import com.ibm.og.util.Context;
import com.google.common.net.HttpHeaders;

/**
 * An http auth implementation which applies header values using IBM BlueMix Identity and Access Management
 * 
 * @since 1.4.0
 */
public class IAMTokenAuth implements HttpAuth {
  public IAMTokenAuth() {}

  @Override
  public AuthenticatedRequest authenticate(final Request request) {
    final String IAMToken = checkNotNull(request.getContext().get(Context.X_OG_IAM_TOKEN));

    final AuthenticatedHttpRequest authenticatedRequest = new AuthenticatedHttpRequest(request);
    authenticatedRequest.addHeader(HttpHeaders.AUTHORIZATION, IAMToken);

    return authenticatedRequest;
  }

  @Override
  public String toString() {
    return "IAMTokenAuth []";
  }
}
