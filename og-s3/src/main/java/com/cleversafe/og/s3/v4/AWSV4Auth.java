/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.s3.v4;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.SignableRequest;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.cleversafe.og.api.AuthenticatedRequest;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.http.AuthenticatedHttpRequest;
import com.cleversafe.og.http.HttpAuth;
import com.cleversafe.og.util.Context;

public class AWSV4Auth implements HttpAuth {
  private static Logger _logger = LoggerFactory.getLogger(AWSV4Auth.class);
  private final boolean chunkedEncoding;

  @Inject
  public AWSV4Auth(@Named("authentication.awsChunked") final boolean chunkedEncoding) {
    this.chunkedEncoding = chunkedEncoding;
  }

  @Override
  public AuthenticatedRequest authenticate(final Request request) {
    checkNotNull(request);
    final String accessKeyId = checkNotNull(request.getContext().get(Context.X_OG_USERNAME));
    final String secretAccessKey = checkNotNull(request.getContext().get(Context.X_OG_PASSWORD));
    final AWSCredentials credentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);

    final AWSS3V4Signer signer = new AWSS3V4Signer(this.chunkedEncoding);
    signer.setServiceName("s3");

    final AuthenticatedHttpRequest authenticatedRequest = new AuthenticatedHttpRequest(request);
    final SignableRequest<Request> signableRequest =
        new SignableRequestAdapter(authenticatedRequest);

    signer.sign(signableRequest, credentials);

    return authenticatedRequest;
  }

  @Override
  public String toString() {
    return String.format("AWSV4Auth [chunkedEncoding=%s]", this.chunkedEncoding);
  }
}
