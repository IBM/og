/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.s3.v4;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import com.cleversafe.og.api.Request;
import com.cleversafe.og.http.Headers;

public class AWSAuthV4AuthHeader extends AWSAuthV4Base {


  public AWSAuthV4AuthHeader(String regionName, String serviceName, Long forcedDate) {
    super(regionName, serviceName, forcedDate);
  }

  @Override
  public Map<String, String> getAuthorizationHeaders(Request request) {
    final String keyId = checkNotNull(request.headers().get(Headers.X_OG_USERNAME));
    final String secretKey = checkNotNull(request.headers().get(Headers.X_OG_PASSWORD));

    final URL url;
    try {
      final String host = AWS4SignerBase.findHostHeader(request.headers());
      url = new URL("http", host, request.getUri().toString());
    } catch (MalformedURLException e) {
      throw new InvalidParameterException("Can't convert to request.URI(" + request.getUri()
          + ") to  URL:" + e.getMessage());
    }

    final AWS4SignerForAuthorizationHeader signer =
        new AWS4SignerForAuthorizationHeader(url, request.getMethod().toString(), serviceName,
            regionName);

    final Date date = forcedDate == null ? new Date() : new Date(forcedDate);
    return signer.getAuthHeaders(request.headers(), Collections.<String, String>emptyMap(),
        AWS4SignerBase.EMPTY_BODY_SHA256, keyId, secretKey, date);
  }

  @Override
  public InputStream wrapStream(InputStream stream) {
    return stream;
  }

  @Override
  public long getContentLength(Request request) {
    return request.getBody().getSize();
  }
}
