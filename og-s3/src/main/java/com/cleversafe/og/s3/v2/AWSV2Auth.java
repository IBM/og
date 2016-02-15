/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.s3.v2;

import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.SignableRequest;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.internal.S3Signer;
import com.cleversafe.og.api.AuthenticatedRequest;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.http.AuthenticatedHttpRequest;
import com.cleversafe.og.http.HttpAuth;
import com.cleversafe.og.s3.v4.SignableRequestAdapter;
import com.cleversafe.og.util.Context;

/**
 * An http auth implementation which authenticates using the aws v2 algorithm
 * 
 * @since 1.0
 */
public class AWSV2Auth implements HttpAuth {
  private static Logger _logger = LoggerFactory.getLogger(AWSV2Auth.class);

  public AWSV2Auth() {}

  @Override
  public AuthenticatedRequest authenticate(final Request request) {
    checkNotNull(request);
    final String accessKeyId = checkNotNull(request.getContext().get(Context.X_OG_USERNAME));
    final String secretAccessKey = checkNotNull(request.getContext().get(Context.X_OG_PASSWORD));
    final AWSCredentials credentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);

    final AuthenticatedHttpRequest authenticatedRequest = new AuthenticatedHttpRequest(request);
    final SignableRequest<Request> signableRequest =
        new SignableRequestAdapter(authenticatedRequest);

    final S3Signer signer =
        new S3Signer(signableRequest.getHttpMethod().toString(), signableRequest.getResourcePath());

    signer.sign(signableRequest, credentials);

    return authenticatedRequest;
  }

  @Override
  public String toString() {
    return "AWSV2Auth []";
  }
}
