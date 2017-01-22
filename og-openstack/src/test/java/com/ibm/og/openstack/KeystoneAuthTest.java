/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.openstack;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import com.ibm.og.http.Bodies;
import com.ibm.og.http.HttpAuth;
import com.ibm.og.http.HttpRequest;
import com.ibm.og.util.Context;
import org.junit.Test;

import com.ibm.og.api.AuthenticatedRequest;
import com.ibm.og.api.Method;
import com.ibm.og.api.Operation;
import com.ibm.og.api.Request;

public class KeystoneAuthTest {
  private final HttpAuth keystoneAuth;
  private final URI uri;
  private final String token;
  private final Request request;
  private final AuthenticatedRequest authenticatedRequest;

  public KeystoneAuthTest() throws URISyntaxException {
    this.keystoneAuth = new KeystoneAuth();
    this.uri = new URI("http://127.0.0.1/openstack/container/object");
    this.token = "token";
    this.request = new HttpRequest.Builder(Method.PUT, this.uri, Operation.WRITE)
        .withContext(Context.X_OG_KEYSTONE_TOKEN, this.token).withBody(Bodies.random(1024)).build();
    this.authenticatedRequest = this.keystoneAuth.authenticate(this.request);
  }

  @Test
  public void header() {
    assertThat(this.authenticatedRequest.headers(), hasEntry("X-Auth-Token", "token"));
  }

  @Test
  public void getContent() {
    assertThat(this.authenticatedRequest.getContent(), is(not((InputStream) null)));
  }

  @Test
  public void getContentLength() {
    assertThat(this.authenticatedRequest.getContentLength(), is(1024L));
  }

  @Test(expected = RuntimeException.class)
  public void noToken() {
    final Request badRequest =
        new HttpRequest.Builder(Method.PUT, this.uri, Operation.WRITE).build();
    this.keystoneAuth.authenticate(badRequest);
  }
}
