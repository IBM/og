/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.cleversafe.og.s3.v4;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.SignableRequest;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.cleversafe.og.api.AuthenticatedRequest;
import com.cleversafe.og.api.DataType;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.http.AuthenticatedHttpRequest;
import com.cleversafe.og.http.Bodies;
import com.cleversafe.og.http.HttpAuth;
import com.cleversafe.og.util.Context;
import com.cleversafe.og.util.io.Streams;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;

/**
 * An http auth implementation which authenticates using the aws v4 algorithm
 * 
 * @since 1.0
 */
public class AWSV4Auth implements HttpAuth {
  private static Logger _logger = LoggerFactory.getLogger(AWSV4Auth.class);
  private final boolean chunkedEncoding;
  private final int cacheSize;
  private final DataType data;
  private final LoadingCache<Long, byte[]> digestCache;

  @Inject
  public AWSV4Auth(@Named("authentication.awsChunked") final boolean chunkedEncoding,
      @Named("authentication.awsCacheSize") final int cacheSize, final DataType data) {
    this.chunkedEncoding = chunkedEncoding;
    checkArgument(cacheSize >= 0, "cacheSize must be >= 0 [%s]", cacheSize);
    this.cacheSize = cacheSize;
    this.data = checkNotNull(data);
    checkArgument(data != DataType.NONE, "data must not be NONE");

    if (cacheSize > 0) {
      checkArgument(data == DataType.ZEROES, "If cacheSize > 0, data must be ZEROES [%s]", data);
      this.digestCache = CacheBuilder.newBuilder().maximumSize(cacheSize).build(new DigestLoader());
    } else {
      this.digestCache = null;
    }
  }

  static class DigestLoader extends CacheLoader<Long, byte[]> {
    @Override
    public byte[] load(final Long key) throws Exception {
      checkNotNull(key);
      _logger.debug("Loading digest for size [{}]", key);

      final HashingInputStream hashStream =
          new HashingInputStream(Hashing.sha256(), Streams.create(Bodies.zeroes(key)));
      final byte[] buffer = new byte[4096];
      while (hashStream.read(buffer) != -1) {
      }
      // should never throw an exception since the source is from Streams.create
      hashStream.close();

      return hashStream.hash().asBytes();
    }
  }

  @Override
  public AuthenticatedRequest authenticate(final Request request) {
    checkNotNull(request);
    final String accessKeyId = checkNotNull(request.getContext().get(Context.X_OG_USERNAME));
    final String secretAccessKey = checkNotNull(request.getContext().get(Context.X_OG_PASSWORD));
    final AWSCredentials credentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);

    final AWSS3V4Signer signer = new AWSS3V4Signer(this.chunkedEncoding, this.digestCache);
    signer.setServiceName("s3");

    final AuthenticatedHttpRequest authenticatedRequest = new AuthenticatedHttpRequest(request);
    final SignableRequest<Request> signableRequest =
        new SignableRequestAdapter(authenticatedRequest);

    signer.sign(signableRequest, credentials);

    return authenticatedRequest;
  }

  @Override
  public String toString() {
    return String.format("AWSV4Auth [chunkedEncoding=%s, cacheSize=%s, data=%s]",
        this.chunkedEncoding, this.cacheSize, this.data);
  }
}
