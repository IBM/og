/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.openstack;

import static com.google.common.base.Preconditions.checkNotNull;

import com.cleversafe.og.api.AuthenticatedRequest;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.http.AuthenticatedHttpRequest;
import com.cleversafe.og.http.HttpAuth;
import com.cleversafe.og.util.Context;

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
