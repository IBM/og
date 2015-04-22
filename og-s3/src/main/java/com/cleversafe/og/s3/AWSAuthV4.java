/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.s3;

import java.io.InputStream;
import java.util.Map;

import com.cleversafe.og.api.Request;
import com.cleversafe.og.http.HttpAuth;

public class AWSAuthV4 implements HttpAuth {

  private static final String AWS_SIG_ALG = "AWS4-HMAC-SHA256";

  protected String authenticate(Request request, String awsAccessKeyId, String awsSecretAccessKey) {
    final StringBuilder authString = new StringBuilder(AWS_SIG_ALG);
    authString.append(" Credential=").append(scope(request, awsSecretAccessKey));
    authString.append(",SignedHeaders=").append(signedHeaders());
    authString.append(",Signature=").append(signature(request, awsAccessKeyId, awsSecretAccessKey));
    return authString.toString();
  }

  private Object signature(Request request, String awsAccessKeyId, String awsSecretAccessKey) {
    // TODO Auto-generated method stub
    return null;
  }

  private Object signedHeaders() {
    // TODO Auto-generated method stub
    return null;
  }

  private Object scope(Request request, String awsSecretAccessKey) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Map<String, String> getAuthorizationHeaders(Request request) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public InputStream wrapStream(InputStream stream) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public long getContentLength(Request request) {
    // TODO Auto-generated method stub
    return 0;
  }
}
