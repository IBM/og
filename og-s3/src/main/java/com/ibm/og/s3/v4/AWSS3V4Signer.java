/*
 * Copyright 2013-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except
 * in compliance with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.ibm.og.s3.v4;

import static com.amazonaws.auth.internal.SignerConstants.X_AMZ_CONTENT_SHA256;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ReadLimitInfo;
import com.amazonaws.Request;
import com.amazonaws.ResetException;
import com.amazonaws.SignableRequest;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.internal.AWS4SignerRequestParams;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.request.S3HandlerContextKeys;
import com.amazonaws.util.BinaryUtils;
import com.google.common.cache.LoadingCache;

/**
 * AWS4 signer implementation for AWS S3
 */
public class AWSS3V4Signer extends AWS4Signer {
  private static final String CONTENT_SHA_256 = "STREAMING-AWS4-HMAC-SHA256-PAYLOAD";

  private final boolean chunkedEncoding;
  private final LoadingCache<Long, byte[]> digestCache;

  /**
   * Constructs the signer; configures whether to use aws chunking or not
   * 
   * @param chunkedEncoding whether to use standard or chunked signing
   * @param digestCache optional digest cache for like-sized objects
   */
  public AWSS3V4Signer(final boolean chunkedEncoding,
      @Nullable final LoadingCache<Long, byte[]> digestCache) {
    super(false);
    this.chunkedEncoding = chunkedEncoding;
    this.digestCache = digestCache;
  }

  /**
   * If necessary, creates a chunk-encoding wrapper on the request payload.
   */
  @Override
  protected void processRequestPayload(final SignableRequest<?> request, final byte[] signature,
      final byte[] signingKey, final AWS4SignerRequestParams signerRequestParams) {
    if (useChunkEncoding(request)) {
      final AwsChunkedEncodingInputStream chunkEncodededStream = new AwsChunkedEncodingInputStream(
          request.getContent(), signingKey, signerRequestParams.getFormattedSigningDateTime(),
          signerRequestParams.getScope(), BinaryUtils.toHex(signature), this, this.digestCache);
      request.setContent(chunkEncodededStream);
    }
  }

  @Override
  protected String calculateContentHashPresign(final SignableRequest<?> request) {
    return "UNSIGNED-PAYLOAD";
  }

  /**
   * Returns the pre-defined header value and set other necessary headers if the request needs to be
   * chunk-encoded. Otherwise calls the superclass method which calculates the hash of the whole
   * content for signing.
   */
  @Override
  protected String calculateContentHash(final SignableRequest<?> request) {
    // To be consistent with other service clients using sig-v4,
    // we just set the header as "required", and AWS4Signer.sign() will be
    // notified to pick up the header value returned by this method.
    request.addHeader(X_AMZ_CONTENT_SHA256, "required");
    final String contentLength = request.getHeaders().get(Headers.CONTENT_LENGTH);
    if (useChunkEncoding(request)) {
      final long originalContentLength;
      if (contentLength != null) {
        originalContentLength = Long.parseLong(contentLength);
      } else {
        /**
         * "Content-Length" header could be missing if the caller is uploading a stream without
         * setting Content-Length in ObjectMetadata. Before using sigv4, we rely on HttpClient to
         * add this header by using BufferedHttpEntity when creating the HttpRequest object. But
         * now, we need this information immediately for the signing process, so we have to cache
         * the stream here.
         */
        try {
          originalContentLength = getContentLength(request);
        } catch (final IOException e) {
          throw new AmazonClientException("Cannot get the content-length of the request content.",
              e);
        }
      }
      request.addHeader("x-amz-decoded-content-length", Long.toString(originalContentLength));
      // Make sure "Content-Length" header is not empty so that HttpClient
      // won't cache the stream again to recover Content-Length
      request.addHeader(Headers.CONTENT_LENGTH, Long.toString(
          AwsChunkedEncodingInputStream.calculateStreamContentLength(originalContentLength)));
      return CONTENT_SHA_256;
    }

    if (this.digestCache != null) {
      try {
        final long length = contentLength != null ? Long.parseLong(contentLength) : 0;
        return BinaryUtils.toHex(this.digestCache.get(length));
      } catch (final ExecutionException e) {
        throw new RuntimeException(e);
      }
    }
    return super.calculateContentHash(request);
  }

  /**
   * Determine whether to use aws-chunked for signing
   */
  private boolean useChunkEncoding(final SignableRequest<?> request) {
    // If chunked encoding is explicitly disabled through client options
    // return right here.
    if (isChunkedEncodingDisabled(request)) {
      return false;
    }
    // FIXME this may break with POST or part upload
    return this.chunkedEncoding && request.getHttpMethod() == HttpMethodName.PUT;
  }

  /**
   * @return True if chunked encoding has been explicitly disabled per the request. False otherwise.
   */
  private boolean isChunkedEncodingDisabled(final SignableRequest<?> signableRequest) {
    if (signableRequest instanceof Request) {
      final Request<?> request = (Request<?>) signableRequest;
      final Boolean isChunkedEncodingDisabled =
          request.getHandlerContext(S3HandlerContextKeys.IS_CHUNKED_ENCODING_DISABLED);
      return isChunkedEncodingDisabled != null && isChunkedEncodingDisabled;
    }
    return false;
  }

  /**
   * Read the content of the request to get the length of the stream. This method will wrap the
   * stream by SdkBufferedInputStream if it is not mark-supported.
   */
  static long getContentLength(final SignableRequest<?> request) throws IOException {
    final InputStream content = request.getContent();
    if (!content.markSupported()) {
      throw new IllegalStateException(
          "Bug: request input stream must have been made mark-and-resettable at this point");
    }
    final ReadLimitInfo info = request.getReadLimitInfo();
    final int readLimit = info.getReadLimit();
    long contentLength = 0;
    final byte[] tmp = new byte[4096];
    int read;
    content.mark(readLimit);
    while ((read = content.read(tmp)) != -1) {
      contentLength += read;
    }
    try {
      content.reset();
    } catch (final IOException ex) {
      throw new ResetException("Failed to reset the input stream", ex);
    }
    return contentLength;
  }

  protected void addHostHeader(SignableRequest<?> request) {
    // override to avoid the aws sdk adding host adder if request
    // contains a host header explicitly
    if (!request.getHeaders().containsKey("Host")) {
      super.addHostHeader(request);
    }
  }
}
