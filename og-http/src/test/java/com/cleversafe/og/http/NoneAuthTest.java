/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.cleversafe.og.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

import com.cleversafe.og.api.AuthenticatedRequest;
import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Operation;
import com.cleversafe.og.api.Request;

public class NoneAuthTest {
  private final HttpAuth noneAuth;
  private final URI uri;
  private final Request request;
  private final AuthenticatedRequest authenticatedRequest;

  public NoneAuthTest() throws URISyntaxException {
    this.noneAuth = new NoneAuth();
    this.uri = new URI("http://127.0.0.1/openstack/container/object");
    this.request = new HttpRequest.Builder(Method.PUT, this.uri, Operation.WRITE)
        .withBody(Bodies.random(1024)).build();
    this.authenticatedRequest = this.noneAuth.authenticate(this.request);
  }

  @Test
  public void getContent() {
    assertThat(this.authenticatedRequest.getContent(), is(not((InputStream) null)));
  }

  @Test
  public void getContentLength() {
    assertThat(this.authenticatedRequest.getContentLength(), is(1024L));
  }
}
