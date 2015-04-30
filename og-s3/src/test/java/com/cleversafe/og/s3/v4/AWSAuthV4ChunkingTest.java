/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.s3.v4;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.http.Bodies;
import com.cleversafe.og.http.Headers;
import com.cleversafe.og.http.HttpRequest;
import com.cleversafe.og.http.HttpUtil;
import com.cleversafe.og.util.io.Streams;

public class AWSAuthV4ChunkingTest {
  private static Logger _logger = LoggerFactory.getLogger(AWSAuthV4ChunkingTest.class);

  private final URI URI;

  private static final String SECRET_KEY = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";
  private static final String KEY_ID = "AKIDEXAMPLE";

  public AWSAuthV4ChunkingTest() throws URISyntaxException {
    this.URI = new URI("http://127.0.0.1:8080/container/object");
  }

  /**
   * Use the signer to get a chunked buffer without using the wrapping stream.
   */
  private byte[] getCompleteChunkedBuff(final Request request, final AWSAuthV4Chunked auth,
      final int userDataBlockSize) throws IOException {
    final InputStream requestStream = Streams.create(request.getBody());

    final Map<String, String> headers = HttpUtil.filterOutOgHeaders(request.headers());
    auth.addChunkHeaders(request, headers);

    final AWS4SignerChunked signer = auth.getSigner(request);
    // Call getAuthheaders just to initialize this signer
    signer.getAuthHeaders(headers, Collections.<String, String>emptyMap(),
        AWS4SignerChunked.STREAMING_BODY_SHA256, KEY_ID, SECRET_KEY,
        new Date(request.getMessageTime()));


    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final byte[] buffer = new byte[userDataBlockSize];
    int bytesRead = 0;
    while ((bytesRead = requestStream.read(buffer, 0, buffer.length)) != -1) {
      // process into a chunk
      final byte[] chunk = signer.constructSignedChunk(bytesRead, buffer);

      // send the chunk
      outputStream.write(chunk);
    }

    // last step is to send a signed zero-length chunk to complete the upload
    final byte[] finalChunk = signer.constructSignedChunk(0, buffer);
    outputStream.write(finalChunk);
    outputStream.close();
    return outputStream.toByteArray();
  }

  @Test
  public void wrapTest() throws IOException {
    for (int bodySize = 0; bodySize <= 5; bodySize++) {
      for (int userDataBlockSize = 1; userDataBlockSize <= bodySize; userDataBlockSize++) {
        // Build a request and auth for this body size and block size
        final AWSAuthV4Chunked auth = new AWSAuthV4Chunked("dsnet", "s3", userDataBlockSize);
        final HttpRequest.Builder reqBuilder = new HttpRequest.Builder(Method.PUT, this.URI);
        reqBuilder.withHeader(Headers.X_OG_USERNAME, KEY_ID);
        reqBuilder.withHeader(Headers.X_OG_PASSWORD, SECRET_KEY);
        reqBuilder.withBody(Bodies.zeroes(bodySize));
        final Request request = reqBuilder.build();

        // Get the expected chunked buff without using a wrapped stream
        final byte[] expectedBuff = getCompleteChunkedBuff(request, auth, userDataBlockSize);

        {
          // Test the wrapping stream reading 1 byte at a time
          final InputStream wrappedStream =
              auth.wrapStream(request, Streams.create(request.getBody()));

          final ByteArrayOutputStream actualOutput = new ByteArrayOutputStream();
          int read = wrappedStream.read();
          while (read != -1) {
            actualOutput.write(read);
            read = wrappedStream.read();
          }
          _logger.info("expected = \n{}", new String(expectedBuff));
          _logger.info("actual = \n{}", actualOutput);
          Assert.assertTrue("1 byte reads failed with bodySize = " + bodySize
              + ", userDataBlockSize = " + userDataBlockSize,
              Arrays.equals(expectedBuff, actualOutput.toByteArray()));
        }

        // Test the stream reading 1 - N bytes at a time
        for (int readAmount = 1; readAmount <= expectedBuff.length; readAmount++) {
          final InputStream wrappedStream =
              auth.wrapStream(request, Streams.create(request.getBody()));

          final ByteArrayOutputStream actualOutput = new ByteArrayOutputStream();
          final byte[] buff = new byte[readAmount];
          int read = wrappedStream.read(buff, 0, readAmount);
          while (read != -1) {
            actualOutput.write(buff, 0, read);
            read = wrappedStream.read(buff, 0, readAmount);
          }
          actualOutput.close();
          Assert.assertTrue(readAmount + " byte reads failed with bodySize = " + bodySize
              + ", userDataBlockSize = " + userDataBlockSize,
              Arrays.equals(expectedBuff, actualOutput.toByteArray()));
        }
      }
    }
  }
}
