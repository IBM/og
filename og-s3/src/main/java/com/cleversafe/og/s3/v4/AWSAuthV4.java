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
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.http.Headers;
import com.cleversafe.og.http.HttpAuth;
import com.cleversafe.og.util.io.Streams;

public class AWSAuthV4 extends AWSAuthV4Base implements HttpAuth {
  public AWSAuthV4() {
    super("us-east-1", "s3");
  }

  public AWSAuthV4(final String regionName, final String serviceName) {
    super(regionName, serviceName);
  }

  @Override
  public Map<String, String> getAuthorizationHeaders(final Request request) {
    final String keyId = checkNotNull(request.headers().get(Headers.X_OG_USERNAME));
    final String secretKey = checkNotNull(request.headers().get(Headers.X_OG_PASSWORD));

    try {
      final AWS4SignerBase signer =
          new AWS4SignerBase(request.getUri().toURL(), request.getMethod().toString(), serviceName,
              regionName);

      return signer.getAuthHeaders(request.headers(), Collections.<String, String>emptyMap(),
          calculateFullBodyHash(request.getBody()), keyId, secretKey,
          new Date(request.getMessageTime()));

    } catch (final MalformedURLException e) {
      throw new InvalidParameterException("Can't convert to request.URI(" + request.getUri()
          + ") to  URL:" + e.getMessage());
    }
  }

  private String calculateFullBodyHash(final Body body) {
    // TODO store a cash for body hashes here if performance is not adequate.
    if (body.getSize() == 0) {
      return AWS4SignerBase.EMPTY_BODY_SHA256;
    } else {
      try {
        final InputStream s = Streams.create(body);

        final MessageDigest md = MessageDigest.getInstance("SHA-256");
        final byte[] buff = new byte[8192];
        int read = s.read(buff);
        while (read != -1) {
          md.update(buff, 0, read);
          read = s.read(buff);
        }
        return BinaryUtils.toHex(md.digest());
      } catch (final Exception e) {
        throw new RuntimeException("Unable to compute hash while signing request: "
            + e.getMessage(), e);
      }
    }
  }

  @Override
  public InputStream wrapStream(final Request request, final InputStream stream) {
    return stream;
  }

  @Override
  public long getContentLength(final Request request) {
    return request.getBody().getSize();
  }
}
