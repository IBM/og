/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.s3.v4;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.Map;

import com.cleversafe.og.api.Request;
import com.cleversafe.og.http.HttpAuth;

public class AWSAuthV4Chunked implements HttpAuth {

  private final String REGION_NAME = "us-east-1";
  private final String SERVICE_NAME = "s3";

  public AWSAuthV4Chunked() {}

  @Override
  public Map<String, String> getAuthorizationHeaders(Request request) {
    URL url;
    try {
      url = request.getUri().toURL();
    } catch (MalformedURLException e) {
      throw new InvalidParameterException("Can't convert to request.URI(" + request.getUri()
          + ") to  URL:" + e.getMessage());
    }

    final AWS4SignerBase signer =
        new AWS4SignerForChunkedUpload(url, request.getMethod().toString(), SERVICE_NAME,
            REGION_NAME);
    return Collections.emptyMap();
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
