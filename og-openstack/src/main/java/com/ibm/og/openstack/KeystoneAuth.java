/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.openstack;

import static com.google.common.base.Preconditions.checkNotNull;

import com.ibm.og.api.AuthenticatedRequest;
import com.ibm.og.api.Request;
import com.ibm.og.http.AuthenticatedHttpRequest;
import com.ibm.og.http.HttpAuth;
import com.ibm.og.util.Context;

/**
 * An http auth implementation which applies header values using the keystone token algorithm
 * 
 * @since 1.0
 */
public class KeystoneAuth implements HttpAuth {
  @Override
  public AuthenticatedRequest authenticate(final Request request) {
    final String keystoneToken =
        checkNotNull(request.getContext().get(Context.X_OG_KEYSTONE_TOKEN));

    final AuthenticatedHttpRequest authenticatedRequest = new AuthenticatedHttpRequest(request);
    authenticatedRequest.addHeader("X-Auth-Token", keystoneToken);

    return authenticatedRequest;
  }

  @Override
  public String toString() {
    return "KeystoneAuth []";
  }
}
