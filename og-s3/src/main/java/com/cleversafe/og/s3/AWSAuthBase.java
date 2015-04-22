/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.s3;

import static com.google.common.base.Preconditions.checkNotNull;

import com.cleversafe.og.api.Request;
import com.cleversafe.og.http.Headers;
import com.cleversafe.og.http.HttpAuth;

public abstract class AWSAuthBase implements HttpAuth {

  @Override
  public String nextAuthorizationHeader(Request request) {
    final String awsAccessKeyId = checkNotNull(request.headers().get(Headers.X_OG_USERNAME));
    final String awsSecretAccessKey = checkNotNull(request.headers().get(Headers.X_OG_PASSWORD));
    return authenticate(request, awsAccessKeyId, awsSecretAccessKey);
  }

  protected abstract String authenticate(final Request request, final String awsAccessKeyId,
      final String awsSecretAccessKey);

}
