/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.client;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.ibm.og.util.io.Streams;
import org.apache.http.entity.AbstractHttpEntity;

import com.ibm.og.api.AuthenticatedRequest;
import com.google.common.io.ByteStreams;

/**
 * an http entity which derives its source inputstream
 * 
 * @since 1.0
 */
public class CustomHttpEntity extends AbstractHttpEntity {
  private final AuthenticatedRequest request;
  private final long writeThroughput;
  private long requestContentStart;
  private long requestContentFinish;

  public CustomHttpEntity(final AuthenticatedRequest request, final long writeThroughput) {
    this.request = checkNotNull(request);
    checkArgument(this.writeThroughput >= 0, "writeThroughput must be >= 0 [%s]",
        this.writeThroughput);
    this.writeThroughput = writeThroughput;
  }

  @Override
  public boolean isRepeatable() {
    return true;
  }

  @Override
  public long getContentLength() {
    return this.request.getContentLength();
  }

  @Override
  // FIXME getContent is supposed to return a new instance of InputStream if isRepeatable is true -
  // is it safe to simply reset the stream?
  public InputStream getContent() throws IOException, IllegalStateException {
    this.requestContentStart = 0;
    this.requestContentFinish = 0;
    final InputStream content = this.request.getContent();
    content.reset();
    return content;
  }

  @Override
  public void writeTo(final OutputStream outstream) throws IOException {
    final InputStream in = getContent();
    OutputStream out = outstream;

    if (this.writeThroughput > 0) {
      out = Streams.throttle(outstream, this.writeThroughput);
    }

    this.requestContentStart = System.nanoTime();
    ByteStreams.copy(in, out);
    this.requestContentFinish = System.nanoTime();
    in.close();
  }

  @Override
  public boolean isStreaming() {
    return false;
  }

  public long getRequestContentStart() {
    return this.requestContentStart;
  }

  public long getRequestContentFinish() {
    return this.requestContentFinish;
  }

  @Override
  public String toString() {
    return String.format("CustomHttpEntity [body=%s, writeThroughput=%s]", this.request.getBody(),
        this.writeThroughput);
  }
}
