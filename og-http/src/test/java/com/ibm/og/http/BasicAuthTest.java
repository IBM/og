/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

import com.ibm.og.api.AuthenticatedRequest;
import com.ibm.og.api.Method;
import com.ibm.og.api.Operation;
import com.ibm.og.api.Request;
import com.ibm.og.util.Context;
import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import com.google.common.net.HttpHeaders;


public class BasicAuthTest {
  private final HttpAuth basicAuth;
  private final URI uri;
  private final String username;
  private final String password;
  private final Request request;
  private final AuthenticatedRequest authenticatedRequest;

  public BasicAuthTest() throws URISyntaxException {
    this.basicAuth = new BasicAuth();
    this.uri = new URI("http://127.0.0.1/openstack/container/object");
    this.username = "robert";
    this.password = "password";
    this.request = new HttpRequest.Builder(Method.PUT, this.uri, Operation.WRITE)
        .withContext(Context.X_OG_USERNAME, this.username)
        .withContext(Context.X_OG_PASSWORD, this.password).withBody(Bodies.random(1024)).build();
    this.authenticatedRequest = this.basicAuth.authenticate(this.request);
  }

  @Test
  public void header() {
    final String credentials = this.username + ":" + this.password;
    final String authorization =
        "Basic " + BaseEncoding.base64().encode(credentials.getBytes(Charsets.UTF_8));
    assertThat(this.authenticatedRequest.headers(),
        hasEntry(HttpHeaders.AUTHORIZATION, authorization));
  }

  @Test
  public void getContent() {
    assertThat(this.authenticatedRequest.getContent(), is(not((InputStream) null)));
  }

  @Test
  public void getContentLength() {
    assertThat(this.authenticatedRequest.getContentLength(), is(1024L));
  }

  @Test(expected = NullPointerException.class)
  public void noUsername() {
    final Request badRequest = new HttpRequest.Builder(Method.PUT, this.uri, Operation.WRITE)
        .withContext(Context.X_OG_PASSWORD, this.password).build();
    this.basicAuth.authenticate(badRequest);
  }

  @Test(expected = NullPointerException.class)
  public void noPassword() {
    final Request badRequest = new HttpRequest.Builder(Method.PUT, this.uri, Operation.WRITE)
        .withHeader(Context.X_OG_USERNAME, this.username).build();
    this.basicAuth.authenticate(badRequest);
  }
}
