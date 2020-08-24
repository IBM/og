/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.s3.v2;

import static com.google.common.base.Preconditions.checkNotNull;

import com.ibm.og.api.AuthType;
import com.ibm.og.http.AuthenticatedHttpRequest;
import com.ibm.og.http.HttpAuth;
import com.ibm.og.s3.SignableRequestAdapter;
import com.ibm.og.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.SignableRequest;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.internal.S3Signer;
import com.ibm.og.api.AuthenticatedRequest;
import com.ibm.og.api.Request;

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
        new SignableRequestAdapter(authenticatedRequest, AuthType.AWSV2);

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
