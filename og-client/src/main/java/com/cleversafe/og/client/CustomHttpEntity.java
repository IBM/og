/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.client;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.entity.AbstractHttpEntity;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.util.io.Streams;
import com.google.common.io.ByteStreams;

/**
 * an http entity which derives its source inputstream
 * 
 * @since 1.0
 */
public class CustomHttpEntity extends AbstractHttpEntity {
  private final Body body;
  private final long writeThroughput;
  private long requestContentStart;
  private long requestContentFinish;

  public CustomHttpEntity(final Body body, final long writeThroughput) {
    this.body = checkNotNull(body);
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
    return this.body.getSize();
  }

  @Override
  public InputStream getContent() throws IOException, IllegalStateException {
    this.requestContentStart = 0;
    this.requestContentFinish = 0;
    return Streams.create(this.body);
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
    return String.format("CustomHttpEntity [body=%s, writeThroughput=%s]", this.body,
        this.writeThroughput);
  }
}
