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
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.http.Headers;
import com.cleversafe.og.http.HttpAuth;
import com.cleversafe.og.util.io.Streams;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class AWSAuthV4 extends AWSAuthV4Base implements HttpAuth {
  private static Logger _logger = LoggerFactory.getLogger(AWSAuthV4.class);

  private final LoadingCache<Body, String> hashCache;

  public AWSAuthV4(final String regionName, final String serviceName, final int cacheSize) {
    super(regionName, serviceName);
    if (cacheSize > 0) {
      _logger.debug("Aws v4 auth cache configured with size {}", cacheSize);
      this.hashCache =
          CacheBuilder.newBuilder().maximumSize(cacheSize).build(new CacheLoader<Body, String>() {
            @Override
            public String load(final Body key) throws Exception {
              return calculateFullBodyHash(key);
            }
          });
    } else {
      _logger.debug("Aws v4 auth cache disabled");
      this.hashCache = null;
    }
  }

  @Override
  public Map<String, String> getAuthorizationHeaders(final Request request) {
    final String keyId = checkNotNull(request.headers().get(Headers.X_OG_USERNAME));
    final String secretKey = checkNotNull(request.headers().get(Headers.X_OG_PASSWORD));

    try {
      final AWS4SignerBase signer =
          new AWS4SignerBase(request.getUri().toURL(), request.getMethod().toString(),
              this.serviceName, this.regionName);

      return signer.getAuthHeaders(request.headers(), Collections.<String, String>emptyMap(),
          getBodyHash(request.getBody()), keyId, secretKey, new Date(request.getMessageTime()));

    } catch (final MalformedURLException e) {
      throw new InvalidParameterException("Can't convert to request.URI(" + request.getUri()
          + ") to  URL:" + e.getMessage());
    }
  }

  private String getBodyHash(final Body body) {
    if (this.hashCache == null) {
      return calculateFullBodyHash(body);
    } else {
      try {
        return this.hashCache.get(body);
      } catch (final ExecutionException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private String calculateFullBodyHash(final Body body) {
    if (body.getSize() == 0) {
      return AWS4SignerBase.EMPTY_BODY_SHA256;
    } else {
      try {
        final InputStream s = Streams.create(body);

        final MessageDigest md = MessageDigest.getInstance("SHA-256");
        final byte[] buff = new byte[(int) Math.min(body.getSize(), 10000000l)];
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
