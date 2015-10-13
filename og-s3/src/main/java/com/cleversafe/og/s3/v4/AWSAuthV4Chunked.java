/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.s3.v4;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.api.DataType;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.http.Headers;
import com.cleversafe.og.http.HttpUtil;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class AWSAuthV4Chunked extends AWSAuthV4Base {
  private static Logger _logger = LoggerFactory.getLogger(AWSAuthV4Chunked.class);

  /**
   * The amount of user data in each chunk.
   */
  private final int userDataBlockSize;

  /**
   * Cache to store hashes of all-zeroes strings. Implementing this with the assumption that only
   * all-zeroes will be used greatly simplifies the implementation. If random data is being used
   * then the cache will be useless with pretty much any implementation anyway.
   */
  private final LoadingCache<Integer, String> zeroesHashCache;

  /**
   * The default amount of user data in each chunk.
   */
  public static final int DEFAULT_CHUNK_SIZE = 65536;

  public AWSAuthV4Chunked(final String regionName, final String serviceName, final int chunkSize,
      final int cacheSize) {
    super(regionName, serviceName);
    this.userDataBlockSize = chunkSize;

    if (cacheSize > 0) {
      _logger.debug("Aws v4 auth cache configured with size {}", cacheSize);
      this.zeroesHashCache = CacheBuilder.newBuilder().maximumSize(cacheSize)
          .build(new CacheLoader<Integer, String>() {
            @Override
            public String load(final Integer key) throws Exception {
              return BinaryUtils.toHex(AWS4SignerBase.hash(new byte[key]));
            }
          });
    } else {
      _logger.debug("Aws v4 auth cache disabled");
      this.zeroesHashCache = null;
    }
  }

  AWS4SignerChunked getSigner(final Request request) {
    try {
      return new AWS4SignerChunked(request.getUri().toURL(), request.getMethod().toString(),
          this.serviceName, this.regionName, this.zeroesHashCache);
    } catch (final MalformedURLException e) {
      throw new InvalidParameterException(
          "Can't convert to request.URI(" + request.getUri() + ") to  URL:" + e.getMessage());
    }
  }

  @Override
  public Map<String, String> getAuthorizationHeaders(final Request request) {
    final String keyId = checkNotNull(request.headers().get(Headers.X_OG_USERNAME));
    final String secretKey = checkNotNull(request.headers().get(Headers.X_OG_PASSWORD));

    final Map<String, String> signableHeaders = HttpUtil.filterOutOgHeaders(request.headers());
    addChunkHeaders(request, signableHeaders);

    return getSigner(request).getAuthHeaders(signableHeaders,
        Collections.<String, String>emptyMap(), AWS4SignerChunked.STREAMING_BODY_SHA256, keyId,
        secretKey, new Date(request.getMessageTime()));
  }

  /**
   * Add aws-chunked specific headers
   */
  void addChunkHeaders(final Request request, final Map<String, String> headers) {
    headers.put("x-amz-content-sha256", AWS4SignerChunked.STREAMING_BODY_SHA256);
    headers.put("content-encoding", "" + "aws-chunked");
    headers.put("x-amz-decoded-content-length", "" + request.getBody().getSize());
  }

  @Override
  public InputStream wrapStream(final Request request, final InputStream stream) {
    // FIXME - Think of a way to store the state including signing key and previous sig to avoid
    // recalculating it here.

    final String keyId = checkNotNull(request.headers().get(Headers.X_OG_USERNAME));
    final String secretKey = checkNotNull(request.headers().get(Headers.X_OG_PASSWORD));

    final Map<String, String> signableHeaders = HttpUtil.filterOutOgHeaders(request.headers());
    addChunkHeaders(request, signableHeaders);

    final AWS4SignerChunked signer = getSigner(request);
    signer.getAuthHeaders(signableHeaders, Collections.<String, String>emptyMap(),
        AWS4SignerChunked.STREAMING_BODY_SHA256, keyId, secretKey,
        new Date(request.getMessageTime()));

    return new InputStream() {

      /**
       * Temporary buffer to hold the current chunk which includes the metadata and signature.
       */
      byte[] chunk;

      /**
       * Temporary buffer to hold data read from the backing stream. We read userDataChunkSize at a
       * time.
       */
      byte[] userData = new byte[AWSAuthV4Chunked.this.getUserDataBlockSize()];

      /**
       * Set to true when we've read all user data and already created the final chunk. If this is
       * true, you should never call getNextChunk() again.
       */
      boolean eOfUserData;

      /**
       * Set to true when this wrapping stream has sent all bytes of the final chunk.
       */
      boolean eof;

      /**
       * Position within the current chunk buffer.
       */
      int chunkPos = 0;

      /**
       * Reads the next userDataChunkSize of data from the backing stream, and creates a new chunk
       * buffer with that data along with required chunk metadata.
       * 
       * @throws IOException if you call this after the final chunk has already been created.
       */
      private void getNextChunk() throws IOException {
        if (this.eOfUserData) {
          throw new EOFException();
        }

        final int userDataRead =
            stream.read(this.userData, 0, AWSAuthV4Chunked.this.getUserDataBlockSize());
        if (userDataRead == -1) {
          // Read no more data, but we need to call constructSignedChunk to get the final chunk
          this.eOfUserData = true;
        }
        this.chunk = signer.constructSignedChunk(userDataRead, this.userData,
            request.getBody().getDataType().equals(DataType.ZEROES));
        this.chunkPos = 0;
      }

      @Override
      public int read() throws IOException {
        final byte[] b = new byte[1];
        final int read = read(b, 0, 1);
        return read == -1 ? -1 : b[0];
      }

      @Override
      public int read(final byte b[], final int off, final int len) throws IOException {
        if (b == null) {
          throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
          throw new IndexOutOfBoundsException();
        } else if (len == 0) {
          return 0;
        }

        if (this.eof) {
          return -1;
        }

        int lenCopied = 0;
        while (lenCopied < len) {

          // Read the first/next chunk if necessary
          if (this.chunk == null || this.chunkPos == this.chunk.length) {
            if (this.eOfUserData) {
              // We've already generated all chunks and the chunkPos indicates that we've sent out
              // all the data.
              this.eof = true;
              return lenCopied > 0 ? lenCopied : -1;
            } else {
              getNextChunk();
            }
          }

          final int copyLen;
          final int copyLenLeft = len - lenCopied;
          if (copyLenLeft <= this.chunk.length - this.chunkPos) {
            copyLen = copyLenLeft;
          } else {
            copyLen = this.chunk.length - this.chunkPos;
          }

          System.arraycopy(this.chunk, this.chunkPos, b, off + lenCopied, copyLen);
          this.chunkPos += copyLen;
          assert this.chunkPos <= this.chunk.length;
          lenCopied += copyLen;
        }

        assert lenCopied == len;
        return lenCopied;
      }
    };
  }

  @Override
  public long getContentLength(final Request request) {
    return AWS4SignerChunked.calculateChunkedContentLength(request.getBody().getSize(),
        this.getUserDataBlockSize());
  }

  public int getUserDataBlockSize() {
    return this.userDataBlockSize;
  }
}
