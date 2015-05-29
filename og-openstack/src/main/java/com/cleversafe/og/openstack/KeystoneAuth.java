/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.openstack;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.InputStream;
import java.util.Map;

import com.cleversafe.og.api.Request;
import com.cleversafe.og.http.Headers;
import com.cleversafe.og.http.HttpAuth;
import com.google.common.collect.ImmutableMap;

/**
 * An http auth implementation that creates authorization header values using the keystone auth
 * algorithm
 * 
 * @since 1.0
 */
// FIXME account for requests that do not have a token
public class KeystoneAuth implements HttpAuth {
  @Override
  public Map<String, String> getAuthorizationHeaders(final Request request) {
    final String keystoneToken = checkNotNull(request.headers().get(Headers.X_OG_KEYSTONE_TOKEN));

    return ImmutableMap.of("X-Auth-Token", keystoneToken);
  }

  @Override
  public InputStream wrapStream(final Request request, final InputStream stream) {
    return stream;
  }

  @Override
  public long getContentLength(final Request request) {
    return request.getBody().getSize();
  }

  @Override
  public String toString() {
    return "KeystoneAuth []";
  }
}
