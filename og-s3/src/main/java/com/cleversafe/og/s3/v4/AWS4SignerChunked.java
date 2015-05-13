package com.cleversafe.og.s3.v4;

import java.net.URL;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Sample AWS4 signer demonstrating how to sign 'chunked' uploads
 */
public class AWS4SignerChunked extends AWS4SignerBase {
  private static Logger _logger = LoggerFactory.getLogger(AWS4SignerChunked.class);

  /**
   * Cache to store hashes of all-zeroes strings. Implementing this with the assumption that only
   * all-zeroes will be used greatly simplifies the implementation. If random data is being used
   * then the cache will be useless with pretty much any implementation anyway.
   */
  private final LoadingCache<Integer, String> zeroesHashCache;

  /**
   * SHA256 substitute marker used in place of x-amz-content-sha256 when employing chunked uploads
   */
  public static final String STREAMING_BODY_SHA256 = "STREAMING-AWS4-HMAC-SHA256-PAYLOAD";

  private static final String CLRF = "\r\n";
  private static final String CHUNK_STRING_TO_SIGN_PREFIX = "AWS4-HMAC-SHA256-PAYLOAD";
  private static final String CHUNK_SIGNATURE_HEADER = ";chunk-signature=";
  private static final int SIGNATURE_LENGTH = 64;
  private static final byte[] FINAL_CHUNK = new byte[0];

  public AWS4SignerChunked(final URL endpointUrl, final String httpMethod,
      final String serviceName, final String regionName, final int cacheSize) {
    super(endpointUrl, httpMethod, serviceName, regionName);
    if (cacheSize > 0) {
      _logger.debug("Aws v4 auth cache configured with size {}", cacheSize);
      this.zeroesHashCache =
          CacheBuilder.newBuilder().maximumSize(cacheSize)
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

  /**
   * Calculates the expanded payload size of our data when it is chunked
   * 
   * @param originalLength The true size of the data payload to be uploaded
   * @param chunkSize The size of each chunk we intend to send; each chunk will be prefixed with
   *        signed header data, expanding the overall size by a determinable amount
   * @return The overall payload size to use as content-length on a chunked upload
   */
  public static long calculateChunkedContentLength(final long originalLength, final int chunkSize) {
    if (originalLength <= 0) {
      throw new IllegalArgumentException("Nonnegative content length expected.");
    }

    final long maxSizeChunks = originalLength / chunkSize;
    final int remainingBytes = (int) (originalLength % chunkSize);
    return maxSizeChunks * calculateChunkHeaderLength(chunkSize)
        + (remainingBytes > 0 ? calculateChunkHeaderLength(remainingBytes) : 0)
        + calculateChunkHeaderLength(0);
  }

  /**
   * Returns the size of a chunk header, which only varies depending on the selected chunk size
   * 
   * @param chunkDataSize The intended size of each chunk; this is placed into the chunk header
   * @return The overall size of the header that will prefix the user data in each chunk
   */
  static int calculateChunkHeaderLength(final int chunkDataSize) {
    return Long.toHexString(chunkDataSize).length() + CHUNK_SIGNATURE_HEADER.length()
        + SIGNATURE_LENGTH + CLRF.length() + chunkDataSize + CLRF.length();
  }

  /**
   * Returns a chunk for upload consisting of the signed 'header' or chunk prefix plus the user
   * data. The signature of the chunk incorporates the signature of the previous chunk (or, if the
   * first chunk, the signature of the headers portion of the request).
   * 
   * @param userDataLen The length of the user data contained in userData. If <= 0, it is assumed
   *        that this is a request for the final, 0-length chunk.
   * @param userData Contains the user data to be sent in the upload chunk
   * @param zeroes set to true if the userData is all zeroes. If true, the zeroesHashCache will be
   *        used (if configured with non-zero cache size).
   * @return A new buffer of data for upload containing the chunk header plus user data
   */
  public byte[] constructSignedChunk(final int userDataLen, final byte[] userData,
      final boolean zeroes) {
    // to keep our computation routine signatures simple, if the userData
    // buffer contains less data than it could, shrink it. Note the special case
    // to handle the requirement that we send an empty chunk to complete
    // our chunked upload.
    final byte[] dataToChunk;
    if (userDataLen <= 0) {
      dataToChunk = FINAL_CHUNK;
    } else {
      if (userDataLen < userData.length) {
        // shrink the chunkdata to fit
        dataToChunk = new byte[userDataLen];
        System.arraycopy(userData, 0, dataToChunk, 0, userDataLen);
      } else {
        dataToChunk = userData;
      }
    }

    final StringBuilder chunkHeader = new StringBuilder();

    // start with size of user data
    chunkHeader.append(Integer.toHexString(dataToChunk.length));

    // nonsig-extension; we have none in these samples
    final String nonsigExtension = "";

    // if this is the first chunk, we package it with the signing result
    // of the request headers, otherwise we use the cached signature
    // of the previous chunk

    final String chunkHash;
    if (userDataLen == -1 || userDataLen == 0) {
      chunkHash = AWS4SignerBase.EMPTY_BODY_SHA256;
    } else if (zeroes && this.zeroesHashCache != null) {
      try {
        chunkHash = this.zeroesHashCache.get(userDataLen);
      } catch (final ExecutionException e) {
        throw new RuntimeException(e);
      }
    } else {
      chunkHash = BinaryUtils.toHex(AWS4SignerBase.hash(dataToChunk));
    }
    final String chunkStringToSign =
        CHUNK_STRING_TO_SIGN_PREFIX + "\n" + this.dateTimeStamp + "\n" + this.scope + "\n"
            + this.lastComputedSignature + "\n"
            // nonsig-extension hash (not using any nonsig-extensions, so it's a blank str)
            + EMPTY_BODY_SHA256 + "\n" + chunkHash;

    // compute the V4 signature for the chunk
    final String chunkSignature =
        BinaryUtils.toHex(AWS4SignerBase.sign(chunkStringToSign, this.signingKey, "HmacSHA256"));

    // cache the signature to include with the next chunk's signature computation
    this.lastComputedSignature = chunkSignature;

    // construct the actual chunk, comprised of the non-signed extensions, the
    // 'headers' we just signed and their signature, plus a newline then copy
    // that plus the user's data to a payload to be written to the request stream
    chunkHeader.append(nonsigExtension + CHUNK_SIGNATURE_HEADER + chunkSignature);
    chunkHeader.append(CLRF);

    try {
      final byte[] header = chunkHeader.toString().getBytes("UTF-8");
      final byte[] trailer = CLRF.getBytes("UTF-8");
      final byte[] signedChunk = new byte[header.length + dataToChunk.length + trailer.length];
      System.arraycopy(header, 0, signedChunk, 0, header.length);
      System.arraycopy(dataToChunk, 0, signedChunk, header.length, dataToChunk.length);
      System.arraycopy(trailer, 0, signedChunk, header.length + dataToChunk.length, trailer.length);

      // this is the total data for the chunk that will be sent to the request stream
      return signedChunk;
    } catch (final Exception e) {
      throw new RuntimeException("Unable to sign the chunked data. " + e.getMessage(), e);
    }
  }
}
